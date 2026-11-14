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
import java.util.concurrent.TimeUnit

/** Only the currently authenticated active school admin may make membership
 * changes. UI choices are filtered for usability; Supabase RLS remains the
 * independent authorisation boundary on every GET/PATCH/POST/DELETE.
 */
data class GuardianMembershipLink(
 val id:String,val guardianId:String,val studentId:String
)
data class SchoolMembershipSnapshot(
 val enrolledClasses:Map<String,String?>,
 val guardianLinks:List<GuardianMembershipLink>,
 val partial:Boolean
)

internal val membershipUuid=Regex("^[a-fA-F0-9]{8}-([a-fA-F0-9]{4}-){3}[a-fA-F0-9]{12}$")
internal fun validMembershipActor(actor:CachedProfile):Boolean =
 actor.role=="admin"&&membershipUuid.matches(actor.userId)&&membershipUuid.matches(actor.schoolId)
internal fun validMembershipPair(first:String,second:String):Boolean =
 membershipUuid.matches(first)&&membershipUuid.matches(second)
internal fun eligibleMember(
 people:List<PortalPerson>,id:String,role:String
):Boolean=people.any{it.id==id&&it.role==role&&it.active}

class SchoolMembershipRepository(
 private val endpoint:String,private val publishableKey:String,
 private val broker:SessionBroker,
 private val http:OkHttpClient=OkHttpClient.Builder()
  .connectTimeout(10,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).build()
) {
 init {require(endpoint.startsWith("https://")&&publishableKey.startsWith("sb_publishable_"))}

 private suspend fun request(
  actor:CachedProfile,path:String,method:String="GET",body:JSONObject?=null
 ):JSONArray=withContext(Dispatchers.IO) {
  require(validMembershipActor(actor)){"Only school administrators may manage members"}
  require(method in setOf("GET","PATCH","POST","DELETE"))
  val token=broker.accessToken(actor.userId,actor.schoolId)
  val builder=Request.Builder().url(endpoint.trimEnd('/')+"/rest/v1/"+path)
   .header("apikey",publishableKey)
   .header("Authorization","Bearer "+token)
   .header("Accept","application/json")
  if(method!="GET"){
   builder.header("Prefer","return=representation")
   when(method){
    "DELETE"->builder.delete()
    "POST","PATCH"->{
     require(body!=null)
     builder.header("Content-Type","application/json")
     val payload=body.toString().toRequestBody("application/json".toMediaType())
     if(method=="POST")builder.post(payload) else builder.patch(payload)
    }
   }
  }
  http.newCall(builder.build()).execute().use{response->
   if(!response.isSuccessful)
    throw IOException("School membership request denied (HTTP "+response.code+").")
   val raw=response.body?.string()?:"[]"
   if(raw.length>1_000_000)throw IOException("School membership response too large")
   JSONArray(raw)
  }
 }

 private suspend fun activeMember(actor:CachedProfile,id:String,role:String){
  require(membershipUuid.matches(id)&&role in setOf("student","parent","teacher"))
  val result=request(actor,"users?select=id&id=eq."+id+
   "&school_id=eq."+actor.schoolId+"&role=eq."+role+
   "&is_active=eq.true&deleted_at=is.null&limit=1")
  if(result.length()!=1)throw IOException("Selected active school member was not found")
 }
 private suspend fun activeClass(actor:CachedProfile,id:String){
  require(membershipUuid.matches(id))
  val result=request(actor,"classes?select=id&id=eq."+id+
   "&school_id=eq."+actor.schoolId+"&deleted_at=is.null&limit=1")
  if(result.length()!=1)throw IOException("Selected school class was not found")
 }
 private fun requireOne(result:JSONArray,expectedId:String?=null):String{
  if(result.length()!=1)throw IOException("School server did not confirm the change")
  val id=result.getJSONObject(0).getString("id")
  if(!membershipUuid.matches(id)||(expectedId!=null&&id!=expectedId))
   throw IOException("Invalid school server confirmation")
  return id
 }

 /** Read live school-scoped relationships; no plaintext/offline membership
  * snapshots are persisted on the handset. A 500-record cap is explicit.
  */
 suspend fun load(actor:CachedProfile,directory:SchoolDirectory):SchoolMembershipSnapshot {
  require(validMembershipActor(actor))
  val studentIds=directory.people.filter{it.role=="student"&&it.active}.map{it.id}.toSet()
  val guardianIds=directory.people.filter{it.role=="parent"&&it.active}.map{it.id}.toSet()
  val profiles=request(actor,"student_profiles?select=user_id,class_id"+
   "&deleted_at=is.null&limit=500")
  val enrolled=buildMap<String,String?> {
   for(i in 0 until profiles.length()){
    val item=profiles.getJSONObject(i)
    val id=item.getString("user_id")
    if(id !in studentIds)continue
    val klass=item.optString("class_id").takeUnless{it.isBlank()||it=="null"}
    put(id,klass)
   }
  }
  val links=request(actor,"student_guardian_link?select=id,student_user_id,guardian_user_id"+
   "&deleted_at=is.null&limit=500")
  val existing=buildList{
   for(i in 0 until links.length()){
    val item=links.getJSONObject(i)
    val learner=item.getString("student_user_id")
    val parent=item.getString("guardian_user_id")
    if(learner in studentIds&&parent in guardianIds){
     add(GuardianMembershipLink(item.getString("id"),parent,learner))
    }
   }
  }
  return SchoolMembershipSnapshot(enrolled,existing,
   profiles.length()==500||links.length()==500||directory.partial)
 }

 suspend fun enrol(actor:CachedProfile,studentId:String,classId:String?){
  require(membershipUuid.matches(studentId)&&
   (classId==null||membershipUuid.matches(classId)))
  activeMember(actor,studentId,"student")
  if(classId!=null)activeClass(actor,classId)
  val found=request(actor,"student_profiles?select=id&user_id=eq."+studentId+
   "&deleted_at=is.null&limit=1")
  if(found.length()>1)throw IOException("Conflicting learner profiles")
  val body=JSONObject().put("class_id",classId?:JSONObject.NULL)
  if(found.length()==1){
   val id=found.getJSONObject(0).getString("id")
   require(membershipUuid.matches(id))
   requireOne(request(actor,"student_profiles?id=eq."+id+
    "&select=id","PATCH",body),id)
  }else{
   body.put("user_id",studentId)
   requireOne(request(actor,"student_profiles?select=id","POST",body))
  }
 }

 suspend fun homeroom(actor:CachedProfile,classId:String,teacherId:String?){
  require(membershipUuid.matches(classId)&&
    (teacherId==null||membershipUuid.matches(teacherId)))
  activeClass(actor,classId)
  if(teacherId!=null)activeMember(actor,teacherId,"teacher")
  requireOne(request(actor,"classes?id=eq."+classId+
   "&school_id=eq."+actor.schoolId+"&deleted_at=is.null&select=id",
   "PATCH",JSONObject().put("teacher_id",teacherId?:JSONObject.NULL)),classId)
 }

 suspend fun link(actor:CachedProfile,studentId:String,parentId:String){
  require(validMembershipPair(studentId,parentId)&&studentId!=parentId)
  activeMember(actor,studentId,"student")
  activeMember(actor,parentId,"parent")
  val matches=request(actor,"student_guardian_link?select=id"+
   "&student_user_id=eq."+studentId+"&guardian_user_id=eq."+parentId+
   "&deleted_at=is.null&limit=1")
  if(matches.length()==1)return
  requireOne(request(actor,"student_guardian_link?select=id","POST",
   JSONObject().put("student_user_id",studentId)
    .put("guardian_user_id",parentId)
    .put("is_primary",false).put("can_pay_fees",false)))
 }

 suspend fun revoke(actor:CachedProfile,link:GuardianMembershipLink){
  require(membershipUuid.matches(link.id)&&validMembershipPair(link.studentId,link.guardianId))
  val matches=request(actor,"student_guardian_link?select=id,student_user_id,guardian_user_id"+
   "&id=eq."+link.id+"&deleted_at=is.null&limit=1")
  if(matches.length()!=1)throw IOException("Guardian relationship not found")
  val row=matches.getJSONObject(0)
  if(row.getString("student_user_id")!=link.studentId||
     row.getString("guardian_user_id")!=link.guardianId)
   throw IOException("Guardian relationship was changed")
  // Actual DELETE immediately revokes RLS-based learner visibility.
  requireOne(request(actor,"student_guardian_link?id=eq."+link.id+
   "&select=id","DELETE"),link.id)
 }
}
