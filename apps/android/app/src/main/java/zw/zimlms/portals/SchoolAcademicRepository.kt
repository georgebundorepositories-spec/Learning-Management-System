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

/** Read-only academic data, never stored in a plaintext offline database.
 *
 * Both queries are performed with the signed-in user's access token.
 * Supabase row-level security limits resources by school and assessment
 * scores to the learner, linked guardian or authorised school staff.
 */
data class AcademicResource(
  val id: String,
  val title: String,
  val subjectId: String,
  val description: String,
  val httpsUrl: String?
)

data class PrivateAssessmentResult(
  val id: String,
  val assessmentId: String,
  val learnerId: String,
  val title: String,
  val subjectId: String,
  val assessmentDate: String,
  val score: String,
  val maximum: String
)

class SchoolAcademicRepository(
  private val baseUrl: String,
  private val publishableKey: String,
  private val broker: SessionBroker,
  private val http: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
) {
  private val uuid = Regex("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")

  init {
    require(baseUrl.startsWith("https://") && publishableKey.startsWith("sb_publishable_"))
  }

  private suspend fun rows(actor: CachedProfile, resource: String): List<JSONObject> =
    withContext(Dispatchers.IO) {
      require(uuid.matches(actor.userId) && uuid.matches(actor.schoolId))
      val token = broker.accessToken(actor.userId, actor.schoolId)
      val request = Request.Builder()
        .url("${baseUrl.trimEnd('/')}/rest/v1/$resource")
        .header("apikey", publishableKey)
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/json")
        .get().build()
      http.newCall(request).execute().use { response ->
        if (!response.isSuccessful)
          throw IOException("Academic records unavailable (HTTP ${response.code})")
        val body = response.body?.string() ?: "[]"
        if (body.length > 750_000) throw IOException("Academic response exceeds safety limit")
        val array = JSONArray(body)
        (0 until array.length()).map { array.getJSONObject(it) }
      }
    }

  suspend fun resources(actor: CachedProfile): List<AcademicResource> {
    require(actor.role in setOf("admin", "head", "teacher", "student", "parent"))
    return rows(actor,
      "learning_resources?select=id,title,subject_id,description,resource_url" +
      "&school_id=eq.${actor.schoolId}&deleted_at=is.null&order=created_at.desc&limit=100"
    ).map { item ->
      AcademicResource(
        item.getString("id"), item.getString("title"), item.getString("subject_id"),
        item.optString("description", ""),
        item.optString("resource_url").takeIf { it.startsWith("https://", ignoreCase=true) }
      )
    }
  }

  /** Only students and linked parents can use this screen. Staff record
   * marks in the authenticated web portal, subject to separate RLS checks.
   */
  suspend fun privateResults(actor: CachedProfile): List<PrivateAssessmentResult> {
    require(actor.role in setOf("student", "parent"))
    val marks = rows(actor,
      "assessment_results?select=id,assessment_id,student_user_id,score" +
      "&order=created_at.desc&limit=100")
    if (marks.isEmpty()) return emptyList()

    val assessmentIds = marks.map { it.getString("assessment_id") }.distinct()
    if (assessmentIds.any { !uuid.matches(it) })
      throw IOException("Invalid assessment identifier")
    val assessments = rows(actor,
      "assessments?select=id,title,subject_id,assessment_date,max_score" +
      "&school_id=eq.${actor.schoolId}&deleted_at=is.null" +
      "&id=in.(${assessmentIds.joinToString(",")})&limit=100")
      .associateBy { it.getString("id") }

    return marks.mapNotNull { mark ->
      val assessmentId = mark.getString("assessment_id")
      val assessment = assessments[assessmentId] ?: return@mapNotNull null
      val learnerId = mark.getString("student_user_id")
      if (!uuid.matches(learnerId)) throw IOException("Invalid learner identifier")
      PrivateAssessmentResult(
        mark.getString("id"), assessmentId, learnerId,
        assessment.getString("title"), assessment.getString("subject_id"),
        assessment.getString("assessment_date"),
        mark.getString("score"), assessment.getString("max_score")
      )
    }
  }
}
