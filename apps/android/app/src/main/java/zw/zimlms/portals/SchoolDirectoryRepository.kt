package zw.zimlms.portals

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.SessionBroker
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * RLS-enforced school directories and narrowly scoped admin teaching assignments.
 *
 * No service-role tokens, admin impersonation, local plaintext cache or
 * user-provided table names. Personal names are held in RAM only. The server
 * must still enforce all school/role restrictions: client menus are not auth.
 */
data class PortalPerson(val id: String, val name: String, val role: String, val active: Boolean)
data class PortalClass(val id: String, val name: String, val grade: String)
data class PortalSubject(val id: String, val name: String)
data class PortalAssignment(
  val classId: String,
  val subjectId: String,
  val id: String = "",
  val teacherId: String = "",
  val termId: String = ""
)
data class PortalTerm(
  val id: String,
  val name: String,
  val startDate: String,
  val endDate: String
)
data class PortalLearnerLink(val studentId: String, val classId: String?)

data class SchoolDirectory(
  val schoolName: String,
  val isDemo: Boolean,
  val classes: List<PortalClass>,
  val subjects: List<PortalSubject>,
  val people: List<PortalPerson> = emptyList(),
  val assignments: List<PortalAssignment> = emptyList(),
  val terms: List<PortalTerm> = emptyList(),
  val linkedLearners: List<PortalLearnerLink> = emptyList(),
  val partial: Boolean = false
) {
  fun peopleByRole(role: String) = people.filter { it.role == role }
  fun className(id: String?) = classes.firstOrNull { it.id == id }?.name ?: "Not assigned"
  fun subjectName(id: String?) = subjects.firstOrNull { it.id == id }?.name ?: "Unassigned"
  fun teacherName(id: String?) = people.firstOrNull { it.id == id }?.name ?: "Unassigned"
  fun termName(id: String?) = terms.firstOrNull { it.id == id }?.name ?: "Unassigned"
}

