package zw.zimlms.portals

import org.junit.Assert.*
import org.junit.Test
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.learning.StarterLessons

class TeacherPlanEligibilityTest {
 private val teacher=CachedProfile("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","Teacher","teacher",
   "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")
 private val math=PortalSubject("cccccccc-cccc-4ccc-8ccc-cccccccccccc","Maths")
 private val english=PortalSubject("dddddddd-dddd-4ddd-8ddd-dddddddddddd","English")
 private val klass=PortalClass("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee","Grade 3A","Grade 3")
 @Test fun gradeAndSubjectAliasesMatchWeb() {
  assertEquals("Grade 3",canonicalPlanGrade("Grade 3"))
  assertEquals("ECD A",canonicalPlanGrade("ECD A"))
  assertEquals("Grade 7",canonicalPlanGrade("7"))
  assertEquals("",canonicalPlanGrade("Form 2"))
  val lesson=StarterLessons.lessons.first{it.id=="g3-multiply"}
  assertTrue(lessonFitsClass(lesson,klass))
  assertTrue(lessonFitsSubject(lesson,math))
  assertFalse(lessonFitsSubject(lesson,english))
 }
 @Test fun unassignedTeacherCannotCreatePairs() {
  val dir=SchoolDirectory("School",false,listOf(klass),listOf(math,english))
  assertTrue(teacherPlanPairs(teacher,dir,StarterLessons.lessons).isEmpty())
 }
 @Test fun subjectAssignmentRestrictsCrossSubjectPlanning(){
  val dir=SchoolDirectory("School",false,listOf(klass),listOf(math,english),
   assignments=listOf(PortalAssignment(klass.id,math.id)))
  val pairs=teacherPlanPairs(teacher,dir,StarterLessons.lessons)
  assertEquals(1,pairs.size)
  assertEquals(math.id,pairs.single().subject.id)
  assertTrue(pairs.single().lessons.any{it.id=="g3-multiply"})
 }
 @Test fun homeroomTeacherCanPrepareAssignedGrade(){
  val dir=SchoolDirectory("School",false,listOf(klass.copy(teacherId=teacher.userId)),
   listOf(math,english))
  val pairs=teacherPlanPairs(teacher,dir,StarterLessons.lessons)
  assertTrue(pairs.any{it.subject.id==math.id})
  assertTrue(pairs.any{it.subject.id==english.id})
 }
 @Test fun learnerOrParentNeverGetsPlanningAssignments() {
  val dir=SchoolDirectory("School",false,listOf(klass.copy(teacherId=teacher.userId)),
   listOf(math,english))
  for(role in listOf("student","parent")){
   assertTrue(teacherPlanPairs(teacher.copy(role=role),dir,StarterLessons.lessons).isEmpty())
  }
 }
 @Test fun draftValidationRejectsTamperingAndInvalidDates(){
  val input=TeacherPlanDraft(klass.id,math.id,"g3-multiply",
   "Multiplication","Learners identify equal groups","Use counters to demonstrate groups of three.",
   "", "", "",null,"draft")
  assertTrue(validateTeacherPlanDraft(input))
  assertFalse(validateTeacherPlanDraft(input.copy(classId="outside-school")))
  assertFalse(validateTeacherPlanDraft(input.copy(plannedOn="2026-02-30")))
  assertFalse(validateTeacherPlanDraft(input.copy(status="published")))
  assertFalse(validateTeacherPlanDraft(input.copy(steps="short")))
  assertTrue(validateTeacherPlanDraft(input.copy(plannedOn="2026-11-10",status="ready")))
 }
}
