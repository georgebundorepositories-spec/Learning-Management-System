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
import java.time.LocalDate
import java.util.concurrent.TimeUnit

data class AssessmentTeachingPair(val schoolClass:PortalClass,val subject:PortalSubject){
 val key:String get()=schoolClass.id+":"+subject.id
 val label:String get()=schoolClass.name+" · "+subject.name
}
data class SchoolAssessment(
 val id:String,val classId:String,val subjectId:String,val title:String,
 val maxScore:String,val assessmentDate:String
)
data class AssessmentLearner(val id:String,val fullName:String)
data class AssessmentScore(
 val id:String,val studentId:String,val score:String,val feedback:String,
 val updatedAt:String
)
data class AssessmentRoster(
 val learners:List<AssessmentLearner>,val results:Map<String,AssessmentScore>,
 val partial:Boolean
)

internal val assessmentUuid=Regex("^[a-fA-F0-9]{8}-([a-fA-F0-9]{4}-){3}[a-fA-F0-9]{12}$")
internal fun validAssessmentDate(value:String):Boolean =
 value.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) &&
 runCatching {
  LocalDate.parse(value).year in 2000..2100
 }.getOrDefault(false)

internal fun assessmentPairs(actor:CachedProfile,directory:SchoolDirectory?):List<AssessmentTeachingPair>{
 if(directory==null||actor.role !in setOf("admin","head","teacher"))return emptyList()
 return directory.classes.flatMap{schoolClass->
  directory.subjects.mapNotNull{subject->
   val allowed=actor.role in setOf("admin","head") ||
    schoolClass.teacherId==actor.userId ||
    directory.assignments.any{
     it.classId==schoolClass.id && it.subjectId==subject.id &&
      (it.teacherId.isBlank() || it.teacherId==actor.userId)
    }
   if(allowed)AssessmentTeachingPair(schoolClass,subject) else null
  }
 }
}

internal fun validAssessmentDraft(
 pair:AssessmentTeachingPair,title:String,maxScore:String,date:String
):Boolean =
 assessmentUuid.matches(pair.schoolClass.id)&&assessmentUuid.matches(pair.subject.id)&&
 title.trim().length in 3..160 &&
 validCourseworkMark(maxScore.trim(),maxScore.trim()) &&
 validAssessmentDate(date)

