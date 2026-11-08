package zw.zimlms.portals

import org.junit.Assert.*
import org.junit.Test

class WorkspaceNavigationTest {
  @Test fun administratorsSeeSchoolRegistersAndLearning() {
    val tabs = WorkspaceNavigation.tabs("admin")
    assertTrue(tabs.contains(WorkspaceTab.LEARNERS))
    assertTrue(tabs.contains(WorkspaceTab.TEACHERS))
    assertTrue(tabs.contains(WorkspaceTab.PARENTS))
    assertTrue(tabs.contains(WorkspaceTab.LEARNING))
    assertTrue(tabs.contains(WorkspaceTab.CALENDAR))
  }

  @Test fun teachersCannotBrowseAdministratorRegisters() {
    val tabs = WorkspaceNavigation.tabs("teacher")
    assertTrue(tabs.contains(WorkspaceTab.MY_CLASSES))
    assertTrue(tabs.contains(WorkspaceTab.LEARNING))
    assertFalse(tabs.contains(WorkspaceTab.LEARNERS))
    assertFalse(tabs.contains(WorkspaceTab.PARENTS))
  }

  @Test fun learnersAndParentsCannotOpenStaffCalendarEditor() {
    for (role in listOf("student", "parent")) {
      assertFalse(WorkspaceNavigation.tabs(role).contains(WorkspaceTab.CALENDAR))
      assertTrue(WorkspaceNavigation.tabs(role).contains(WorkspaceTab.LEARNING))
      assertFalse(WorkspaceNavigation.tabs(role).contains(WorkspaceTab.TEACHERS))
    }
  }

  @Test fun unknownRoleCannotSeeOtherUsersRecords() {
    assertEquals(listOf(WorkspaceTab.HOME), WorkspaceNavigation.tabs("unrecognised"))
  }

  @Test fun parentOnlyGetsLinkedLearnerPortal() {
    val tabs = WorkspaceNavigation.tabs("parent")
    assertTrue(tabs.contains(WorkspaceTab.CHILDREN))
    assertFalse(tabs.contains(WorkspaceTab.LEARNERS))
  }
}
