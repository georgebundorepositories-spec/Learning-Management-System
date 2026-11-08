package zw.zimlms.core.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * User-JWT transport. A trusted session broker supplies a current access token.
 * Never embed a service-role key or phone password in the APK.
 */
class SyncHttpApi(
  private val baseUrl: String,
  private val publishableKey: String,
  private val deviceId: String,
  private val accessToken: suspend () -> String,
  private val http: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS).build()
) : SyncApi {
  init {
    require(baseUrl.startsWith("https://")) { "Sync requires HTTPS" }
    require(publishableKey.startsWith("sb_publishable_")) { "Publishable key required" }
    require(deviceId.matches(Regex("^[0-9a-fA-F-]{36}$"))) { "Valid device UUID required" }
  }

  private suspend fun send(path: String, method: String, body: JSONObject? = null): JSONObject =
    withContext(Dispatchers.IO) {
      val token = accessToken()
      if (token.isBlank()) throw IOException("No authenticated session for sync")
      val requestBody = body?.toString()
        ?.toRequestBody("application/json".toMediaType())
      val url = baseUrl.trimEnd('/') + "/functions/v1/" + path
      val request = Request.Builder().url(url)
        .header("apikey", publishableKey)
        .header("Authorization", "Bearer $token")
        .header("Cache-Control", "no-store")
      when (method) {
        "POST" -> request.post(requestBody ?: throw IOException("Missing request body"))
        "GET" -> request.get()
        else -> throw IOException("Unsupported sync request")
      }
      http.newCall(request.build()).execute().use { response ->
        if (!response.isSuccessful) throw IOException(
          "Sync server returned ${response.code}; local queue preserved"
        )
        JSONObject(response.body?.string() ?: throw IOException("Empty sync response"))
      }
    }

  override suspend fun push(op: SyncQueueEntity): PushOutcome {
    val payload = JSONObject(op.payloadJson)
    val request = JSONObject()
      .put("device_id", deviceId)
      .put("client_version", "0.2.0-p1")
      .put("ops", JSONArray().put(
        JSONObject()
          .put("op_id", op.opId).put("entity", op.entity).put("entity_id", op.entityId)
          .put("op_type", op.opType).put("payload", payload)
          .put("base_version", op.baseVersion).put("client_ts", op.clientTs)
      ))
    val response = send("sync-push", "POST", request)
    val results = response.optJSONArray("results")
      ?: throw IOException("Invalid push acknowledgement")
    if (results.length() != 1) throw IOException("Unexpected push response length")
    val item = results.getJSONObject(0)
    if (item.optString("op_id") != op.opId) throw IOException("Operation ID mismatch")
    val status = item.optString("result")
    if (status !in setOf("applied","conflict","rejected")) {
      throw IOException("Invalid server result")
    }
    return PushOutcome(
      result = status,
      serverRow = item.optJSONObject("server_row"),
      conflictDetail = item.optJSONObject("conflict_detail")?.toString()
    )
  }

  override suspend fun pull(cursor: String, limit: Int): PullPage {
    if (!cursor.matches(Regex("^(0|[1-9]\\d{0,18})$")) || limit !in 1..500) {
      throw IOException("Invalid pull cursor or limit")
    }
    val response = send("sync-pull?cursor=$cursor&entities=calendar_events&limit=$limit", "GET")
    val changes = response.getJSONObject("changes").getJSONArray("calendar_events")
    val rows = (0 until changes.length()).map { changes.getJSONObject(it) }
    return PullPage(
      rows = rows,
      nextCursor = response.getString("next_cursor"),
      hasMore = response.getBoolean("has_more")
    )
  }
}
