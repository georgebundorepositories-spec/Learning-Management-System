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

class AuthRepository(
  private val baseUrl: String,
  private val publishableKey: String,
  private val http: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
) {

  suspend fun login(identifier: String, password: String): CachedProfile = withContext(Dispatchers.IO) {
    require(baseUrl.startsWith("https://") && publishableKey.startsWith("sb_publishable_")) {
      "School connection is not configured"
    }
    require(password.isNotBlank()) { "Password is required" }
    val login = LoginIdentifiers.parse(identifier)
    val payload = JSONObject().put(login.key, login.value).put("password", password)
    val tokenRequest = Request.Builder()
      .url("${baseUrl.trimEnd('/')}/auth/v1/token?grant_type=password")
      .header("apikey", publishableKey)
      .header("Content-Type", "application/json")
      .post(payload.toString().toRequestBody("application/json".toMediaType()))
      .build()

    val token: JSONObject = http.newCall(tokenRequest).execute().use { response ->
      if (!response.isSuccessful) throw IOException("Unable to sign in; check your credentials or connection")
      JSONObject(response.body?.string() ?: throw IOException("Empty authentication response"))
    }
    val accessToken = token.optString("access_token")
    val userId = token.optJSONObject("user")?.optString("id").orEmpty()
    if (accessToken.isBlank() || userId.isBlank()) throw IOException("Invalid login response")

    // The JWT is checked by Supabase and the row is filtered by school-aware RLS.
    val profileRequest = Request.Builder()
      .url("${baseUrl.trimEnd('/')}/rest/v1/users?id=eq.${userId}&select=id,school_id,full_name,role,is_active")
      .header("apikey", publishableKey)
      .header("Authorization", "Bearer $accessToken")
      .get().build()
    val profile: JSONObject = http.newCall(profileRequest).execute().use { response ->
      if (!response.isSuccessful) throw IOException("Cannot access school profile")
      val records = JSONArray(response.body?.string() ?: "[]")
      if (records.length() != 1) throw IOException("Account not yet registered with a school")
      records.getJSONObject(0)
    }
    if (!profile.optBoolean("is_active", false)) throw IOException("Account inactive")
    CachedProfile(
      userId = userId,
      fullName = profile.getString("full_name"),
      role = profile.getString("role"),
      schoolId = profile.getString("school_id")
    )
  }
}
