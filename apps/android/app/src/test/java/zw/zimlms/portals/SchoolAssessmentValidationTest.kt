package zw.zimlms.portals

import org.junit.Assert.*
import org.junit.Test
import zw.zimlms.core.auth.CachedProfile

class SchoolAssessmentValidationTest{
 private val actor=CachedProfile(
  "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
  "Mathematics Teacher","teacher",
  "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")
 private val klass=PortalClass("cccccccc-cccc-4ccc-8ccc-cccccccccccc",
  "Grade 4A","Grade 4")
 private val maths=PortalSubject("dddddddd-dddd-4ddd-8ddd-dddddddddddd","Mathematics")
 private val english=PortalSubject("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee","English")
 @Test fun onlyAssignedClassSubjectIsAllowed(){
  val dir=SchoolDirectory("Local",false,listOf(klass),listOf(maths,english),
   assignments=listOf(PortalAssignment(klass.id,maths.id)))
  val pairs=assessmentPairs(actor,dir)
  assertEquals(1,pairs.size)
  assertEquals(maths.id,pairs.single().subject.id)
  assertTrue(assessmentPairs(actor.copy(role="student"),dir).isEmpty())
  assertTrue(assessmentPairs(actor.copy(role="parent"),dir).isEmpty())
  assertEquals(2,assessmentPairs(actor.copy(role="admin"),dir).size)
 }
 @Test fun homeroomTeacherCanRecordAllOwnClassSubjects(){
  val dir=SchoolDirectory("Local",false,
   listOf(klass.copy(teacherId=actor.userId)),listOf(maths,english))
  assertEquals(2,assessmentPairs(actor,dir).size)
 }
 @Test fun datesAndMarksAreStrict(){
  val pair=AssessmentTeachingPair(klass,maths)
  assertTrue(validAssessmentDraft(pair,"Term Test","100","2026-11-11"))
  assertFalse(validAssessmentDraft(pair,"T","100","2026-11-11"))
  assertFalse(validAssessmentDraft(pair,"Test","0","2026-11-11"))
  assertFalse(validAssessmentDraft(pair,"Test","10000.01","2026-11-11"))
  assertFalse(validAssessmentDraft(pair,"Test","100","2026-02-30"))
  assertFalse(validAssessmentDraft(pair,"Test","100","2026-11-11x"))
  assertTrue(validCourseworkMark("0","100"))
  assertTrue(validCourseworkMark("99.25","100"))
  assertFalse(validCourseworkMark("100.01","100"))
  assertFalse(validCourseworkMark("-1","100"))
 }
}
