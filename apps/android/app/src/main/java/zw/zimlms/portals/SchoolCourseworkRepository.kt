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
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

/** Online coursework with JWT and school/learner RLS.
 * No answers are stored in plaintext offline storage.
 */
data class SchoolCoursework(
  val id: String,
  val classId: String,
  val subjectId: String,
  val title: String,
  val instructions: String,
  val dueOn: String?,
  val maxScore: String
)
data class SchoolCourseworkAnswer(
  val id: String,
  val assignmentId: String,
  val learnerId: String,
  val text: String,
  val updatedAt: String
)
data class SchoolCourseworkReview(
  val id: String,
  val submissionId: String,
  val score: String,
  val feedback: String,
  val reviewedAt: String
)
data class SchoolCourseworkSnapshot(
  val assignments: List<SchoolCoursework>,
  val answers: List<SchoolCourseworkAnswer>,
  val reviews: List<SchoolCourseworkReview>
)

private val markPattern=Regex("""^(?:0|[1-9]\d{0,4})(?:\.\d{1,2})?$""")
/** Exact decimal validation avoids floating-point grade-rounding errors. */
internal fun validCourseworkMark(score:String,maximum:String):Boolean{
  if(!markPattern.matches(score) || !markPattern.matches(maximum))return false
  val mark=BigDecimal(score)
  val max=BigDecimal(maximum)
  return max>BigDecimal.ZERO && max<=BigDecimal("10000") && mark<=max
}

