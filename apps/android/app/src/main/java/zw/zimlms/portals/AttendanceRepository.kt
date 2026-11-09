package zw.zimlms.portals

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
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Online-only attendance: the existing Supabase table enforces school, class
 * membership, attendance manager role and unique student/date constraints.
 * No learner personal data, marks or attendance responses are persisted
 * offline. Server-side checks remain authoritative.
 */
data class AttendanceLearner(val id: String, val name: String, val classId: String)
data class AttendanceMark(
  val rowId: String, val studentId: String, val status: String, val date: String
)

class AttendanceRepository(
  private val baseUrl: String,
  private val publicKey: String,
  private val broker: SessionBroker,
  private val http: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).build()
) {
  private val uuid=Regex("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
  private val statuses=setOf("present","absent","late","excused")
  init { require(baseUrl.startsWith("https://") && publicKey.startsWith("sb_publishable_")) }

  private suspend fun send(
    actor: CachedProfile, route: String, method: String = "GET", body: String? = null
  ): JSONArray = withContext(Dispatchers.IO) {
    val token=broker.accessToken(actor.userId,actor.schoolId)
    val url="${baseUrl.trimEnd('/')}/rest/v1/$route"
    val request=Request.Builder().url(url)
      .header("apikey",publicKey)
      .header("Authorization","Bearer $token")
      .header("Accept","application/json")
      .header("Prefer","return=representation")
      .apply {
        if(method=="POST")post((body?:error("Missing payload"))
          .toRequestBody("application/json".toMediaType()))
        else if(method=="PATCH")patch((body?:error("Missing payload"))
          .toRequestBody("application/json".toMediaType()))
        else get()
      }.build()
    http.newCall(request).execute().use { response ->
      if(!response.isSuccessful) {
        throw IOException("Attendance request denied by the school server (HTTP ${response.code})")
      }
      val data=response.body?.string() ?: "[]"
      if(data.length>500_000)throw IOException("Attendance response too large")
      JSONArray(data)
    }
  }

  private fun each(array:JSONArray):List<JSONObject> =
    (0 until array.length()).map {array.getJSONObject(it)}

  private fun authoriseClass(actor:CachedProfile,classId:String) {
    require(actor.role in setOf("admin","head","teacher")) { "Staff account required" }
    require(uuid.matches(actor.userId) && uuid.matches(actor.schoolId)
      && uuid.matches(classId)) { "Invalid school or class identifier" }
  }

  suspend fun roster(actor:CachedProfile,classId:String):List<AttendanceLearner> {
    authoriseClass(actor,classId)
    // The authenticated RPC is SECURITY INVOKER. Its unexposed internal
    // function limits results to a teacher's assigned classes (or admin/head).
    // Do not fall back to broad student_profiles or users directory queries.
    val entries=each(send(actor,"rpc/get_attendance_roster","POST",
      JSONObject().put("p_class_id",classId).toString()))
    if(entries.size>=300)throw IOException("Roster exceeds safe page size")
    val learners=entries.map { row ->
      val studentId=row.getString("student_user_id")
      if(!uuid.matches(studentId))throw IOException("Invalid learner ID in roster")
      AttendanceLearner(studentId,row.optString("display_name","Learner"),classId)
    }
    if(learners.map { it.id }.distinct().size!=learners.size)
      throw IOException("Duplicate learner in the roster")
    return learners.sortedBy { it.name }
  }

  suspend fun day(actor:CachedProfile,classId:String,date:LocalDate):List<AttendanceMark> {
    authoriseClass(actor,classId)
    return each(send(actor,
      "attendance?select=id,student_user_id,status,date,school_id,class_id" +
        "&school_id=eq.${actor.schoolId}&class_id=eq.$classId&date=eq.$date" +
        "&deleted_at=is.null&limit=300")).map {
      check(it.getString("school_id")==actor.schoolId
        && it.getString("class_id")==classId) { "Foreign attendance record" }
      AttendanceMark(it.getString("id"),it.getString("student_user_id"),
        it.getString("status"),it.getString("date"))
    }
  }

  suspend fun mark(
    actor:CachedProfile,student:AttendanceLearner,date:LocalDate,
    status:String,existing:AttendanceMark?
  ) {
    authoriseClass(actor,student.classId)
    require(actor.role in setOf("admin","head")){"Administrator access required"}
    require(uuid.matches(student.id) && status in statuses) {"Invalid attendance record"}
    if(existing!=null) {
      require(existing.studentId==student.id && existing.date==date.toString()
        && uuid.matches(existing.rowId)) {"Record identity does not match learner/date"}
    }
    val payload=JSONObject().put("status",status).put("marked_by",actor.userId)
    val route=if(existing==null) {
      payload.put("school_id",actor.schoolId)
        .put("class_id",student.classId).put("student_user_id",student.id)
        .put("date",date.toString())
      "attendance?select=id,student_user_id,status,date"
    }else {
      "attendance?id=eq.${existing.rowId}&school_id=eq.${actor.schoolId}" +
        "&class_id=eq.${student.classId}&select=id,student_user_id,status,date"
    }
    val saved=send(actor,route,if(existing==null)"POST" else "PATCH",payload.toString())
    if(saved.length()!=1 || saved.getJSONObject(0).optString("student_user_id")!=student.id)
      throw IOException("Attendance was not confirmed by the school server")
  }
}
