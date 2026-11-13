package zw.zimlms.learning

import org.junit.Assert.assertEquals
import org.junit.Test

class LessonSyncPlannerTest {
  @Test fun offlineAndCloudSetsConvergeWithoutDeletion() {
    val plan=planLessonSync(setOf("local-a","shared"),setOf("shared","remote-b"))
    assertEquals(setOf("local-a"),plan.upload)
    assertEquals(setOf("local-a","shared","remote-b"),plan.merged)
  }
  @Test fun repeatedSynchronisationIsIdempotent() {
    val done=setOf("lesson-1","lesson-2")
    val plan=planLessonSync(done,done)
    assertEquals(emptySet<String>(),plan.upload)
    assertEquals(done,plan.merged)
  }
  @Test fun emptyCloudStillUploadsPreviouslyCompletedLessons() {
    val done=setOf("lesson-1")
    assertEquals(done,planLessonSync(done,emptySet()).upload)
  }
}