class SchoolCourseworkRepository(
  private val baseUrl:String,
  private val publishableKey:String,
  private val broker:SessionBroker,
  private val http:OkHttpClient=OkHttpClient.Builder()
    .connectTimeout(10,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).build()
) {
  private val uuid=Regex("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
  init { require(baseUrl.startsWith("https://") && publishableKey.startsWith("sb_publishable_")) }

  private suspend fun request(actor:CachedProfile,path:String,
      method:String="GET",json:String?=null):JSONArray = withContext(Dispatchers.IO) {
    require(uuid.matches(actor.userId) && uuid.matches(actor.schoolId))
    require(method in setOf("GET","POST","PATCH"))
    val token=broker.accessToken(actor.userId,actor.schoolId)
    val builder=Request.Builder()
      .url("${baseUrl.trimEnd('/')}/rest/v1/$path")
      .header("apikey",publishableKey)
      .header("Authorization","Bearer $token")
      .header("Accept","application/json")
    if (method!="GET") {
      require(json!=null)
      builder.header("Content-Type","application/json")
        .header("Prefer","return=representation")
      val body=json.toRequestBody("application/json".toMediaType())
      if(method=="POST") builder.post(body) else builder.patch(body)
    }
    http.newCall(builder.build()).execute().use {response->
      if(!response.isSuccessful)
        throw IOException("Coursework request failed (HTTP ${response.code}). Check your school permissions.")
      val raw=response.body?.string()?:"[]"
      if(raw.length>1_000_000)throw IOException("Coursework response exceeded safe limit")
      JSONArray(raw)
    }
  }

  suspend fun load(actor:CachedProfile):SchoolCourseworkSnapshot {
    require(actor.role in setOf("admin","head","teacher","student","parent"))
    val items=request(actor,"school_assignments?select=id,class_id,subject_id,title,instructions,due_on,max_score" +
      "&school_id=eq.${actor.schoolId}&deleted_at=is.null&order=created_at.desc&limit=100")
    val answers=request(actor,"assignment_submissions?select=id,assignment_id,student_user_id,text_answer,updated_at" +
      "&school_id=eq.${actor.schoolId}&order=submitted_at.desc&limit=500")
    val reviewed=request(actor,"assignment_reviews?select=id,submission_id,score,feedback,reviewed_at" +
      "&school_id=eq.${actor.schoolId}&order=reviewed_at.desc&limit=500")
    val assignments=(0 until items.length()).map{i->
      val row=items.getJSONObject(i)
      SchoolCoursework(
        row.getString("id"),row.getString("class_id"),row.getString("subject_id"),
        row.getString("title"),row.getString("instructions"),
        row.optString("due_on").takeUnless{it.isEmpty()||it=="null"},
        row.getString("max_score")
      )
    }
    val visible=assignments.map{it.id}.toSet()
    val privateAnswers=(0 until answers.length()).mapNotNull{i->
      val row=answers.getJSONObject(i)
      val assignmentId=row.getString("assignment_id")
      if(assignmentId !in visible)return@mapNotNull null
      SchoolCourseworkAnswer(
        row.getString("id"),assignmentId,row.getString("student_user_id"),
        row.getString("text_answer"),row.optString("updated_at")
      )
    }
    val visibleAnswers=privateAnswers.map{it.id}.toSet()
    val reviews=(0 until reviewed.length()).mapNotNull{i->
      val row=reviewed.getJSONObject(i)
      val submissionId=row.getString("submission_id")
      if(submissionId !in visibleAnswers)return@mapNotNull null
      SchoolCourseworkReview(
        row.getString("id"),submissionId,row.getString("score"),
        row.optString("feedback",""),row.getString("reviewed_at")
      )
    }
    return SchoolCourseworkSnapshot(assignments,privateAnswers,reviews)
  }

  /** A learner may create or revise only their own answer; server-side
   * column grants prohibit reassignment or changing another learner's work.
   */
  suspend fun submit(actor:CachedProfile,assignmentId:String,textAnswer:String,
      existingId:String?):Unit {
    require(actor.role=="student"){"Learner role required"}
    require(uuid.matches(assignmentId) && (existingId==null || uuid.matches(existingId)))
    val answer=textAnswer.trim()
    require(answer.length in 1..5000){"Answer must contain 1–5,000 characters"}
    val path:String
    val payload:JSONObject
    val method:String
    if(existingId==null) {
      path="assignment_submissions?select=id"
      method="POST"
      payload=JSONObject()
        .put("school_id",actor.schoolId)
        .put("assignment_id",assignmentId)
        .put("student_user_id",actor.userId)
        .put("text_answer",answer)
    }else{
      path="assignment_submissions?id=eq.$existingId&student_user_id=eq.${actor.userId}&select=id"
      method="PATCH"
      payload=JSONObject().put("text_answer",answer)
        .put("updated_at",java.time.Instant.now().toString())
    }
    val rows=request(actor,path,method,payload.toString())
    if(rows.length()!=1)throw IOException("School server did not confirm the answer")
    if(!uuid.matches(rows.getJSONObject(0).getString("id")))
      throw IOException("Invalid coursework confirmation")
  }

  /** Record teacher feedback with an authenticated PostgREST request.  The
   * server verifies class+subject assignment, score ceiling and record owner.
   */
  suspend fun review(actor:CachedProfile,submissionId:String,scoreInput:String,
      maxScore:String,feedbackInput:String,existingId:String?):Unit{
    require(actor.role in setOf("admin","head","teacher")){"School staff role required"}
    require(uuid.matches(submissionId) && (existingId==null || uuid.matches(existingId)))
    require(validCourseworkMark(scoreInput.trim(),maxScore)){"Invalid mark or maximum score"}
    val feedback=feedbackInput.trim()
    require(feedback.length<=2000){"Feedback exceeds 2,000 characters"}

    val payload=JSONObject()
      .put("score",BigDecimal(scoreInput.trim()))
      .put("feedback",feedback)
      .put("reviewed_by",actor.userId)
    val method:String
    val path:String
    if(existingId==null){
      method="POST"
      path="assignment_reviews?select=id"
      payload.put("school_id",actor.schoolId)
        .put("submission_id",submissionId)
    }else{
      method="PATCH"
      path="assignment_reviews?id=eq.$existingId&school_id=eq.${actor.schoolId}&select=id"
      payload.put("reviewed_at",java.time.Instant.now().toString())
    }
    val saved=request(actor,path,method,payload.toString())
    if(saved.length()!=1 || !uuid.matches(saved.getJSONObject(0).getString("id")))
      throw IOException("School server did not confirm the review")
  }
}
