package zw.zimlms.core.sync

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import org.json.JSONObject

/**
 * Phase 1: calendar-only offline cache. Keep student/parent PII OUT of this
 * unencrypted database until a reviewed at-rest encryption layer is installed.
 */
@Entity(tableName = "calendar_events", indices = [Index("schoolId")])
data class CalendarEventCache(
  @PrimaryKey val id: String,
  val schoolId: String,
  val rowJson: String,
  val syncVersion: Int,
  val deletedAt: String? = null
) {
  companion object {
    fun fromServer(row: JSONObject): CalendarEventCache {
      val id = row.getString("id")
      val schoolId = row.getString("school_id")
      return CalendarEventCache(
        id = id, schoolId = schoolId, rowJson = row.toString(),
        syncVersion = row.getInt("sync_version"),
        deletedAt = row.optString("deleted_at").takeUnless { it.isBlank() || it == "null" }
      )
    }
  }
}

@Entity(tableName = "sync_queue", indices = [
  Index("entityId"), Index("status"), Index(value = ["createdAtMillis", "opId"])
])
data class SyncQueueEntity(
  @PrimaryKey val opId: String,
  val entity: String = "calendar_events",
  val entityId: String,
  val opType: String,
  val payloadJson: String,
  val clientTs: String,
  val baseVersion: Int?,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val attempts: Int = 0,
  val status: String = "queued",
  val conflictDetail: String? = null
)

@Entity(tableName = "sync_meta")
data class SyncMetaEntity(@PrimaryKey val key: String, val value: String)

@Dao
interface CalendarCacheDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(row: CalendarEventCache)

  @Query("SELECT * FROM calendar_events WHERE schoolId = :school ORDER BY id")
  suspend fun allForSchool(school: String): List<CalendarEventCache>

  @Query("SELECT * FROM calendar_events WHERE id = :id LIMIT 1")
  suspend fun get(id: String): CalendarEventCache?
}

@Dao
interface SyncQueueDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun enqueue(op: SyncQueueEntity)

  @Query("SELECT * FROM sync_queue WHERE status = 'queued' ORDER BY createdAtMillis, opId LIMIT :limit")
  suspend fun pending(limit: Int): List<SyncQueueEntity>

  @Query("SELECT * FROM sync_queue ORDER BY createdAtMillis, opId")
  suspend fun all(): List<SyncQueueEntity>

  @Query("SELECT * FROM sync_queue WHERE opId = :opId LIMIT 1")
  suspend fun get(opId: String): SyncQueueEntity?

  @Query("SELECT * FROM sync_queue WHERE entityId = :entityId AND status = 'queued' AND opType <> 'insert' ORDER BY createdAtMillis, opId LIMIT 1")
  suspend fun nextDependent(entityId: String): SyncQueueEntity?

  @Update suspend fun update(op: SyncQueueEntity)

  @Query("DELETE FROM sync_queue WHERE opId = :opId")
  suspend fun remove(opId: String)
}

@Dao
interface SyncMetaDao {
  @Query("SELECT value FROM sync_meta WHERE key = :key LIMIT 1")
  suspend fun get(key: String): String?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun put(item: SyncMetaEntity)
}

@Database(
  entities = [CalendarEventCache::class, SyncQueueEntity::class, SyncMetaEntity::class],
  version = 1,
  exportSchema = true
)
abstract class OfflineDatabase : RoomDatabase() {
  abstract fun calendar(): CalendarCacheDao
  abstract fun queue(): SyncQueueDao
  abstract fun meta(): SyncMetaDao

  companion object {
    /**
     * Call only with trusted, authenticated profile identity. Database names
     * separate accounts on shared school tablets. No destructive migrations.
     */
    fun open(context: Context, userId: String, schoolId: String): OfflineDatabase {
      val uuid = Regex("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
      require(uuid.matches(userId) && uuid.matches(schoolId)) { "Verified user and school required" }
      val name = "zimlms-${userId.lowercase()}-${schoolId.lowercase()}.db"
      return Room.databaseBuilder(context.applicationContext, OfflineDatabase::class.java, name)
        .build()
    }
  }
}