class SchoolAssessmentRepository(
 private val endpoint:String,private val publicKey:String,private val broker:SessionBroker,
 private val http:OkHttpClient=OkHttpClient.Builder()
  .connectTimeout(10,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).build()
){
 init{require(endpoint.startsWith("https://")&&publicKey.startsWith("sb_publishable_"))}

 private suspend fun send(
  actor:CachedProfile,path:String,method:String="GET",body:JSONObject?=null
 ):JSONArray=withContext(Dispatchers.IO){
  require(actor.role in setOf("admin","head","teacher"))
  require(assessmentUuid.matches(actor.userId)&&assessmentUuid.matches(actor.schoolId))
  require(method in setOf("GET","POST","PATCH"))
  val token=broker.accessToken(actor.userId,actor.schoolId)
  val request=Request.Builder().url(endpoint.trimEnd('/')+"/rest/v1/"+path)
   .header("apikey",publicKey)
   .header("Authorization","Bearer "+token)
   .header("Accept","application/json")
  if(method!="GET"){
   require(body!=null)
   request.header("Prefer","return=representation")
    .header("Content-Type","application/json")
   val payload=body.toString().toRequestBody("application/json".toMediaType())
   if(method=="POST")request.post(payload) else request.patch(payload)
  }
  http.newCall(request.build()).execute().use{result->
   if(!result.isSuccessful)
    throw IOException("Assessment request denied by school server (HTTP "+result.code+").")
   val text=result.body?.string()?:"[]"
   if(text.length>800_000)throw IOException("School assessment response is too large")
   JSONArray(text)
  }
 }
 private fun confirmed(rows:JSONArray,expected:String?=null):String{
  if(rows.length()!=1)throw IOException("School server did not confirm a single saved record")
  val id=rows.getJSONObject(0).getString("id")
  if(!assessmentUuid.matches(id)||expected!=null&&id!=expected)
   throw IOException("Invalid assessment save confirmation")
  return id
 }

 suspend fun list(actor:CachedProfile):List<SchoolAssessment>{
  val rows=send(actor,"assessments?select=id,class_id,subject_id,title,max_score,assessment_date"+
   "&school_id=eq."+actor.schoolId+"&deleted_at=is.null&order=created_at.desc&limit=100")
  return (0 until rows.length()).map{i->
   val row=rows.getJSONObject(i)
   SchoolAssessment(row.getString("id"),row.getString("class_id"),
    row.getString("subject_id"),row.getString("title"),
    row.getString("max_score"),row.getString("assessment_date"))
  }
 }

 suspend fun create(
  actor:CachedProfile,directory:SchoolDirectory,pair:AssessmentTeachingPair,
  title:String,maxScore:String,date:String
 ):String{
  require(validAssessmentDraft(pair,title,maxScore,date)){"Invalid assessment details"}
  require(assessmentPairs(actor,directory).any{it.key==pair.key}){
   "Only authorised classroom/subject combinations can be selected"
  }
  val record=JSONObject()
   .put("school_id",actor.schoolId)
   .put("class_id",pair.schoolClass.id)
   .put("subject_id",pair.subject.id)
   .put("title",title.trim())
   .put("max_score",BigDecimal(maxScore.trim()))
   .put("assessment_date",date)
   .put("created_by",actor.userId)
  return confirmed(send(actor,"assessments?select=id","POST",record))
 }

 /** RLS filters both user identities and learner profiles. Return only
  * enrolled, active students of the selected class; no offline roster cache.
  * Cap one mobile page at 100 to keep URL/request sizes bounded.
  */
 suspend fun roster(actor:CachedProfile,assessment:SchoolAssessment):AssessmentRoster{
  require(assessmentUuid.matches(assessment.id)&&assessmentUuid.matches(assessment.classId))
  val visible=send(actor,"assessments?select=id&id=eq."+assessment.id+
   "&school_id=eq."+actor.schoolId+"&deleted_at=is.null&limit=1")
  if(visible.length()!=1)throw IOException("No longer authorised for this assessment")

  val profiles=send(actor,"student_profiles?select=user_id"+
   "&class_id=eq."+assessment.classId+"&deleted_at=is.null&limit=101")
  val ids=(0 until minOf(profiles.length(),100)).map{
   profiles.getJSONObject(it).getString("user_id")
  }.filter(assessmentUuid::matches).distinct()
  if(ids.isEmpty())return AssessmentRoster(emptyList(),emptyMap(),false)
  val people=send(actor,"users?select=id,full_name,role,is_active"+
   "&school_id=eq."+actor.schoolId+
   "&id=in.("+ids.joinToString(",")+")"+
   "&role=eq.student&is_active=eq.true&deleted_at=is.null&limit=100")
  val byId=(0 until people.length()).associate{i->
   val person=people.getJSONObject(i)
   person.getString("id") to person.getString("full_name")
  }
  val learners=ids.mapNotNull{id->
   byId[id]?.let{AssessmentLearner(id,it)}
  }.sortedBy{it.fullName.lowercase()}
  val records=send(actor,"assessment_results?select=id,student_user_id,score,feedback,updated_at"+
   "&assessment_id=eq."+assessment.id+"&limit=500")
  val permitted=learners.map{it.id}.toSet()
  val marks=(0 until records.length()).mapNotNull{i->
   val row=records.getJSONObject(i)
   val student=row.getString("student_user_id")
   if(student !in permitted)return@mapNotNull null
   student to AssessmentScore(row.getString("id"),student,
    row.getString("score"),row.optString("feedback",""),
    row.optString("updated_at",""))
  }.toMap()
  return AssessmentRoster(learners,marks,profiles.length()>100||records.length()==500)
 }

 suspend fun saveScore(
  actor:CachedProfile,assessment:SchoolAssessment,studentId:String,
  score:String,feedback:String,previous:String?
 ):String{
  require(assessmentUuid.matches(assessment.id)&&assessmentUuid.matches(studentId)&&
   (previous==null||assessmentUuid.matches(previous)))
  require(validCourseworkMark(score.trim(),assessment.maxScore)){"Mark exceeds the maximum"}
  require(feedback.trim().length<=2000){"Feedback exceeds 2,000 characters"}
  // Refresh current maximum/subject/class before the write. RLS independently
  // validates active enrolment and subject assignment on insert and update.
  val current=send(actor,"assessments?select=id,class_id,subject_id,max_score"+
   "&id=eq."+assessment.id+"&school_id=eq."+actor.schoolId+
   "&deleted_at=is.null&limit=1")
  if(current.length()!=1)throw IOException("Assessment is no longer accessible")
  val item=current.getJSONObject(0)
  if(item.getString("class_id")!=assessment.classId ||
     item.getString("subject_id")!=assessment.subjectId ||
     !validCourseworkMark(score.trim(),item.getString("max_score")))
   throw IOException("The assessment has changed; refresh before marking")
  val data=JSONObject()
   .put("score",BigDecimal(score.trim()))
   .put("feedback",feedback.trim())
   .put("marked_by",actor.userId)
  val path:String
  val method:String
  if(previous==null){
   method="POST"
   path="assessment_results?select=id"
   data.put("assessment_id",assessment.id).put("student_user_id",studentId)
  }else{
   method="PATCH"
   path="assessment_results?select=id&id=eq."+previous+
    "&assessment_id=eq."+assessment.id+"&student_user_id=eq."+studentId
   data.put("updated_at",java.time.Instant.now().toString())
  }
  return confirmed(send(actor,path,method,data),previous)
 }
}
