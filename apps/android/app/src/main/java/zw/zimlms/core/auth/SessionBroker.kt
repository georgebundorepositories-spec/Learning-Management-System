package zw.zimlms.core.auth

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client-side GoTrue refresh-token rotation. Safe for a single signed-in user
 * on a shared tablet; do not store passwords or any service-role credentials.
 * Cross-worker/manual refresh calls are serialized to avoid invalidating an
 * older rotated refresh token by accident.
 */
class SessionBroker(
  context: Context,
  private val baseUrl: String,
  private val publishableKey: String,
  private val http: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS).build()
) {
  private val vault = SessionVault(context)
  private val refreshGuard get() = refreshMutex

  init {
    require(baseUrl.startsWith("https://") && publishableKey.startsWith("sb_publishable_")) {
      "HTTPS Supabase endpoint and public key required"
    }
  }

  suspend fun store(login: AuthenticatedSchoolLogin): EncryptedSchoolSession =
    refreshGuard.withLock {
      val old = vault.load()
      val deviceId = if (old?.profile?.userId == login.profile.userId &&
        old.profile.schoolId == login.profile.schoolId) old.deviceId
      else SessionVault.newDeviceId()
      val session = EncryptedSchoolSession(login.profile, login.accessToken,
        login.refreshToken, login.expiresAtMillis, deviceId)
      vault.save(session)
      session
    }

  fun peek(): EncryptedSchoolSession? = vault.load()

  suspend fun accessToken(expectedUserId: String, expectedSchoolId: String): String =
    refreshGuard.withLock {
      val session = vault.load()
        ?: throw IOException("A new online sign-in is required for synchronisation")
      if (session.profile.userId != expectedUserId ||
          session.profile.schoolId != expectedSchoolId) {
        throw IOException("Stored session does not belong to this school account")
      }
      // Renew before expiry to survive slow, intermittently available networks.
      if (session.expiresAtMillis > System.currentTimeMillis() + 90_000L) {
        return@withLock session.accessToken
      }
      val response = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("refresh_token", session.refreshToken)
        val req = Request.Builder()
          .url("${baseUrl.trimEnd('/')}/auth/v1/token?grant_type=refresh_token")
          .header("apikey", publishableKey)
          .header("Content-Type", "application/json")
          .post(payload.toString().toRequestBody("application/json".toMediaType()))
          .build()
        http.newCall(req).execute().use { res ->
          if (res.code == 400 || res.code == 401 || res.code == 403) {
            // Account deleted, refresh revoked or rotated elsewhere.
            vault.clear()
            throw IOException("Session revoked; sign in online again")
          }
          if (!res.isSuccessful) throw IOException(
            "Network unavailable while refreshing session; offline changes preserved"
          )
          JSONObject(res.body?.string() ?: throw IOException("Empty refresh response"))
        }
      }
      val refreshedUser = response.optJSONObject("user")?.optString("id")
      if (refreshedUser != session.profile.userId) {
        vault.clear()
        throw IOException("Refresh identity mismatch; online login required")
      }
      val token = response.optString("access_token")
      val nextRefresh = response.optString("refresh_token")
      val expiresIn = response.optLong("expires_in", 0L)
      if (token.isBlank() || nextRefresh.isBlank() || expiresIn !in 60L..86400L) {
        throw IOException("Invalid refreshed session; offline changes preserved")
      }
      vault.save(session.copy(accessToken = token, refreshToken = nextRefresh,
        expiresAtMillis = System.currentTimeMillis() + expiresIn * 1000L))
      token
    }

  suspend fun signOut() {
    refreshGuard.withLock {
      // Local logout is immediate and works offline; remote sessions expire or
      // must be revoked explicitly from the trusted server administration flow.
      vault.clear()
    }
  }

  companion object {
    private val refreshMutex = Mutex()
  }
}
