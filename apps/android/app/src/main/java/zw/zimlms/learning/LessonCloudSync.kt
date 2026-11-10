package zw.zimlms.learning

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.SessionBroker
import java.io.IOException
import java.util.concurrent.TimeUnit

data class LessonSyncPlan(val upload:Set<String>,val merged:Set<String>)
internal fun planLessonSync(local:Set<String>,cloud:Set<String>)=
  LessonSyncPlan(upload=local-cloud,merged=local+cloud)
data class LessonSyncResult(val uploaded:Int,val total:Int)

/**
 * Online sync of encrypted offline lesson progress using the signed-in JWT.
 * Server enforces student identity, active school, and a fixed lesson catalog.
 * On failure the existing device-local AES-GCM record is never discarded.
 *
 * This is formative self-check completion only, NOT teacher-graded coursework.
 */
class LessonCloudSync(
  private val baseUrl:String,
  private val publishableKey:String,
  private val broker:SessionBroker,
  private val http:OkHttpClient=OkHttpClient.Builder()
    .connectTimeout(10,TimeUnit.SECONDS)
    .readTimeout(20,TimeUnit.SECONDS).build()
) {
  private val uuid=Regex("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
  init{
    require(baseUrl.startsWith("https://") && publishableKey.startsWith("sb_publishable_"))
  }

  private suspend fun execute(actor:CachedProfile,path:String,method:String,
    payload:String?=null):Pair<Int,String> = withContext(Dispatchers.IO){
    val token=broker.accessToken(actor.userId,actor.schoolId)
    val builder=Request.Builder()
      .url("${baseUrl.trimEnd('/')}/rest/v1/$path")
      .header("apikey",publishableKey)
      .header("Authorization","Bearer $token")
      .header("Accept","application/json")
    if(method=="POST"){
      require(payload!=null)
      builder.header("Content-Type","application/json")
        .header("Prefer","return=minimal")
        .post(payload.toRequestBody("application/json".toMediaType()))
    }
    val req=if(method=="GET")builder.get().build() else builder.build()
    http.newCall(req).execute().use{response->
      val raw=response.body?.string()?:""
      if(raw.length>65_536)throw IOException("Lesson sync response exceeds safe size")
      Pair(response.code,raw)
    }
  }

  suspend fun synchronize(actor:CachedProfile,vault:LessonProgressVault):LessonSyncResult {
    require(actor.role=="student"){"Only learners can upload self-check progress"}
    require(uuid.matches(actor.schoolId)&&uuid.matches(actor.userId)){"Verified school user needed"}
    val path="lesson_completions?select=lesson_id&school_id=eq.${actor.schoolId}" +
      "&student_user_id=eq.${actor.userId}&limit=100"
    val (code,response)=execute(actor,path,"GET")
    if(code !in 200..299)throw IOException("Cannot read school lesson progress (HTTP $code)")
    val records=JSONArray(response)
    if(records.length()>75)throw IOException("Unexpected cloud lesson count")
    val remote=(0 until records.length()).map{
      records.getJSONObject(it).getString("lesson_id")
    }.toSet()
    // Save cloud completions immediately; preserve the local set on any upload error.
    val merged=vault.mergeCompleted(actor.userId,actor.schoolId,remote)
    val plan=planLessonSync(merged,remote)
    var sent=0
    for(lessonId in plan.upload.sorted()){
      val body=JSONObject()
        .put("school_id",actor.schoolId)
        .put("student_user_id",actor.userId)
        .put("lesson_id",lessonId)
      val (postCode,errorText)=execute(actor,"lesson_completions", "POST",body.toString())
      if(postCode in 200..299) {sent++;continue}
      // Other device might have inserted the same id after our GET. Only
      // a known unique-constraint conflict can be considered idempotent.
      if(postCode==409 && runCatching{
          JSONObject(errorText).optString("code")=="23505"
        }.getOrDefault(false)) {sent++;continue}
      throw IOException("Lesson sync upload failed (HTTP $postCode); local progress is safe")
    }
    return LessonSyncResult(uploaded=sent,total=merged.size)
  }
}
