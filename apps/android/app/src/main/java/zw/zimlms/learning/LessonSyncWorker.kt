package zw.zimlms.learning

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import zw.zimlms.BuildConfig
import zw.zimlms.core.auth.SessionBroker
import zw.zimlms.core.auth.SessionVault
import java.io.IOException

/** Replays encrypted completions only with the current signed-in learner JWT. */
class LessonSyncWorker(context:Context,parameters:WorkerParameters)
  : CoroutineWorker(context,parameters) {

  override suspend fun doWork():Result {
    val session=try { SessionVault(applicationContext).load() }
      catch(_:IllegalStateException){return Result.failure()}
      ?:return Result.success()
    if(session.profile.role!="student")return Result.success()
    val url=BuildConfig.SUPABASE_URL
    val key=BuildConfig.SUPABASE_PUBLISHABLE_KEY
    if(!url.startsWith("https://")||!key.startsWith("sb_publishable_"))
      return Result.success() // Local-only classroom preview.
    val broker=SessionBroker(applicationContext,url,key)
    return try {
      LessonCloudSync(url,key,broker).synchronize(
        session.profile,LessonProgressVault(applicationContext))
      Result.success()
    }catch(_:IOException){
      if(broker.peek()==null)Result.success() else Result.retry()
    }catch(_:IllegalArgumentException){Result.failure()}
     catch(_:IllegalStateException){Result.failure()}
  }
}
