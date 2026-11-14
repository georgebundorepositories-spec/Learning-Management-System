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
import java.time.Instant
import java.util.concurrent.TimeUnit

data class SavedTeacherPlan(
 val id:String,val teacherId:String,val classId:String,val subjectId:String,
 val lessonId:String,val title:String,val objective:String,val steps:String,
 val resources:String,val assessment:String,val reflection:String,
 val plannedOn:String?,val status:String,val updatedAt:String
)
data class TeacherPlanDraft(
 val classId:String,val subjectId:String,val lessonId:String,
 val title:String,val objective:String,val steps:String,val resources:String,
 val assessment:String,val reflection:String,val plannedOn:String?,val status:String
)
private val planUuid=Regex("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
private val planLessonId=Regex("^[a-z0-9][a-z0-9-]{2,120}$")

internal fun validateTeacherPlanDraft(draft:TeacherPlanDraft):Boolean {
 return planUuid.matches(draft.classId)&&planUuid.matches(draft.subjectId)&&
   planLessonId.matches(draft.lessonId)&&draft.title.trim().length in 3..180&&
   draft.objective.trim().length in 10..3000&&
   draft.steps.trim().length in 20..6000&&
   draft.resources.length<=3000&&draft.assessment.length<=3000&&
   draft.reflection.length<=3000&&draft.status in setOf("draft","ready")&&
   (draft.plannedOn==null||runCatching {LocalDate.parse(draft.plannedOn)}.isSuccess)
}

/**
 * Phase 12: encrypted-session authenticated, RLS-protected cloud lesson plans.
 * No private plans or teacher reflections are written to plaintext disk,
 * offline queues, Android logs or shared preferences.
 */
class TeacherPlanRepository(
 private val baseUrl:String,private val publishableKey:String,
 private val broker:SessionBroker,
 private val http:OkHttpClient=OkHttpClient.Builder()
  .connectTimeout(10,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).build()
) {
 init {require(baseUrl.startsWith("https://")&&publishableKey.startsWith("sb_publishable_"))}
 private suspend fun request(actor:CachedProfile,path:String,method:String="GET",json:String?=null):
 JSONArray = withContext(Dispatchers.IO) {
  require(planUuid.matches(actor.userId)&&planUuid.matches(actor.schoolId))
  require(method in setOf("GET","POST","PATCH"))
  val token=broker.accessToken(actor.userId,actor.schoolId)
  val req=Request.Builder()
   .url(baseUrl.trimEnd('/') + "/rest/v1/" + path)
   .header("apikey",publishableKey)
   .header("Authorization","Bearer "+token)
   .header("Accept","application/json")
   .apply {
     if(method=="GET")get() else{
      require(json!=null)
      header("Content-Type","application/json")
      header("Prefer","return=representation")
      val body=json.toRequestBody("application/json".toMediaType())
      if(method=="POST")post(body) else patch(body)
     }
   }.build()
  http.newCall(req).execute().use {response->
   if(!response.isSuccessful)
    throw IOException("Unable to access lesson plans (HTTP "+response.code+"). Verify your school role and teaching assignment.")
   val raw=response.body?.string()?:"[]"
   if(raw.length>900_000)throw IOException("Lesson plan response exceeds safety limit")
   JSONArray(raw)
  }
 }

 suspend fun load(actor:CachedProfile):List<SavedTeacherPlan>{
  require(actor.role in setOf("teacher","head","admin")){"School staff access required"}
  val teacherFilter=if(actor.role=="teacher")"&teacher_id=eq."+actor.userId else ""
  val data=request(actor,"teacher_lesson_plans?select=id,teacher_id,class_id,subject_id,"+
    "source_lesson_id,lesson_title,objective,teaching_steps,resources,assessment,"+
    "reflection,planned_on,status,updated_at"+
    "&school_id=eq."+actor.schoolId+"&deleted_at=is.null"+teacherFilter+
    "&order=updated_at.desc&limit=100")
  return (0 until data.length()).map { i->
   val item=data.getJSONObject(i)
   SavedTeacherPlan(
    id=item.getString("id"),teacherId=item.getString("teacher_id"),
    classId=item.getString("class_id"),subjectId=item.getString("subject_id"),
    lessonId=item.getString("source_lesson_id"),title=item.getString("lesson_title"),
    objective=item.getString("objective"),steps=item.getString("teaching_steps"),
    resources=item.optString("resources",""),assessment=item.optString("assessment",""),
    reflection=item.optString("reflection",""),
    plannedOn=item.optString("planned_on").takeUnless{it==""||it=="null"},
    status=item.getString("status"),updatedAt=item.getString("updated_at")
   )
  }.also { rows ->
   if(rows.any{!planUuid.matches(it.id)||!planUuid.matches(it.teacherId)})
     throw IOException("School returned an invalid plan reference")
  }
 }

 /** Only an assigned teacher can create/revise. PATCH never changes the
  * owner, class, school, subject or curriculum reference. The database also
  * denies those columns and applies its own RLS.
  */
 suspend fun save(actor:CachedProfile,draft:TeacherPlanDraft,existingId:String?=null):String{
  require(actor.role=="teacher"){"Only a teacher can edit lesson plans"}
  require(validateTeacherPlanDraft(draft)){"Invalid lesson plan: check dates and field lengths"}
  require(existingId==null||planUuid.matches(existingId))
  val body=JSONObject()
   .put("lesson_title",draft.title.trim())
   .put("objective",draft.objective.trim())
   .put("teaching_steps",draft.steps.trim())
   .put("resources",draft.resources.trim())
   .put("assessment",draft.assessment.trim())
   .put("reflection",draft.reflection.trim())
   .put("planned_on",draft.plannedOn?:JSONObject.NULL)
   .put("status",draft.status)
  val path:String
  val method:String
  if(existingId==null){
   method="POST"
   path="teacher_lesson_plans?select=id"
   body.put("school_id",actor.schoolId)
    .put("teacher_id",actor.userId)
    .put("class_id",draft.classId)
    .put("subject_id",draft.subjectId)
    .put("source_lesson_id",draft.lessonId)
  }else{
   method="PATCH"
   path="teacher_lesson_plans?id=eq."+existingId+"&teacher_id=eq."+actor.userId+
    "&school_id=eq."+actor.schoolId+"&deleted_at=is.null&select=id"
   body.put("updated_at",Instant.now().toString())
  }
  val rows=request(actor,path,method,body.toString())
  if(rows.length()!=1)throw IOException("The school server did not confirm this plan. Check your teaching assignment.")
  val id=rows.getJSONObject(0).getString("id")
  if(!planUuid.matches(id)||(existingId!=null&&id!=existingId))
   throw IOException("Invalid lesson-plan save confirmation")
  return id
 }
}
