package zw.zimlms.portals

import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.learning.StarterLesson
import java.util.Locale

/**
 * Classroom/subject pairing mirrors web curriculum matching.
 * Client filtering improves UX; the database remains the only security authority.
 */
data class PlanTeachingPair(
  val classroom:PortalClass,
  val subject:PortalSubject,
  val lessons:List<StarterLesson>
) {
  val key:String get() = classroom.id + ":" + subject.id
  val label:String get() = classroom.name + " · " + subject.name
}

internal fun canonicalPlanGrade(text:String):String {
  val normal=text.lowercase(Locale.ROOT).trim()
  val ecd=Regex("""ecd\s*([ab])""").find(normal)?.groupValues?.get(1)
  if(ecd!=null)return "ECD " + ecd.uppercase(Locale.ROOT)
  val grade=Regex("""(?:grade\s*|^)([1-7])(?:\b|$)""").find(normal)?.groupValues?.get(1)
  return if(grade!=null)"Grade " + grade else ""
}

internal fun lessonFitsClass(lesson:StarterLesson,schoolClass:PortalClass):Boolean {
  val grade=canonicalPlanGrade(schoolClass.grade).ifBlank{canonicalPlanGrade(schoolClass.name)}
  return grade.isNotBlank() && grade==lesson.level
}

internal fun lessonFitsSubject(lesson:StarterLesson,subject:PortalSubject):Boolean {
  val a=lesson.subject.trim().lowercase(Locale.ROOT)
  val b=subject.name.trim().lowercase(Locale.ROOT)
  if(a==b)return true
  val aliases=mapOf(
    "english language" to setOf("english","english language"),
    "science and technology" to setOf("science","science and technology","science & technology"),
    "mathematics" to setOf("maths","mathematics","math"),
    "indigenous languages" to setOf("indigenous languages","shona","ndebele",
      "kalanga","tonga","venda","nambya","sotho","xhosa","chewa","tswana",
      "chibarwe","khoisan","sign language")
  )
  return b in (aliases[a]?:emptySet())
}

/** Only the verified active classroom/subject assignment is selectable.
 * All records in SchoolDirectory were already read using the user's JWT + RLS.
 */
fun teacherPlanPairs(
 profile:CachedProfile,directory:SchoolDirectory?,lessons:List<StarterLesson>
):List<PlanTeachingPair> {
 if(profile.role!="teacher"||directory==null)return emptyList()
 return directory.classes.flatMap { schoolClass ->
  directory.subjects.mapNotNull { subject ->
   val assigned=schoolClass.teacherId==profile.userId ||
     directory.assignments.any { it.classId==schoolClass.id &&
       it.subjectId==subject.id &&
       (it.teacherId.isBlank() || it.teacherId==profile.userId) }
   // For homeroom teachers, teacher_id alone grants all subjects, matching
   // app_private.can_manage_assessment. Subject assignment rows are otherwise
   // restricted to this teacher by SchoolDirectoryRepository.
   if(!assigned)return@mapNotNull null
   val matched=lessons.filter { lessonFitsClass(it,schoolClass)&&lessonFitsSubject(it,subject) }
   if(matched.isEmpty())null else PlanTeachingPair(schoolClass,subject,matched)
  }
 }
}
