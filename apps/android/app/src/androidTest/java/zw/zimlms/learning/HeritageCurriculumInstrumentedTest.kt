package zw.zimlms.learning

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on an emulator with no network requirement: validates bundled lessons. */
@RunWith(AndroidJUnit4::class)
class HeritageCurriculumInstrumentedTest {
 @Test fun sharedCatalogIsAvailableOfflineForEveryCoveredGrade() {
   val context=InstrumentationRegistry.getInstrumentation().targetContext
   val lessons=HeritageCurriculum.load(context)
   assertEquals(47, lessons.size)
   assertEquals(47, lessons.map { it.id }.distinct().size)
   for(level in listOf("ECD A","ECD B","Grade 1","Grade 2","Grade 3",
       "Grade 4","Grade 5","Grade 6","Grade 7")) {
     assertTrue("Missing level: $level", lessons.any { it.level==level })
   }
   for(subject in listOf("Mathematics","Science and Technology",
       "English Language","Indigenous Languages")) {
     assertTrue("Missing subject: $subject", lessons.any { it.subject==subject })
   }
   assertTrue("Lessons must have substantive explanations, examples and quizzes",
     lessons.all { it.objective.length>15 && it.explanation.size>=3 &&
       it.workedExample.length>20 && it.exercises.size==3 &&
       it.assignmentInstructions.isNotBlank() && it.choices.size==3 &&
       it.correctIndex in it.choices.indices })
 }
}
