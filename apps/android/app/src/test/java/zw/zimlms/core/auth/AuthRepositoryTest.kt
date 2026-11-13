package zw.zimlms.core.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

/** Exercises sign-in exchange and RLS-scoped profile retrieval without network or secrets. */
class AuthRepositoryTest {
  private val profileId = "00000000-0000-4000-8000-000000000123"
  private val schoolId = "11111111-1111-4111-8111-111111111111"

  private fun response(request: okhttp3.Request, code: Int, json: String): Response =
    Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(code)
      .message(if (code in 200..299) "OK" else "Denied")
      .body(json.toResponseBody("application/json".toMediaType()))
      .build()

  @Test fun phoneLoginSendsCredentialsAndAccessTokenToProtectedProfileEndpoint() = runBlocking {
    val requests = mutableListOf<okhttp3.Request>()
    val http = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
      val req = chain.request()
      requests += req
      when (req.url.encodedPath) {
        "/auth/v1/token" ->
          response(req, 200, """{"access_token":"test.jwt","user":{"id":"$profileId"}}""")
        "/rest/v1/users" ->
          response(req, 200, """[{"id":"$profileId","school_id":"$schoolId","full_name":"Test Teacher","role":"teacher","is_active":true}]""")
        else -> response(req, 404, """{"error":"not found"}""")
      }
    }).build()
    val repo = AuthRepository("https://test.example.invalid", "sb_publishable_ci_key", http)
    val profile = repo.login("077 123 4567", "PasswordForCI!")
    assertEquals(profileId, profile.userId)
    assertEquals(schoolId, profile.schoolId)
    assertEquals("teacher", profile.role)
    assertEquals(2, requests.size)
    assertEquals("POST", requests[0].method)
    assertEquals("grant_type=password", requests[0].url.encodedQuery)
    assertTrue(requests[0].body!!.let { body ->
      val buffer = okio.Buffer()
      body.writeTo(buffer)
      buffer.readUtf8().contains("+263771234567")
    })
    assertEquals("Bearer test.jwt", requests[1].header("Authorization"))
    assertEquals("sb_publishable_ci_key", requests[1].header("apikey"))
  }

  @Test fun forbiddenOrMissingProfileBlocksLogin() = runBlocking {
    val http = OkHttpClient.Builder().addInterceptor { chain ->
      val req = chain.request()
      when (req.url.encodedPath) {
        "/auth/v1/token" -> response(req, 200, """{"access_token":"test.jwt","user":{"id":"$profileId"}}""")
        "/rest/v1/users" -> response(req, 200, "[]")
        else -> response(req, 404, "{}")
      }
    }.build()
    val repo = AuthRepository("https://test.example.invalid", "sb_publishable_ci_key", http)
    try {
      repo.login("teacher@example.invalid", "PasswordForCI!")
      fail("Missing school registration must reject sign-in")
    } catch (expected: IOException) {
      assertTrue(expected.message!!.contains("not yet registered"))
    }
  }

  @Test fun inactiveSchoolProfileBlocksLogin() = runBlocking {
    val http = OkHttpClient.Builder().addInterceptor { chain ->
      val req = chain.request()
      when (req.url.encodedPath) {
        "/auth/v1/token" -> response(req, 200, """{"access_token":"test.jwt","user":{"id":"$profileId"}}""")
        "/rest/v1/users" ->
          response(req, 200, """[{"id":"$profileId","school_id":"$schoolId","full_name":"Inactive Account","role":"teacher","is_active":false}]""")
        else -> response(req, 404, "{}")
      }
    }.build()
    val repo = AuthRepository("https://test.example.invalid", "sb_publishable_ci_key", http)
    try {
      repo.login("teacher@example.invalid", "PasswordForCI!")
      fail("Inactive school profiles must reject login")
    } catch (expected: IOException) {
      assertTrue(expected.message!!.contains("inactive"))
    }
  }

  @Test fun wrongPasswordGetsCredentialsMessageNotMisleadingConnectionMessage() = runBlocking {
    val http = OkHttpClient.Builder().addInterceptor { chain ->
      response(chain.request(), 400, """{"error_code":"invalid_credentials","msg":"Invalid login credentials"}""")
    }.build()
    val repo = AuthRepository("https://test.example.invalid","sb_publishable_ci_key",http)
    try {
      repo.login("teacher@example.invalid","incorrect")
      fail("A rejected password must not open school access")
    } catch (e: SchoolSignInFailure) {
      assertEquals(SchoolSignInProblem.INCORRECT_CREDENTIALS,e.problem)
      assertFalse(safeSchoolSignInMessage(e).contains("connection",ignoreCase=true))
    }
  }

  @Test fun confirmedAuthWithoutSchoolShowsActionableEnrolmentMessage() = runBlocking {
    val http = OkHttpClient.Builder().addInterceptor { chain ->
      val req=chain.request()
      when (req.url.encodedPath) {
        "/auth/v1/token" ->
          response(req,200,"""{"access_token":"jwt-for-test","user":{"id":"$profileId"}}""")
        "/rest/v1/users" -> response(req,200,"[]")
        else -> response(req,404,"{}")
      }
    }.build()
    val repo=AuthRepository("https://test.example.invalid","sb_publishable_ci_key",http)
    try {
      repo.login("teacher@example.invalid","correct")
      fail("Orphaned Auth identities must not get a role")
    } catch (e: SchoolSignInFailure) {
      assertEquals(SchoolSignInProblem.SCHOOL_NOT_LINKED,e.problem)
      val message=safeSchoolSignInMessage(e)
      assertTrue(message.contains("Password accepted"))
      assertTrue(message.contains("administrator"))
      assertTrue(message.contains("school and role"))
    }
  }

  @Test fun serverOutageAndRateLimitsHaveDifferentGuidance() = runBlocking {
    for ((status,expected) in listOf(
      429 to SchoolSignInProblem.TOO_MANY_ATTEMPTS,
      503 to SchoolSignInProblem.SERVER_UNAVAILABLE,
      400 to SchoolSignInProblem.EMAIL_NOT_CONFIRMED
    )) {
      val http=OkHttpClient.Builder().addInterceptor { chain ->
        response(chain.request(),status,
          if(status==400) """{"error_code":"email_not_confirmed"}""" else "{}")
      }.build()
      val repo=AuthRepository("https://test.example.invalid","sb_publishable_ci_key",http)
      try {
        repo.login("teacher@example.invalid","test")
        fail("A failed Auth response must not sign in")
      } catch (e: SchoolSignInFailure) {
        assertEquals(expected,e.problem)
      }
    }
  }

  @Test fun unexpectedServerErrorsNeverLeakRawExceptionToUser() {
    val msg=safeSchoolSignInMessage(IllegalStateException("token=super-secret"))
    assertFalse(msg.contains("super-secret"))
    assertTrue(msg.contains("Could not sign in securely"))
  }

  @Test fun invalidPublicUrlCannotBeUsedForCredentials() = runBlocking {
    val repo = AuthRepository("http://insecure.example.invalid", "sb_publishable_ci_key")
    try {
      repo.login("teacher@example.invalid", "PasswordForCI!")
      fail("TLS must be enforced")
    } catch (error: SchoolSignInFailure) {
      assertEquals(SchoolSignInProblem.APP_NOT_CONFIGURED, error.problem)
      // Auth credentials are never sent over cleartext HTTP.
    }
  }
}
