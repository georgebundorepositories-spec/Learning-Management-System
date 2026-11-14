package zw.zimlms.core.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * A single, enrolled device session. Contains tokens, NEVER school passwords.
 * Its contents are AES-GCM encrypted under an Android Keystore non-exportable key.
 * Offline PIN and server sessions are separate: the PIN permits local reading,
 * whereas network sync requires a non-expired server token.
 */
data class EncryptedSchoolSession(
  val profile: CachedProfile,
  val accessToken: String,
  val refreshToken: String,
  val expiresAtMillis: Long,
  val deviceId: String
)

class SessionVault(context: Context) {
  private val prefs = context.applicationContext.getSharedPreferences(
    "zimschool_encrypted_session_v1", Context.MODE_PRIVATE
  )
  private val alias = "zimschool_auth_session_aes_gcm_v1"

  private fun key(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val previous = store.getKey(alias, null) as? SecretKey
    if (previous != null) return previous
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(KeyGenParameterSpec.Builder(
      alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256).build())
    return generator.generateKey()
  }

  @Synchronized fun save(session: EncryptedSchoolSession) {
    val uuid = Regex("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
    require(uuid.matches(session.profile.userId) && uuid.matches(session.profile.schoolId) &&
      uuid.matches(session.deviceId)) { "Verified school, user and device UUIDs required" }
    require(session.accessToken.isNotBlank() && session.refreshToken.isNotBlank() &&
      session.expiresAtMillis > 0) { "Refusing to persist invalid session" }
    val data = JSONObject()
      .put("user_id", session.profile.userId).put("school_id", session.profile.schoolId)
      .put("full_name", session.profile.fullName).put("role", session.profile.role)
      .put("access_token", session.accessToken).put("refresh_token", session.refreshToken)
      .put("expires_at", session.expiresAtMillis).put("device_id", session.deviceId)
      .toString().toByteArray(Charsets.UTF_8)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key())
    val bytes = cipher.iv + cipher.doFinal(data)
    data.fill(0)
    check(prefs.edit().putString("session", Base64.encodeToString(bytes, Base64.NO_WRAP)).commit()) {
      "Could not securely persist user session"
    }
  }

  @Synchronized fun load(): EncryptedSchoolSession? {
    val blob = prefs.getString("session", null) ?: return null
    return try {
      val bytes = Base64.decode(blob, Base64.NO_WRAP)
      require(bytes.size > 28) { "Corrupt session" }
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
      val record = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
      EncryptedSchoolSession(
        profile = CachedProfile(
          record.getString("user_id"), record.getString("full_name"),
          record.getString("role"), record.getString("school_id")
        ),
        accessToken = record.getString("access_token"),
        refreshToken = record.getString("refresh_token"),
        expiresAtMillis = record.getLong("expires_at"),
        deviceId = record.getString("device_id")
      )
    } catch (_: Exception) {
      // Fail closed if the Keystore is reset, storage tampered with, or device restored.
      null
    }
  }

  @Synchronized fun clear() {
    check(prefs.edit().clear().commit()) { "Could not revoke local session" }
  }

  companion object {
    fun newDeviceId(): String = UUID.randomUUID().toString()
  }
}
