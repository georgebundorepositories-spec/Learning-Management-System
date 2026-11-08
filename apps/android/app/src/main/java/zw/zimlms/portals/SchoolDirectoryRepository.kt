package zw.zimlms.portals

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.SessionBroker
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Read-only, RLS-enforced portal directories.
 *
 * No service-role tokens, admin impersonation, local plaintext cache or
 * user-provided table names. Personal names are held in RAM only. The server
 * must still enforce all school/role restrictions: client menus are not auth.
 */
data class PortalPerson(val id: String, val name: String, val role: String, val active: Boolean)
data class PortalClass(val id: String, val name: String, val grade: String)
data class PortalSubject(val id: String, val name: String)
data class PortalAssignment(val classId: String, val subjectId: String)
data class PortalLearnerLink(val studentId: String, val classId: String?)

data class SchoolDirectory(
  val schoolName: String,
  val isDemo: Boolean,
  val classes: List<PortalClass>,
  val subjects: List<PortalSubject>,
  val people: List<PortalPerson> = emptyList(),
  val assignments: List<PortalAssignment> = emptyList(),
  val linkedLearners: List<PortalLearnerLink> = emptyList(),
  val partial: Boolean = false
) {
  fun peopleByRole(role: String) = people.filter { it.role == role }
  fun className(id: String?) = classes.firstOrNull { it.id == id }?.name ?: "Not assigned"
  fun subjectName(id: String?) = subjects.firstOrNull { it.id == id }?.name ?: "Unassigned"
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
      classes, subjects, people, assignments, linkedLearners,
      partial = classes.size == 500 || subjects.size == 500 || people.size == 500
    )
  }
}
