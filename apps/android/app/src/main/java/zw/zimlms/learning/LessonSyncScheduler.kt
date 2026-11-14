package zw.zimlms.learning

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

/** Never persist tokens or learner IDs in WorkManager inputs. */
object LessonSyncScheduler {
  private const val PERIODIC="zimschool-lesson-sync-periodic-v1"
  private const val IMMEDIATE="zimschool-lesson-sync-immediate-v1"
  private fun online()=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

  fun schedule(context:Context) {
    val work=PeriodicWorkRequestBuilder<LessonSyncWorker>(15,TimeUnit.MINUTES)
      .setConstraints(online())
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS)
      .addTag("zimschool-lesson-sync").build()
    WorkManager.getInstance(context.applicationContext)
      .enqueueUniquePeriodicWork(PERIODIC,ExistingPeriodicWorkPolicy.UPDATE,work)
    requestImmediate(context)
  }

  fun requestImmediate(context:Context) {
    val work=OneTimeWorkRequestBuilder<LessonSyncWorker>()
      .setConstraints(online())
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS)
      .addTag("zimschool-lesson-sync").build()
    // Keep the newest offline completion even if an earlier retry is pending.
    WorkManager.getInstance(context.applicationContext)
      .enqueueUniqueWork(IMMEDIATE,ExistingWorkPolicy.APPEND_OR_REPLACE,work)
  }

  fun cancel(context:Context) {
    val manager=WorkManager.getInstance(context.applicationContext)
    manager.cancelUniqueWork(PERIODIC)
    manager.cancelUniqueWork(IMMEDIATE)
  }
}
