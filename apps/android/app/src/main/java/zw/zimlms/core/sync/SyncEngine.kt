package zw.zimlms.core.sync

import androidx.room.withTransaction
import java.math.BigInteger
import org.json.JSONObject

data class PushOutcome(
  val result: String,
  val serverRow: JSONObject? = null,
  val conflictDetail: String? = null
)
data class PullPage(
  val rows: List<JSONObject>,
  val nextCursor: String,
  val hasMore: Boolean
)
interface SyncApi {
  suspend fun push(op: SyncQueueEntity): PushOutcome
  suspend fun pull(cursor: String, limit: Int): PullPage
}
data class SyncSummary(
  val pushed: Int, val conflicts: Int, val rejected: Int, val pulled: Int, val cursor: String
)

/**
 * Phase 1 engine: ordered, durable push -> pull. Every operation is acknowledged
 * transactionally. A lost network response never deletes a pending operation;
 * the server deduplicates retried op_id values.
 *
 * Call this only with a verified session and a queue bound to the authenticated
 * user's school. Persistent token refresh and scheduled WorkManager execution
 * remain gated until a secure session store exists.
 */
class SyncEngine(
  private val database: OfflineDatabase,
  private val api: SyncApi,
  private val schoolId: String
) {
  private val queue get() = database.queue()
  private val meta get() = database.meta()
  private val calendar get() = database.calendar()

  private fun cursorNumber(value: String): BigInteger {
    require(value.matches(Regex("^(0|[1-9]\\d{0,18})$"))) { "Invalid server cursor" }
    val number = BigInteger(value)
    require(number <= BigInteger("9223372036854775807")) { "Cursor out of range" }
    return number
  }

  suspend fun syncNow(): SyncSummary {
    var pushed = 0
    var conflicts = 0
    var rejected = 0
    var pulled = 0

    for (attempt in 0 until 100) {
      val op = queue.pending(1).firstOrNull() ?: break
      require(op.entity == "calendar_events") { "Unrecognised sync entity" }
      val outcome = api.push(op)
      if (outcome.result !in setOf("applied", "conflict", "rejected")) {
        error("Invalid server acknowledgement")
      }
      val row = if (outcome.result == "applied")
        CalendarEventCache.fromServer(outcome.serverRow
          ?: error("Applied acknowledgement missing server row")) else null
      if (row != null && (row.schoolId != schoolId || row.id != op.entityId)) {
        error("Server returned mismatched tenant/entity")
      }

      database.withTransaction {
        val current = queue.get(op.opId) ?: return@withTransaction
        if (current.status != "queued") return@withTransaction
        when (outcome.result) {
          "applied" -> {
            calendar.upsert(row!!)
            queue.remove(op.opId)
            val next = queue.nextDependent(op.entityId)
            if (next != null) queue.update(next.copy(baseVersion = row.syncVersion))
          }
          "conflict" -> queue.update(current.copy(
            status = "conflict", conflictDetail = outcome.conflictDetail
          ))
          "rejected" -> queue.update(current.copy(
            status = "rejected", conflictDetail = outcome.conflictDetail
          ))
        }
      }
      when (outcome.result) {
        "applied" -> pushed++
        "conflict" -> conflicts++
        "rejected" -> rejected++
      }
    }

    var cursor = meta.get("calendar_cursor") ?: "0"
    cursorNumber(cursor)
    repeat(1000) {
      val page = api.pull(cursor, 500)
      val next = cursorNumber(page.nextCursor)
      require(next >= cursorNumber(cursor)) { "Server cursor moved backwards" }
      require(!page.hasMore || next > cursorNumber(cursor)) {
        "Server returned a non-advancing page"
      }
      // Parse all rows before opening the transaction to avoid partial writes.
      val changes = page.rows.map(CalendarEventCache::fromServer)
      require(changes.all { it.schoolId == schoolId }) { "Cross-school pull refused" }
      database.withTransaction {
        changes.forEach { calendar.upsert(it) }
        meta.put(SyncMetaEntity("calendar_cursor",page.nextCursor))
      }
      cursor = page.nextCursor
      pulled += changes.size
      if (!page.hasMore) return SyncSummary(pushed, conflicts, rejected, pulled, cursor)
    }
    error("Exceeded safe number of sync pages")
  }
}
