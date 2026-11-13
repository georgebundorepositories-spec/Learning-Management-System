package zw.zimlms.core.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class CachedProfile(
  val userId: String,
  val fullName: String,
  val role: String,
  val schoolId: String
)

data class AuthenticatedSchoolLogin(
  val profile: CachedProfile,
  val accessToken: String,
  val refreshToken: String,
  val expiresAtMillis: Long
)

class AuthRepository(
  private val baseUrl: String,
  private val publishableKey: String,
  private val http: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
) {

  suspend fun login(identifier: String, password: String): CachedProfile =
    loginCore(identifier, password).first

  suspend fun loginWithSession(identifier: String, password: String): AuthenticatedSchoolLogin {
    val (profile, session) = loginCore(identifier, password)
    val accessToken = session.optString("access_token")
    val refreshToken = session.optString("refresh_token")
    val expiresIn = session.optLong("expires_in", 0L)
    if (accessToken.isBlank() || refreshToken.isBlank() || expiresIn !in 60L..86400L) {
      throw SchoolSignInFailure(SchoolSignInProblem.INVALID_SESSION)
    }
    // Set an early safety margin to avoid using expired JWTs on unstable networks.
    return AuthenticatedSchoolLogin(
      profile, accessToken, refreshToken,
      System.currentTimeMillis() + expiresIn * 1000L
    )
  }

  private suspend fun loginCore(identifier: String, password: String):
    Pair<CachedProfile, JSONObject> = withContext(Dispatchers.IO) {
    if (!baseUrl.startsWith("https://") || !publishableKey.startsWith("sb_publishable_")) {
      throw SchoolSignInFailure(SchoolSignInProblem.APP_NOT_CONFIGURED)
    }
    if (password.isBlank()) throw SchoolSignInFailure(SchoolSignInProblem.INCORRECT_CREDENTIALS)
    val login = try { LoginIdentifiers.parse(identifier) }
      catch (_: IllegalArgumentException) { throw SchoolSignInFailure(SchoolSignInProblem.INVALID_IDENTIFIER) }
    val payload = JSONObject().put(login.key, login.value).put("password", password)
    val tokenRequest = Request.Builder()
      .url("${baseUrl.trimEnd('/')}/auth/v1/token?grant_type=password")
      .header("apikey", publishableKey)
      .header("Content-Type", "application/json")
      .post(payload.toString().toRequestBody("application/json".toMediaType()))
      .build()

    val token: JSONObject = http.newCall(tokenRequest).execute().use { response ->
      if (!response.isSuccessful) {
        // Classify the server's response without ever displaying its raw text.
        val raw = response.body?.string().orEmpty().lowercase()
        val problem = when {
          response.code == 429 -> SchoolSignInProblem.TOO_MANY_ATTEMPTS
          raw.contains("email_not_confirmed") || raw.contains("email not confirmed") ->
            SchoolSignInProblem.EMAIL_NOT_CONFIRMED
          response.code in 400..403 -> SchoolSignInProblem.INCORRECT_CREDENTIALS
          else -> SchoolSignInProblem.SERVER_UNAVAILABLE
        }
        throw SchoolSignInFailure(problem)
      }
      JSONObject(response.body?.string() ?: throw SchoolSignInFailure(SchoolSignInProblem.INVALID_SESSION))
    }
    val accessToken = token.optString("access_token")
    val userId = token.optJSONObject("user")?.optString("id").orEmpty()
    if (accessToken.isBlank() || userId.isBlank())
      throw SchoolSignInFailure(SchoolSignInProblem.INVALID_SESSION)

    // The JWT is checked by Supabase and the row is filtered by school-aware RLS.
    val profileRequest = Request.Builder()
      .url("${baseUrl.trimEnd('/')}/rest/v1/users?id=eq.${userId}&select=id,school_id,full_name,role,is_active")
      .header("apikey", publishableKey)
      .header("Authorization", "Bearer $accessToken")
      .get().build()
    val profile: JSONObject = http.newCall(profileRequest).execute().use { response ->
      if (!response.isSuccessful) throw SchoolSignInFailure(SchoolSignInProblem.PROFILE_UNAVAILABLE)
      val records = JSONArray(response.body?.string() ?: "[]")
      if (records.length() != 1) throw SchoolSignInFailure(SchoolSignInProblem.SCHOOL_NOT_LINKED)
      records.getJSONObject(0)
    }
    if (!profile.optBoolean("is_active", false))
      throw SchoolSignInFailure(SchoolSignInProblem.SCHOOL_INACTIVE)
    val authorized = CachedProfile(
      userId = userId,
      fullName = profile.getString("full_name"),
      role = profile.getString("role"),
      schoolId = profile.getString("school_id")
    )
    Pair(authorized, token)
  }
}
