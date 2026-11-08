package zw.zimlms.portals

enum class WorkspaceTab(val label: String) {
  HOME("Dashboard"),
  LEARNERS("Learners"),
  TEACHERS("Teachers"),
  PARENTS("Parents"),
  CLASSES("Classes"),
  ASSIGNMENTS("Teaching assignments"),
  CHILDREN("My learners"),
  MY_CLASSES("My classes"),
  LEARNING("Learning library"),
  CALENDAR("Calendar"),
  ATTENDANCE("Attendance")
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
      WorkspaceTab.PARENTS, WorkspaceTab.CLASSES, WorkspaceTab.ASSIGNMENTS,
      WorkspaceTab.ATTENDANCE, WorkspaceTab.LEARNING, WorkspaceTab.CALENDAR
    )
    "teacher" -> listOf(
      WorkspaceTab.HOME, WorkspaceTab.MY_CLASSES, WorkspaceTab.ATTENDANCE,
      WorkspaceTab.LEARNING, WorkspaceTab.CALENDAR
    )
    "student" -> listOf(
      WorkspaceTab.HOME, WorkspaceTab.MY_CLASSES, WorkspaceTab.LEARNING
    )
    "parent" -> listOf(
      WorkspaceTab.HOME, WorkspaceTab.CHILDREN, WorkspaceTab.LEARNING
    )
    "bursar", "clerk" -> listOf(WorkspaceTab.HOME, WorkspaceTab.LEARNING)
    else -> listOf(WorkspaceTab.HOME)
  }

  fun heading(role: String) = when(role) {
    "admin", "head" -> "School administration"
    "teacher" -> "Teacher portal"
    "student" -> "Learner portal"
    "parent" -> "Parent portal"
    else -> "School workspace"
  }
}
