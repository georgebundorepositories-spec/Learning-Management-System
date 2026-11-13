package zw.zimlms.portals

enum class WorkspaceTab(val label: String) {
  HOME("Dashboard"),
  LEARNERS("Learners"),
  TEACHERS("Teachers"),
  PARENTS("Parents"),
  MEMBERS("School memberships"),
  CLASSES("Classes"),
  ASSIGNMENTS("Teaching assignments"),
  ATTENDANCE("Attendance"),
  CHILDREN("My learners"),
  MY_CLASSES("My classes"),
  LEARNING("Learning library"),
  PLANNING("Lesson planning"),
  CALENDAR("Calendar"),
  NOTICES("Notices"),
  FEES("Fee invoices"),
  RESOURCES("School resources"),
  ASSESSMENTS("Assessments & marks"),
  RESULTS("Assessment results"),
  COURSEWORK("Assignments"),
  SCHOOL_LIFE("My school life"),
  CAREERS("My future")
}

/**
 * Visual navigation is NOT an authorization layer; Supabase RLS enforces
 * every directory read. Never let an admin switch authentication identities
 * to display another role's private records.
 */
object WorkspaceNavigation {
  fun tabs(role: String): List<WorkspaceTab> = when (role) {
    "admin", "head" -> listOf(
      WorkspaceTab.HOME, WorkspaceTab.LEARNERS, WorkspaceTab.TEACHERS,
      WorkspaceTab.PARENTS, WorkspaceTab.MEMBERS, WorkspaceTab.CLASSES, WorkspaceTab.ASSIGNMENTS,
      WorkspaceTab.ATTENDANCE, WorkspaceTab.ASSESSMENTS, WorkspaceTab.PLANNING,
      WorkspaceTab.LEARNING, WorkspaceTab.CALENDAR,
      WorkspaceTab.NOTICES, WorkspaceTab.FEES, WorkspaceTab.RESOURCES,
      WorkspaceTab.COURSEWORK
    ).filter { role=="admin" || it!=WorkspaceTab.MEMBERS }
    "teacher" -> listOf(
      WorkspaceTab.HOME, WorkspaceTab.MY_CLASSES, WorkspaceTab.ATTENDANCE,
      WorkspaceTab.ASSESSMENTS, WorkspaceTab.PLANNING, WorkspaceTab.LEARNING, WorkspaceTab.CALENDAR, WorkspaceTab.NOTICES,
      WorkspaceTab.RESOURCES, WorkspaceTab.COURSEWORK
    )
    "student" -> listOf(
      WorkspaceTab.HOME, WorkspaceTab.MY_CLASSES, WorkspaceTab.LEARNING,
      WorkspaceTab.NOTICES, WorkspaceTab.FEES, WorkspaceTab.RESOURCES, WorkspaceTab.RESULTS,
      WorkspaceTab.COURSEWORK, WorkspaceTab.SCHOOL_LIFE, WorkspaceTab.CAREERS
    )
    "parent" -> listOf(
      WorkspaceTab.HOME, WorkspaceTab.CHILDREN, WorkspaceTab.LEARNING,
      WorkspaceTab.NOTICES, WorkspaceTab.FEES, WorkspaceTab.RESOURCES, WorkspaceTab.RESULTS, WorkspaceTab.COURSEWORK
    )
    "bursar" -> listOf(WorkspaceTab.HOME, WorkspaceTab.FEES, WorkspaceTab.NOTICES)
    "clerk" -> listOf(WorkspaceTab.HOME, WorkspaceTab.NOTICES)
    else -> listOf(WorkspaceTab.HOME)
  }

  /** Show at most five first-screen actions, keeping other authorised tools in the menu. */
  fun homeTabs(role:String):List<WorkspaceTab>{
    val requested=when(role){
      "admin"->listOf(WorkspaceTab.MEMBERS,WorkspaceTab.LEARNERS,
        WorkspaceTab.ASSESSMENTS,WorkspaceTab.NOTICES,WorkspaceTab.FEES)
      "head"->listOf(WorkspaceTab.LEARNERS,WorkspaceTab.ASSESSMENTS,
        WorkspaceTab.LEARNING,WorkspaceTab.NOTICES,WorkspaceTab.FEES)
      "teacher"->listOf(WorkspaceTab.MY_CLASSES,WorkspaceTab.PLANNING,
        WorkspaceTab.ASSESSMENTS,WorkspaceTab.NOTICES,WorkspaceTab.LEARNING)
      "student"->listOf(WorkspaceTab.LEARNING,WorkspaceTab.RESULTS,
        WorkspaceTab.NOTICES,WorkspaceTab.SCHOOL_LIFE,WorkspaceTab.CAREERS)
      "parent"->listOf(WorkspaceTab.CHILDREN,WorkspaceTab.RESULTS,
        WorkspaceTab.NOTICES,WorkspaceTab.FEES,WorkspaceTab.LEARNING)
      "bursar"->listOf(WorkspaceTab.FEES,WorkspaceTab.NOTICES)
      "clerk"->listOf(WorkspaceTab.NOTICES)
      else->emptyList()
    }
    return requested.filter{canOpen(role,it)}
  }
  fun canOpen(role:String,tab:WorkspaceTab)=tab in tabs(role)

  fun heading(role: String) = when(role) {
    "admin", "head" -> "School administration"
    "teacher" -> "Teacher portal"
    "student" -> "Learner portal"
    "parent" -> "Parent portal"
    else -> "School workspace"
  }
}
