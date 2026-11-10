package zw.zimlms.learning

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Device-local encrypted progress for every bundled learning lesson.
 * No learner names, marks, passwords or real school records are persisted.
 * Each user+school is an independent AES-GCM protected, authenticated namespace.
 *
 * The separate cloud synchroniser uploads only a student's successfully completed
 * lesson IDs. An invalid encrypted payload fails closed rather than resetting.
 */
class LessonProgressVault(context: Context) {
  // Coordinate separate UI and WorkManager instances during read/merge/write.
  private companion object { val progressLock=Any() }
  private val prefs=context.applicationContext.getSharedPreferences(
    "zimschool_lesson_progress_v1",Context.MODE_PRIVATE)
  private val keyAlias="zimschool_lesson_progress_aes_v1"
  private val uuid=Regex("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
  // Includes the 63 curriculum lessons, not just the original 12 starter IDs.
  // If curriculum assets cannot load, existing starter records remain usable.
  private val validIds=(StarterLessons.lessons.map { it.id } +
    runCatching { HeritageCurriculum.load(context.applicationContext).map { it.id } }
      .getOrDefault(emptyList())).toSet()

  private fun key(): SecretKey {
    val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
    val old=store.getKey(keyAlias,null) as? SecretKey
    if(old!=null)return old
    val generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
    generator.init(KeyGenParameterSpec.Builder(keyAlias,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256).build())
    return generator.generateKey()
  }

  private fun slot(userId:String,schoolId:String):String {
    require(uuid.matches(userId)&&uuid.matches(schoolId)){
      "Verified school account needed for learning progress"
    }
    val digest=MessageDigest.getInstance("SHA-256")
      .digest("$userId|$schoolId".toByteArray(Charsets.UTF_8))
    return "account_"+digest.joinToString("") { "%02x".format(it) }
  }

  @Synchronized fun completed(userId:String,schoolId:String):Set<String> {
    val slot=slot(userId,schoolId)
    val blob=prefs.getString(slot,null) ?: return emptySet()
    try {
      val encrypted=Base64.decode(blob,Base64.NO_WRAP)
      require(encrypted.size>28){"Corrupted learning record"}
      val cipher=Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE,key(),
        GCMParameterSpec(128,encrypted.copyOfRange(0,12)))
      cipher.updateAAD(slot.toByteArray(Charsets.UTF_8))
      val json=JSONObject(String(
        cipher.doFinal(encrypted.copyOfRange(12,encrypted.size)),Charsets.UTF_8))
      require(json.getString("user_id")==userId&&json.getString("school_id")==schoolId){
        "School progress identity mismatch"
      }
      val array=json.getJSONArray("completed")
      return (0 until array.length()).map{array.getString(it)}.filter{it in validIds}.toSet()
    }catch(e:Exception){
      throw IllegalStateException("Saved lesson progress is unavailable or damaged",e)
    }
  }

  @Synchronized fun markCompleted(userId:String,schoolId:String,lessonId:String):Set<String> {
    require(lessonId in validIds){"Unknown bundled lesson"}
    return mergeCompleted(userId,schoolId,setOf(lessonId))
  }

  /** Merge remote and local completion sets without losing offline events.
   * Preserves the existing AES-GCM namespace and writes only when needed.
   */
  @Synchronized fun mergeCompleted(
    userId:String,schoolId:String,cloudIds:Set<String>
  ):Set<String> = synchronized(progressLock) {
    val current=completed(userId,schoolId)
    val safeCloud=cloudIds.filter { it in validIds }.toSet()
    val set=current+safeCloud
    if(set==current)return current
    val slot=slot(userId,schoolId)
    val record=JSONObject().put("user_id",userId).put("school_id",schoolId)
      .put("completed",JSONArray(set.sorted())).toString().toByteArray(Charsets.UTF_8)
    val cipher=Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE,key())
    cipher.updateAAD(slot.toByteArray(Charsets.UTF_8))
    val encrypted=cipher.iv+cipher.doFinal(record)
    record.fill(0)
    check(prefs.edit().putString(slot,
      Base64.encodeToString(encrypted,Base64.NO_WRAP)).commit()){
      "Could not save learning progress"
    }
    set
  }
}
