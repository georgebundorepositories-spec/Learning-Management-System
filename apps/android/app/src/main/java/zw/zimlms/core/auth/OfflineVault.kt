package zw.zimlms.core.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

sealed interface OfflineLogin {
  data class Success(val profile: CachedProfile): OfflineLogin
  data class Rejected(val reason: String): OfflineLogin
}

/**
 * Only a salted slow PIN verifier and minimum profile fields are stored, encrypted by
 * a non-exportable AES-GCM key in Android Keystore. No server passwords are retained.
 */
class OfflineVault(private val context: Context) {
  private val alias = "zimschool_offline_key_v1"
  private val prefs = context.getSharedPreferences("zimschool_local_auth", Context.MODE_PRIVATE)
  private val saltBytes = 16
  private val iterations = 210_000
  private val maxAgeMillis = 30L * 24 * 60 * 60 * 1000
  private val lockMillis = 15L * 60 * 1000

  private fun key(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val existing = store.getKey(alias, null) as? SecretKey
    if (existing != null) return existing
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    val spec = KeyGenParameterSpec.Builder(
      alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256).build()
    generator.init(spec)
    return generator.generateKey()
  }

  private fun derive(pin: String, salt: ByteArray): ByteArray {
    val chars = pin.toCharArray()
    val spec = PBEKeySpec(chars, salt, iterations, 256)
    return try {
      SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    } finally {
      spec.clearPassword()
      chars.fill(' ')
    }
  }

  private fun save(record: JSONObject) {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key())
    val encrypted = cipher.doFinal(record.toString().toByteArray(Charsets.UTF_8))
    val bytes = cipher.iv + encrypted
    check(prefs.edit().putString("blob", Base64.encodeToString(bytes, Base64.NO_WRAP)).commit()) {
      "Could not save offline credentials"
    }
  }

  private fun load(): JSONObject? {
    val encoded = prefs.getString("blob", null) ?: return null
    return try {
      val bytes = Base64.decode(encoded, Base64.NO_WRAP)
      val iv = bytes.copyOfRange(0, 12)
      val payload = bytes.copyOfRange(12, bytes.size)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
      JSONObject(String(cipher.doFinal(payload), Charsets.UTF_8))
    } catch (_: Exception) { null } // Fail closed if device data is corrupt or tampered with.
  }

  @Synchronized fun enroll(profile: CachedProfile, pin: String) {
    require(pin.matches(Regex("^\\d{4,8}$"))) { "PIN must contain 4–8 numbers" }
    val salt = ByteArray(saltBytes).also(SecureRandom()::nextBytes)
    val now = System.currentTimeMillis()
    save(JSONObject()
      .put("userId", profile.userId)
      .put("fullName", profile.fullName)
      .put("role", profile.role)
      .put("schoolId", profile.schoolId)
      .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
      .put("verifier", Base64.encodeToString(derive(pin, salt), Base64.NO_WRAP))
      .put("lastOnline", now)
      .put("lastChecked", now)
      .put("failures", 0).put("lockUntil", 0L))
  }

  @Synchronized fun unlock(pin: String): OfflineLogin {
    val record = load() ?: return OfflineLogin.Rejected("Sign in online to enable offline access")
    val now = System.currentTimeMillis()
    if (now < record.optLong("lastChecked")) {
      return OfflineLogin.Rejected("Device clock changed; sign in online")
    }
    if (now - record.optLong("lastOnline") > maxAgeMillis) {
      return OfflineLogin.Rejected("Offline access expired; sign in online")
    }
    if (now < record.optLong("lockUntil")) {
      return OfflineLogin.Rejected("Too many attempts. Try again after 15 minutes")
    }
    if (!pin.matches(Regex("^\\d{4,8}$"))) {
      return OfflineLogin.Rejected("Enter your 4–8 digit PIN")
    }
    val salt = Base64.decode(record.getString("salt"), Base64.NO_WRAP)
    val expected = Base64.decode(record.getString("verifier"), Base64.NO_WRAP)
    val actual = derive(pin, salt)
    val verified = MessageDigest.isEqual(expected, actual)
    actual.fill(0)
    if (!verified) {
      val failures = record.optInt("failures", 0) + 1
      record.put("failures", failures).put("lastChecked", now)
      if (failures >= 5) record.put("lockUntil", now + lockMillis).put("failures", 0)
      save(record)
      return OfflineLogin.Rejected(
        if (failures >= 5) "Account locked for 15 minutes" else "Incorrect PIN"
      )
    }
    record.put("failures", 0).put("lockUntil", 0L).put("lastChecked", now)
    save(record)
    return OfflineLogin.Success(CachedProfile(
      record.getString("userId"),
      record.getString("fullName"),
      record.getString("role"),
      record.getString("schoolId")
    ))
  }

  @Synchronized fun clear() {
    prefs.edit().clear().commit()
  }
}
