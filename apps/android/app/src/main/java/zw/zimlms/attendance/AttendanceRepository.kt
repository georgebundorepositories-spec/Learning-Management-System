package zw.zimlms.attendance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import zw.zimlms.core.auth.CachedProfile
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Online-only Phase 3 attendance entry; queued offline LWW sync is a separate
 * release gate. Never cache student names or attendance in plaintext on disk.
 * All HTTP requests use a short-lived user JWT and server-enforced RLS.
 */
data class RosterLearner(val userId: String, val name: String)
data class AttendanceMark(val studentUserId: String, val status: String)

class AttendanceRepository(
  private val baseUrl: String,
  private val publishableKey: String,
  private val accessToken: suspend () -> String,
  private val http: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()
) {
  private val uuid = Regex("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
  private val statuses = setOf("present","absent","late","excused")

  init {
    require(baseUrl.startsWith("https://") && publishableKey.startsWith("sb_publishable_")) {
      "Only authenticated HTTPS school requests are permitted"
    }
  }

  private fun validate(profile: CachedProfile, classId: String) {
    require(profile.role in setOf("admin","head","teacher")) {
      "Attendance marking requires authorised school staff"
    }
    require(uuid.matches(profile.userId) && uuid.matches(profile.schoolId) &&
      uuid.matches(classId)) { "Verified user, school and class identities required" }
  }

  private suspend fun request(
    relative: String,
    method: String = "GET",
    payload: String? = null,
    prefer: String? = null
  ): String = withContext(Dispatchers.IO) {
    val jwt = accessToken()
    if (jwt.isBlank()) throw IOException("Sign in online to mark attendance")
    val builder = Request.Builder()
      .url("${baseUrl.trimEnd('/')}/rest/v1/$relative")
      .header("apikey",publishableKey)
      .header("Authorization","Bearer $jwt")
      .header("Accept","application/json")
    if (prefer != null) builder.header("Prefer",prefer)
    val req = when(method) {
      "GET" -> builder.get().build()
      "POST" -> builder.header("Content-Type","application/json")
        .post((payload ?: "[]").toRequestBody("application/json".toMediaType())).build()
      else -> throw IllegalArgumentException("Unsupported request method")
    }
    http.newCall(req).execute().use { response ->
      if (!response.isSuccessful) throw IOException(
        "School server could not process attendance (HTTP ${response.code}). No local submission was confirmed."
      )
      val body = response.body?.string() ?: "[]"
      if (body.length > 2_000_000) throw IOException("Response too large to process safely")
      body
    }
  }

  suspend fun loadRoster(profile: CachedProfile, classId: String): List<RosterLearner> {
    validate(profile,classId)
    val body = request("rpc/get_attendance_roster","POST",
      JSONObject().put("p_class_id",classId).toString())
    val array = JSONArray(body)
    if (array.length() > 500) throw IOException("Class roster exceeds display limit")
    return (0 until array.length()).map { index ->
      val item=array.getJSONObject(index)
      val id=item.getString("student_user_id")
      require(uuid.matches(id)) { "Invalid learner identifier from server" }
      RosterLearner(id,item.getString("display_name"))
    }.distinctBy { it.userId }
  }

  suspend fun loadDay(
    profile: CachedProfile,
    classId: String,
    date: LocalDate
  ): List<AttendanceMark> {
    validate(profile,classId)
    val body=request("attendance?select=student_user_id,status" +
      "&class_id=eq.$classId&date=eq.$date&deleted_at=is.null&limit=500")
    val array=JSONArray(body)
    if (array.length()>500) throw IOException("Attendance response too large")
    return (0 until array.length()).map { index ->
      val row=array.getJSONObject(index)
      val student=row.getString("student_user_id")
      val status=row.getString("status")
      require(uuid.matches(student) && status in statuses) { "Invalid attendance record" }
      AttendanceMark(student,status)
    }
  }

  /**
   * One explicit, atomic multi-row submission. Natural key (class,learner,date)
   * means repeat POSTs merge the same day's record without creating duplicates.
   * A server 2xx with fewer returned records is treated as a failed submission.
   */
  suspend fun saveDay(
    profile: CachedProfile,
    classId: String,
    date: LocalDate,
    marks: Map<String,String>
  ): Int {
    validate(profile,classId)
    require(marks.isNotEmpty() && marks.size<=200) {
      "Select at least one learner and no more than 200 per submission"
    }
    require(marks.all { (id,status) -> uuid.matches(id) && status in statuses }) {
      "Attendance contains an invalid learner or status"
    }
    val body=JSONArray()
    marks.toSortedMap().forEach { (student,status) ->
      body.put(JSONObject()
        .put("school_id",profile.schoolId)
        .put("class_id",classId)
        .put("student_user_id",student)
        .put("date",date.toString())
        .put("status",status)
        .put("marked_by",profile.userId))
    }
    val response=JSONArray(request(
      "attendance?on_conflict=class_id,student_user_id,date" +
        "&select=student_user_id,status",
      "POST",body.toString(),"resolution=merge-duplicates,return=representation"
    ))
    val confirmed=(0 until response.length()).map { index ->
      val row=response.getJSONObject(index)
      val id=row.getString("student_user_id")
      val status=row.getString("status")
      AttendanceMark(id,status)
    }
    val requestedIds=marks.keys
    require(confirmed.size==marks.size &&
      confirmed.map { it.studentUserId }.toSet()==requestedIds &&
      confirmed.all { marks[it.studentUserId]==it.status }) {
      "School server did not confirm the full attendance register"
    }
    return confirmed.size
  }
}
