package zw.zimlms.core.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class SessionVaultInstrumentedTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val vault = SessionVault(context)
  private val profile = CachedProfile(
    "11111111-1111-4111-8111-111111111111",
    "Fictional Test Teacher", "teacher",
    "22222222-2222-4222-8222-222222222222"
  )
  private fun session(expiration: Long = System.currentTimeMillis() + 3600000L) =
    EncryptedSchoolSession(profile,"sample-jwt-not-real","sample-refresh-not-real",
      expiration,"33333333-3333-4333-8333-333333333333")

  @Before fun prepare() { vault.clear() }
  @After fun cleanup() { vault.clear() }

  @Test fun encryptedTokenSurvivesReopenWithoutPlaintextOnDisk() {
    vault.save(session())
    val prefs = context.getSharedPreferences(
      "zimschool_encrypted_session_v1",Context.MODE_PRIVATE)
    val bytes = prefs.all.values.joinToString()
    assertFalse(bytes.contains("sample-jwt-not-real"))
    assertFalse(bytes.contains("sample-refresh-not-real"))
    val read = SessionVault(context).load()
    assertNotNull(read)
    assertEquals(profile.userId, read!!.profile.userId)
    assertEquals(profile.schoolId, read.profile.schoolId)
    assertEquals(session().deviceId, read.deviceId)
    assertEquals("sample-refresh-not-real", read.refreshToken)
    assertEquals("sample-jwt-not-real", read.accessToken)
  }

  @Test fun tamperedCiphertextFailsClosed() {
    vault.save(session())
    val prefs = context.getSharedPreferences(
      "zimschool_encrypted_session_v1",Context.MODE_PRIVATE)
    assertTrue(prefs.edit().putString("session","NOT-VALID-BASE64").commit())
    assertNull(vault.load())
  }

  @Test fun signOutRevokesOnlyCurrentRemoteSessionAndClearsTokens() = runBlocking {
    vault.save(session())
    val calls=AtomicInteger(0)
    var sentPath: String? = null
    var sentQuery: String? = null
    var sentAuthorization: String? = null
    val http=OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
      val req=chain.request()
      sentPath = req.url.encodedPath
      sentQuery = req.url.encodedQuery
      sentAuthorization = req.header("Authorization")
      calls.incrementAndGet()
      Response.Builder().request(req).protocol(Protocol.HTTP_1_1)
        .code(200).message("OK")
        .body("{}".toResponseBody("application/json".toMediaType())).build()
    }).build()
    val broker=SessionBroker(context,"https://test.invalid","sb_publishable_ci",http)
    val serverRevoked = broker.signOut()
    assertEquals("HTTP logout must be called exactly once", 1, calls.get())
    assertEquals("/auth/v1/logout", sentPath)
    assertEquals("scope=local", sentQuery)
    assertEquals("Bearer sample-jwt-not-real", sentAuthorization)
    assertTrue("HTTP 200 should report local-scope revocation", serverRevoked)
    assertNull(SessionVault(context).load())
  }

  @Test fun offlineSignOutStillClearsSessionWithoutRemoteAck() = runBlocking {
    vault.save(session())
    val http=OkHttpClient.Builder().addInterceptor { _ ->
      throw IOException("No network at school")
    }.build()
    val broker=SessionBroker(context,"https://test.invalid","sb_publishable_ci",http)
    assertFalse(broker.signOut())
    assertNull(vault.load())
  }

  @Test fun refreshRotationIsSerializedForConcurrentWorkers() = runBlocking {
    vault.save(session(expiration=System.currentTimeMillis()-1000L))
    val calls=AtomicInteger()
    val http=OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
      val request=chain.request()
      if (request.url.encodedPath != "/auth/v1/token" ||
          request.url.encodedQuery != "grant_type=refresh_token") {
        throw IOException("Unexpected endpoint")
      }
      calls.incrementAndGet()
      Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
        .code(200).message("OK")
        .body(("{\"access_token\":\"new.jwt\",\"refresh_token\":\"rotated.refresh\"," +
          "\"expires_in\":3600,\"user\":{\"id\":\"${profile.userId}\"}}")
          .toResponseBody("application/json".toMediaType()))
        .build()
    }).build()
    val broker=SessionBroker(context,"https://test.invalid","sb_publishable_ci",http)
    val tokens=coroutineScope {
      (1..4).map { async { broker.accessToken(profile.userId,profile.schoolId) } }.awaitAll()
    }
    assertTrue(tokens.all { it=="new.jwt" })
    assertEquals("Only one refresh network request permitted",1,calls.get())
    assertEquals("rotated.refresh",SessionVault(context).load()!!.refreshToken)
  }

  @Test fun requestingAnotherSchoolsCredentialsFailsWithoutNetwork() = runBlocking {
    vault.save(session())
    val broker=SessionBroker(context,"https://test.invalid","sb_publishable_ci")
    try {
      broker.accessToken(profile.userId,"44444444-4444-4444-8444-444444444444")
      fail("Cross-school token access must fail")
    } catch (expected: IOException) {
      assertTrue(expected.message!!.contains("does not belong"))
    }
    assertNotNull(vault.load())
  }
}
