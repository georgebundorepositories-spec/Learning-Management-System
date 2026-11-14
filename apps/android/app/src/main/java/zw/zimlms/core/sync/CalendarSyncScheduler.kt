package zw.zimlms.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedule only after online login and local PIN enrollment have succeeded.
 * WorkManager has a minimum 15-minute repeat interval, but Doze and network
 * limits can defer execution. No work is sent while the device is offline.
 */
object CalendarSyncScheduler {
  const val PERIODIC_NAME = "zimschool-calendar-periodic-v1"
  const val IMMEDIATE_NAME = "zimschool-calendar-reconnect-v1"

  private fun networkRequired() = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

  fun schedule(context: Context) {
    val worker = PeriodicWorkRequestBuilder<CalendarSyncWorker>(15, TimeUnit.MINUTES)
      .setConstraints(networkRequired())
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
      .addTag("zimschool-calendar-sync")
      .build()
    WorkManager.getInstance(context.applicationContext)
      .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, worker)
    requestImmediate(context)
  }

  fun requestImmediate(context: Context) {
    val worker = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
      .setConstraints(networkRequired())
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
      .addTag("zimschool-calendar-sync")
      .build()
    WorkManager.getInstance(context.applicationContext)
      .enqueueUniqueWork(IMMEDIATE_NAME, ExistingWorkPolicy.KEEP, worker)
  }

  fun cancel(context: Context) {
    val manager = WorkManager.getInstance(context.applicationContext)
    manager.cancelUniqueWork(PERIODIC_NAME)
    manager.cancelUniqueWork(IMMEDIATE_NAME)
  }
}
