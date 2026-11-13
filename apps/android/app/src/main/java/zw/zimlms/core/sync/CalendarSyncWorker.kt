package zw.zimlms.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import zw.zimlms.BuildConfig
import zw.zimlms.core.auth.SessionBroker
import zw.zimlms.core.auth.SessionVault
import java.io.IOException

/**
 * Android schedules this at its discretion (minimum 15-minute interval, not
 * guaranteed exact). Requires network connectivity. Never stores access tokens
 * or personal records in WorkManager input Data.
 */
class CalendarSyncWorker(
  context: Context,
  parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

  override suspend fun doWork(): Result {
    val context = applicationContext
    val session = SessionVault(context).load() ?: return Result.success()
    if (!BuildConfig.SUPABASE_URL.startsWith("https://") ||
        !BuildConfig.SUPABASE_PUBLISHABLE_KEY.startsWith("sb_publishable_")) {
      return Result.failure()
    }

    val broker = SessionBroker(context,
      BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY)
    val database = OfflineDatabase.open(context,
      session.profile.userId, session.profile.schoolId)
    return try {
      val transport = SyncHttpApi(
        baseUrl = BuildConfig.SUPABASE_URL,
        publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        deviceId = session.deviceId,
        accessToken = { broker.accessToken(session.profile.userId, session.profile.schoolId) }
      )
      SyncEngine(database, transport, session.profile.schoolId).syncNow()
      Result.success()
    } catch (_: IOException) {
      // Transient provider/network errors never erase the durable Room queue.
      if (broker.peek() == null) Result.success() else Result.retry()
    } catch (_: IllegalArgumentException) {
      // Corrupt response/cursor or invalid local data must not endlessly retry.
      Result.failure()
    } catch (_: IllegalStateException) {
      Result.retry()
    } finally {
      database.close()
    }
  }
}
