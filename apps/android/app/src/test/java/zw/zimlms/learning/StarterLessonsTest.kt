package zw.zimlms.learning

import org.junit.Assert.*
import org.junit.Test

class StarterLessonsTest {
  @Test fun lessonsContainSubstantiveOfflineContentAndQuizAnswers() {
    assertEquals(12,StarterLessons.lessons.size)
    assertEquals(12,StarterLessons.lessons.map { it.id }.toSet().size)
    assertTrue(StarterLessons.lessons.all { item ->
      item.title.length > 6 &&
      item.objective.length > 20 &&
      item.explanation.size >= 3 &&
      item.explanation.all { it.length > 60 } &&
      item.workedExample.length > 40 &&
      item.question.length > 10 &&
      item.choices.size == 3 &&
      item.correctIndex in item.choices.indices &&
      item.feedback.length > 20
    })
  }

  @Test fun primaryAndSecondaryContentBothExist() {
    val levels = StarterLessons.lessons.map { it.level }.toSet()
    for (level in listOf("ECD A","Grade 3","Grade 5","Grade 7",
      "Form 1","Form 2","Form 3","Form 4")) {
      assertTrue("Missing learning level: $level", level in levels)
    }
  }
}
