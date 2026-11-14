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
    assertTrue(tabs.contains(WorkspaceTab.ASSIGNMENTS))
    assertTrue(tabs.contains(WorkspaceTab.ATTENDANCE))
  }

  @Test fun teachersCannotBrowseAdministratorRegisters() {
    val tabs = WorkspaceNavigation.tabs("teacher")
    assertTrue(tabs.contains(WorkspaceTab.MY_CLASSES))
    assertTrue(tabs.contains(WorkspaceTab.LEARNING))
    assertFalse(tabs.contains(WorkspaceTab.LEARNERS))
    assertFalse(tabs.contains(WorkspaceTab.PARENTS))
    assertFalse(tabs.contains(WorkspaceTab.ASSIGNMENTS))
    assertTrue(tabs.contains(WorkspaceTab.NOTICES))
    assertTrue(tabs.contains(WorkspaceTab.RESOURCES))
    assertTrue(tabs.contains(WorkspaceTab.COURSEWORK))
    assertFalse(tabs.contains(WorkspaceTab.FEES))
    assertFalse(tabs.contains(WorkspaceTab.RESULTS))
    assertTrue(tabs.contains(WorkspaceTab.ATTENDANCE))
  }

  @Test fun learnersAndParentsCannotOpenStaffCalendarEditor() {
    for (role in listOf("student", "parent")) {
      assertFalse(WorkspaceNavigation.tabs(role).contains(WorkspaceTab.CALENDAR))
      assertFalse(WorkspaceNavigation.tabs(role).contains(WorkspaceTab.ATTENDANCE))
      assertTrue(WorkspaceNavigation.tabs(role).contains(WorkspaceTab.LEARNING))
      assertFalse(WorkspaceNavigation.tabs(role).contains(WorkspaceTab.TEACHERS))
    }
  }

  @Test fun assessmentRecordingIsStaffOnly(){
    for(role in listOf("admin","head","teacher"))
      assertTrue(WorkspaceNavigation.canOpen(role,WorkspaceTab.ASSESSMENTS))
    for(role in listOf("student","parent","bursar","clerk","unknown"))
      assertFalse(WorkspaceNavigation.canOpen(role,WorkspaceTab.ASSESSMENTS))
  }

  @Test fun lessonPlanningIsStaffOnly() {
    for(role in listOf("admin","head","teacher"))
      assertTrue(WorkspaceNavigation.canOpen(role,WorkspaceTab.PLANNING))
    for(role in listOf("student","parent","bursar","unknown"))
      assertFalse(WorkspaceNavigation.canOpen(role,WorkspaceTab.PLANNING))
  }

  @Test fun onlyAdministratorCanManageMemberRelationships(){
    assertTrue(WorkspaceNavigation.canOpen("admin",WorkspaceTab.MEMBERS))
    for(role in listOf("head","teacher","student","parent","bursar","unknown")){
      assertFalse(WorkspaceNavigation.canOpen(role,WorkspaceTab.MEMBERS))
    }
  }

  @Test fun roleFirstHomeHasFiveOrFewerAuthorisedChoices(){
    for(role in listOf("admin","head","teacher","student","parent","bursar","clerk","unknown")){
      val home=WorkspaceNavigation.homeTabs(role)
      assertTrue(home.size<=5)
      assertTrue(home.all{WorkspaceNavigation.canOpen(role,it)})
    }
    assertTrue(WorkspaceNavigation.homeTabs("student").contains(WorkspaceTab.RESULTS))
    assertTrue(WorkspaceNavigation.homeTabs("student").contains(WorkspaceTab.CAREERS))
    assertTrue(WorkspaceNavigation.homeTabs("student").contains(WorkspaceTab.SCHOOL_LIFE))
    assertFalse(WorkspaceNavigation.canOpen("parent",WorkspaceTab.CAREERS))
  }
  @Test fun unknownRoleCannotSeeOtherUsersRecords() {
    assertEquals(listOf(WorkspaceTab.HOME), WorkspaceNavigation.tabs("unrecognised"))
  }

  @Test fun parentOnlyGetsLinkedLearnerPortal() {
    val tabs = WorkspaceNavigation.tabs("parent")
    assertTrue(tabs.contains(WorkspaceTab.CHILDREN))
    assertTrue(tabs.contains(WorkspaceTab.FEES))
    assertTrue(tabs.contains(WorkspaceTab.NOTICES))
    assertTrue(tabs.contains(WorkspaceTab.RESOURCES))
    assertTrue(tabs.contains(WorkspaceTab.RESULTS))
    assertTrue(tabs.contains(WorkspaceTab.COURSEWORK))
    assertFalse(tabs.contains(WorkspaceTab.LEARNERS))
  }
}