class SchoolDirectoryRepository(
  private val baseUrl: String,
  private val publishableKey: String,
  private val broker: SessionBroker,
  private val http: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS).build()
) {
  private val uuid = Regex("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
  init {
    require(baseUrl.startsWith("https://") && publishableKey.startsWith("sb_publishable_"))
  }

  private suspend fun rows(profile: CachedProfile, path: String): JSONArray =
    withContext(Dispatchers.IO) {
      val token = broker.accessToken(profile.userId, profile.schoolId)
      val request = Request.Builder()
        .url("${baseUrl.trimEnd('/')}/rest/v1/$path")
        .header("apikey", publishableKey)
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/json")
        .get().build()
      http.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          throw IOException("School data could not be loaded (HTTP ${response.code}). Your local lessons remain available.")
        }
        val body = response.body?.string() ?: "[]"
        if (body.length > 2_000_000) throw IOException("School directory response too large")
        JSONArray(body)
      }
    }

  private fun jsonList(array: JSONArray): List<JSONObject> =
    (0 until array.length()).map { array.getJSONObject(it) }

  suspend fun load(profile: CachedProfile): SchoolDirectory {
    require(uuid.matches(profile.userId) && uuid.matches(profile.schoolId)) {
      "A verified school identity is required"
    }
    val school = jsonList(rows(profile,
      "schools?select=id,name,is_demo&id=eq.${profile.schoolId}&limit=1"))
      .singleOrNull() ?: throw IOException("School membership is no longer available")
    if (school.getString("id") != profile.schoolId) throw IOException("School mismatch")

    // Every query is restricted to RLS-scoped records. The school filter is a
    // performance convenience, NOT the authorization boundary.
    val classes = jsonList(rows(profile,
      "classes?select=id,school_id,name,grade_level&school_id=eq.${profile.schoolId}&deleted_at=is.null&order=name.asc&limit=500"))
      .map { entry ->
        check(entry.getString("school_id") == profile.schoolId) { "Cross-school class payload" }
        PortalClass(entry.getString("id"), entry.getString("name"), entry.optString("grade_level"))
      }
    val subjects = jsonList(rows(profile,
      "subjects?select=id,school_id,name&school_id=eq.${profile.schoolId}&deleted_at=is.null&order=name.asc&limit=500"))
      .map { entry ->
        check(entry.getString("school_id") == profile.schoolId) { "Cross-school subject payload" }
        PortalSubject(entry.getString("id"), entry.getString("name"))
      }

    var people = emptyList<PortalPerson>()
    var assignments = emptyList<PortalAssignment>()
    var terms = emptyList<PortalTerm>()
    var linkedLearners = emptyList<PortalLearnerLink>()
    if (profile.role in setOf("admin", "head")) {
      people = jsonList(rows(profile,
        "users?select=id,school_id,full_name,role,is_active&school_id=eq.${profile.schoolId}&deleted_at=is.null&order=full_name.asc&limit=500"))
        .map { entry ->
          check(entry.getString("school_id") == profile.schoolId) { "Cross-school user payload" }
          PortalPerson(
            entry.getString("id"), entry.getString("full_name"),
            entry.getString("role"), entry.optBoolean("is_active", false)
          )
        }
      terms = jsonList(rows(profile,
        "terms?select=id,school_id,name,start_date,end_date&school_id=eq.${profile.schoolId}&deleted_at=is.null&order=start_date.desc&limit=100"))
        .map { entry ->
          check(entry.getString("school_id")==profile.schoolId) { "Cross-school term payload" }
          PortalTerm(entry.getString("id"),entry.getString("name"),
            entry.getString("start_date"),entry.getString("end_date"))
        }
      assignments = jsonList(rows(profile,
        "class_subject_teacher?select=id,class_id,subject_id,teacher_id,term_id&deleted_at=is.null&limit=500"))
        .map {
          PortalAssignment(it.getString("class_id"),it.getString("subject_id"),
            it.getString("id"),it.getString("teacher_id"),it.getString("term_id"))
        }
        .filter { assignment ->
          classes.any { it.id==assignment.classId } &&
            subjects.any { it.id==assignment.subjectId } &&
            people.any { it.id==assignment.teacherId && it.role=="teacher" } &&
            terms.any { it.id==assignment.termId }
        }
    } else if (profile.role == "teacher") {
      assignments = jsonList(rows(profile,
        "class_subject_teacher?select=class_id,subject_id&teacher_id=eq.${profile.userId}&deleted_at=is.null&limit=500"))
        .map { PortalAssignment(it.getString("class_id"), it.getString("subject_id")) }
        .filter { assignment ->
          // Never display assignment metadata from a foreign or inaccessible class.
          classes.any { it.id == assignment.classId } &&
            subjects.any { it.id == assignment.subjectId }
        }
    } else if (profile.role in setOf("parent", "student")) {
      val ids = if (profile.role == "student") listOf(profile.userId)
      else jsonList(rows(profile,
        "student_guardian_link?select=student_user_id&guardian_user_id=eq.${profile.userId}&deleted_at=is.null&limit=200"))
        .map { it.getString("student_user_id") }.filter(uuid::matches)
      if (ids.isNotEmpty()) {
        val query = ids.joinToString(",")
        linkedLearners = jsonList(rows(profile,
          "student_profiles?select=user_id,class_id&user_id=in.($query)&deleted_at=is.null&limit=200"))
          .map { item ->
            PortalLearnerLink(
              item.getString("user_id"),
              item.optString("class_id").takeUnless { it.isEmpty() || it == "null" }
            )
          }.filter { it.studentId in ids }
      }
    }
    return SchoolDirectory(
      school.getString("name"), school.optBoolean("is_demo"),
      classes, subjects, people, assignments, terms, linkedLearners,
      partial = classes.size == 500 || subjects.size == 500 || people.size == 500
    )

  /**
   * School administrators can assign an existing teacher to an existing class,
   * subject and term. The server RLS policy validates all four relationships.
   * This client cannot create teachers or schools and never receives a secret key.
   */
  suspend fun upsertTeachingAssignment(
    profile: CachedProfile,
    classId: String,
    subjectId: String,
    teacherId: String,
    termId: String,
    existingId: String? = null
  ) = withContext(Dispatchers.IO) {
    require(profile.role in setOf("admin","head")){"Admin role required"}
    require(listOf(profile.userId,profile.schoolId,classId,subjectId,teacherId,termId)
      .all(uuid::matches)){"Invalid assignment identity"}
    require(existingId==null || uuid.matches(existingId)){"Invalid assignment row"}
    val token=broker.accessToken(profile.userId,profile.schoolId)
    val fields=JSONObject()
      .put("class_id",classId)
      .put("subject_id",subjectId)
      .put("teacher_id",teacherId)
      .put("term_id",termId)
    val url=if(existingId==null) {
      "${baseUrl.trimEnd('/')}/rest/v1/class_subject_teacher?select=id"
    } else {
      "${baseUrl.trimEnd('/')}/rest/v1/class_subject_teacher?id=eq.$existingId&select=id"
    }
    val payload=if(existingId==null) fields.toString()
    else JSONObject().put("teacher_id",teacherId).toString()
    val request=Request.Builder().url(url)
      .header("apikey",publishableKey)
      .header("Authorization","Bearer $token")
      .header("Content-Type","application/json")
      .header("Prefer","return=representation")
      .apply {
        val body=payload.toRequestBody("application/json".toMediaType())
        if(existingId==null) post(body) else patch(body)
      }.build()
    http.newCall(request).execute().use {response->
      if(!response.isSuccessful) throw IOException(
        "Could not save assignment (HTTP ${response.code}). Check school membership and duplicate class/subject links.")
      val saved=JSONArray(response.body?.string()?:"[]")
      if(saved.length()!=1) throw IOException("Assignment not confirmed by the school server")
    }
  }

  suspend fun deleteTeachingAssignment(profile:CachedProfile,assignmentId:String) =
    withContext(Dispatchers.IO) {
      require(profile.role in setOf("admin","head")){"Admin role required"}
      require(uuid.matches(profile.userId)&&uuid.matches(profile.schoolId)&&uuid.matches(assignmentId)){
        "Invalid assignment identity"
      }
      val token=broker.accessToken(profile.userId,profile.schoolId)
      val request=Request.Builder()
        .url("${baseUrl.trimEnd('/')}/rest/v1/class_subject_teacher?id=eq.$assignmentId&select=id")
        .header("apikey",publishableKey)
        .header("Authorization","Bearer $token")
        .header("Prefer","return=representation")
        .delete().build()
      http.newCall(request).execute().use {response->
        if(!response.isSuccessful) throw IOException("Assignment removal denied (HTTP ${response.code}).")
        val removed=JSONArray(response.body?.string()?:"[]")
        if(removed.length()!=1) throw IOException("Assignment removal not confirmed")
      }
    }
  }
}
