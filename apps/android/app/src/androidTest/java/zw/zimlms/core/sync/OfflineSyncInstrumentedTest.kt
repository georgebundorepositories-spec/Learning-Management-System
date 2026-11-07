package zw.zimlms.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class OfflineSyncInstrumentedTest {
  private val school = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
  private val user = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"

  @Test fun queuedCalendarChangeSurvivesDatabaseReopening() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val name = "zimlms-${user}-${school}.db"
    context.deleteDatabase(name)
    val db1 = OfflineDatabase.open(context, user, school)
    db1.queue().enqueue(SyncQueueEntity(
      opId = "11111111-1111-4111-8111-111111111111",
      entityId = "eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee1",
      opType = "insert",
      payloadJson = "{\"title\":\"Saved offline\"}",
      clientTs = "2026-11-04T08:00:00Z", baseVersion = null
    ))
    db1.close()
    val db2 = OfflineDatabase.open(context, user, school)
    try {
      assertEquals(1, db2.queue().pending(100).size)
      assertEquals("Saved offline",
        JSONObject(db2.queue().pending(1)[0].payloadJson).getString("title"))
    } finally {
      db2.close()
      context.deleteDatabase(name)
    }
  }

  @Test fun twoCachesConvergeAfterFiftyOperationsEach() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val a = Room.inMemoryDatabaseBuilder(context, OfflineDatabase::class.java).build()
    val b = Room.inMemoryDatabaseBuilder(context, OfflineDatabase::class.java).build()

    class Server {
      var seq = 0
      val log = mutableListOf<Pair<Int, JSONObject>>()
      val seen = mutableMapOf<String, JSONObject>()
      fun api() = object : SyncApi {
        override suspend fun push(op: SyncQueueEntity): PushOutcome {
          val prior = seen[op.opId]
          if (prior != null) return PushOutcome("applied", prior)
          if (op.opType != "insert") return PushOutcome("rejected", conflictDetail = "unsupported_in_test")
          val event = JSONObject(op.payloadJson)
            .put("id", op.entityId).put("school_id", school)
            .put("sync_version", 1).put("deleted_at", JSONObject.NULL)
            .put("approval_status", "pending").put("created_by", user)
          seen[op.opId] = event
          log.add(Pair(++seq, event))
          return PushOutcome("applied", event)
        }
        override suspend fun pull(cursor: String, limit: Int): PullPage {
          val changes = log.filter { it.first > cursor.toInt() }
          val page = changes.take(limit)
          return PullPage(page.map { it.second },
            (page.lastOrNull()?.first ?: cursor.toInt()).toString(),
            changes.size > page.size)
        }
      }
    }
    val server = Server()
    try {
      for (i in 1..100) {
        val op = SyncQueueEntity(
          opId = UUID.nameUUIDFromBytes("op-$i".toByteArray()).toString(),
          entityId = UUID.nameUUIDFromBytes("event-$i".toByteArray()).toString(),
          opType = "insert",
          payloadJson = JSONObject()
            .put("title", "Event $i").put("start_at","2026-12-01T08:00:00Z")
            .put("end_at","2026-12-01T09:00:00Z").toString(),
          clientTs = "2026-11-04T08:00:00Z",baseVersion = null,
          createdAtMillis = i.toLong()
        )
        if (i <= 50) a.queue().enqueue(op) else b.queue().enqueue(op)
      }
      val engineA = SyncEngine(a,server.api(),school)
      val engineB = SyncEngine(b,server.api(),school)
      assertEquals(50,engineA.syncNow().pushed)
      assertEquals(50,engineB.syncNow().pushed)
      engineA.syncNow()
      assertEquals(100,a.calendar().allForSchool(school).size)
      assertEquals(100,b.calendar().allForSchool(school).size)
      assertEquals("100",a.meta().get("calendar_cursor"))
      assertEquals("100",b.meta().get("calendar_cursor"))
      assertTrue(a.queue().pending(100).isEmpty())
      assertTrue(b.queue().pending(100).isEmpty())
    } finally {
      a.close()
      b.close()
    }
  }

  @Test fun manualConflictKeepsOriginalOperationForReview() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, OfflineDatabase::class.java).build()
    try {
      db.queue().enqueue(SyncQueueEntity(
        opId="22222222-2222-4222-8222-222222222222",
        entityId="eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee2",opType="update",
        payloadJson="{\"title\":\"Conflict\"}",baseVersion=1,
        clientTs="2026-11-04T08:00:00Z"
      ))
      val api = object: SyncApi {
        override suspend fun push(op: SyncQueueEntity) = PushOutcome("conflict", conflictDetail="stale_version")
        override suspend fun pull(cursor: String,limit: Int) =
          PullPage(emptyList(),cursor,false)
      }
      val outcome=SyncEngine(db,api,school).syncNow()
      assertEquals(1,outcome.conflicts)
      assertEquals("conflict",db.queue().all().single().status)
      assertEquals("stale_version",db.queue().all().single().conflictDetail)
    } finally { db.close() }
  }
}
