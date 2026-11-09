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

/** School-scoped, read-only financial and communication records.
 *
 * No fee payments, settlement, receipts, SMS or push are performed by this
 * Android client. Learner/guardian visibility is enforced by the server RLS.
 * Responses are not written to the offline Room database.
 */
data class PortalNotice(
  val id:String, val title:String, val body:String, val audience:String, val createdAt:String
)
data class PortalInvoice(
  val id:String, val description:String, val amount:String,
  val currency:String, val dueOn:String?, val status:String
)

class SchoolOperationsRepository(
  private val baseUrl:String,
  private val publishableKey:String,
  private val broker:SessionBroker,
  private val http:OkHttpClient=OkHttpClient.Builder()
    .connectTimeout(10,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).build()
) {
  private val uuid=Regex("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
  init { require(baseUrl.startsWith("https://") && publishableKey.startsWith("sb_publishable_")) }

  private suspend fun read(actor:CachedProfile,resource:String):List<JSONObject> =
    withContext(Dispatchers.IO) {
      require(uuid.matches(actor.userId) && uuid.matches(actor.schoolId))
      val token=broker.accessToken(actor.userId,actor.schoolId)
      val request=Request.Builder()
        .url("${baseUrl.trimEnd('/')}/rest/v1/$resource")
        .header("apikey",publishableKey)
        .header("Authorization","Bearer $token")
        .header("Accept","application/json")
        .get().build()
      http.newCall(request).execute().use {response ->
        if(!response.isSuccessful)
          throw IOException("School records could not be loaded (HTTP ${response.code})")
        val data=response.body?.string()?:"[]"
        if(data.length>500_000)throw IOException("School response exceeded safety limit")
        val array=JSONArray(data)
        (0 until array.length()).map {array.getJSONObject(it)}
      }
    }

  suspend fun notices(actor:CachedProfile):List<PortalNotice> {
    val rows=read(actor,"school_notices?select=id,title,body,audience_role,created_at" +
      "&school_id=eq.${actor.schoolId}&order=created_at.desc&limit=100")
    return rows.map { row ->
      PortalNotice(row.getString("id"),row.getString("title"),row.getString("body"),
        row.optString("audience_role","all"),row.getString("created_at"))
    }
  }

  suspend fun invoices(actor:CachedProfile):List<PortalInvoice> {
    require(actor.role in setOf("admin","head","bursar","parent","student"))
    val rows=read(actor,"fee_invoices?select=id,description,amount,currency,due_on,status" +
      "&school_id=eq.${actor.schoolId}&order=created_at.desc&limit=100")
    return rows.map { row ->
      val due=row.optString("due_on").takeUnless{it.isBlank()||it=="null"}
      PortalInvoice(row.getString("id"),row.getString("description"),
        row.getString("amount"),row.getString("currency"),due,
        row.getString("status"))
    }
  }
}
