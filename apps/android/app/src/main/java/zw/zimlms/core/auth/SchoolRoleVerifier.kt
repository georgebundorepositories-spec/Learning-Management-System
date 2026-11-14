package zw.zimlms.core.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class SchoolRoleStatus { VERIFIED, CHANGED, DEACTIVATED }

/** Authoritative school roles always come from public.users under a signed-in JWT.
 * A locally cached PIN/profile never creates a fresh staff or guardian permission.
 */
internal fun evaluateSchoolRole(
 cached:CachedProfile,actualId:String,actualSchool:String,
 actualRole:String,isActive:Boolean,isDeleted:Boolean
):SchoolRoleStatus{
 if(actualId!=cached.userId || actualSchool!=cached.schoolId ||
    !isActive || isDeleted)return SchoolRoleStatus.DEACTIVATED
 if(actualRole!=cached.role)return SchoolRoleStatus.CHANGED
 return SchoolRoleStatus.VERIFIED
}

class SchoolRoleVerifier(
 private val baseUrl:String,private val publishableKey:String,
 private val broker:SessionBroker,
 private val http:OkHttpClient=OkHttpClient.Builder()
  .connectTimeout(7,TimeUnit.SECONDS).readTimeout(10,TimeUnit.SECONDS).build()
){
 init{require(baseUrl.startsWith("https://") && publishableKey.startsWith("sb_publishable_"))}
 suspend fun verify(cached:CachedProfile):SchoolRoleStatus=withContext(Dispatchers.IO){
  val id=Regex("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
  require(id.matches(cached.userId)&&id.matches(cached.schoolId))
  val token=broker.accessToken(cached.userId,cached.schoolId)
  val url=baseUrl.trimEnd('/')+"/rest/v1/users?select=id,school_id,role,is_active,deleted_at" +
    "&id=eq."+cached.userId+"&limit=1"
  val req=Request.Builder().url(url)
   .header("apikey",publishableKey)
   .header("Authorization","Bearer "+token)
   .header("Accept","application/json")
   .get().build()
  http.newCall(req).execute().use{response->
   if(response.code==401||response.code==403)return@withContext SchoolRoleStatus.DEACTIVATED
   if(!response.isSuccessful)throw IOException("Online school role verification unavailable")
   val raw=response.body?.string()?:"[]"
   if(raw.length>15_000)throw IOException("Unexpected role-verification response")
   val rows=JSONArray(raw)
   if(rows.length()!=1)return@withContext SchoolRoleStatus.DEACTIVATED
   val row=rows.getJSONObject(0)
   evaluateSchoolRole(cached,row.getString("id"),row.getString("school_id"),
    row.getString("role"),row.optBoolean("is_active",false),
    !row.isNull("deleted_at"))
  }
 }
}
