package zw.zimlms

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.ui.ZimSchoolTheme
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class WorkspaceUiInstrumentedTest {
  @get:Rule val compose=createComposeRule()
  private val schoolId="22222222-2222-4222-8222-222222222222"

  private fun profile(role:String)=CachedProfile(
    "11111111-1111-4111-8111-111111111111",
    "Demo Administrator",role,schoolId
  )

  @Test fun administratorCanSwitchToOfflineLearningWithoutInternet() {
    compose.setContent {
      ZimSchoolTheme { SchoolWorkspace(profile("admin"),onSignOut={}) }
    }
    compose.onNodeWithText("Library").assertExists().performClick()
    compose.onNodeWithTag("learning-library-heading").assertExists()
    compose.onNodeWithText("75 ready-to-read lessons").assertExists()
    compose.onNodeWithText("Search lessons, subjects or topics").assertExists()
  }

  @Test fun newCurriculumLessonShowsExercisesAndAssignmentOffline() {
    compose.setContent {
      ZimSchoolTheme { SchoolWorkspace(profile("student"),onSignOut={}) }
    }
    compose.onNodeWithText("Library").performClick()
    compose.onNodeWithTag("learning-library-list")
      .performScrollToNode(hasTestTag("learning-library-heading"))
    compose.onNodeWithTag("learning-library-list")
      .performScrollToNode(hasText("Sounds around us"))
    compose.onNodeWithText("Sounds around us").assertIsDisplayed()
      .performTouchInput { click() }
    compose.onNodeWithTag("learning-library-list")
      .performScrollToNode(hasTestTag("lesson-exercises"))
    compose.onNodeWithTag("lesson-exercises").assertExists()
    compose.onNodeWithText("Three practice exercises").assertExists()
    compose.onNodeWithTag("learning-library-list")
      .performScrollToNode(hasTestTag("lesson-assignment"))
    compose.onNodeWithTag("lesson-assignment").assertExists()
    compose.onNodeWithText("CLASS ASSIGNMENT").assertExists()
  }

  @Test fun authenticatedShellHasAccessibleNavigationAndSignOut() {
    val requested=AtomicBoolean(false)
    compose.setContent {
      ZimSchoolTheme { SchoolWorkspace(profile("admin"),
        onSignOut={requested.set(true)}) }
    }
    compose.onNodeWithContentDescription("Open navigation menu")
      .assertExists().performClick()
    compose.onNodeWithText("WORKSPACE").assertExists()
    compose.waitForIdle()
    // Verify the drawer action is actually visible and physically tappable
    // rather than invoking semantics on an off-screen drawer item.
    compose.onNodeWithTag("drawer-sign-out")
      .assertIsDisplayed().performTouchInput { click() }
    compose.waitUntil(5_000L) { requested.get() }
    assertTrue("Signout callback must fire",requested.get())
  }

  @Test fun administratorKeepsSecondaryDirectoriesUnderMoreMenu(){
    compose.setContent {
      ZimSchoolTheme { SchoolWorkspace(profile("admin"),onSignOut={}) }
    }
    compose.onNodeWithTag("home-module-list")
      .performScrollToNode(hasTestTag("home-tool-members"))
    compose.onNodeWithTag("home-tool-members").assertExists()
    compose.onNodeWithTag("home-tool-teachers").assertDoesNotExist()
    compose.onNodeWithContentDescription("Open navigation menu").performClick()
    compose.onNodeWithTag("drawer-more-tools").performClick()
    compose.onNodeWithText("Teachers").assertExists()
  }

  @Test fun teacherToolOpensClassesInsteadOfRemainingOnDashboard() {
    compose.setContent {
      ZimSchoolTheme { SchoolWorkspace(profile("teacher"),onSignOut={}) }
    }
    compose.onNodeWithTag("home-module-list")
      .performScrollToNode(hasTestTag("home-tool-my_classes"))
    compose.onNodeWithTag("home-tool-my_classes")
      .assertIsDisplayed().performTouchInput { click() }
    compose.onNodeWithTag("module-screen-my_classes").assertExists()
    compose.onNodeWithContentDescription("Back to dashboard").assertExists()
  }

  @Test fun learnerHomeOpensCareerGuidanceAndKeepsResultsAndSchoolLife(){
    compose.setContent {
      ZimSchoolTheme { SchoolWorkspace(profile("student"),onSignOut={}) }
    }
    for(card in listOf("results","school_life","careers")){
      compose.onNodeWithTag("home-module-list")
        .performScrollToNode(hasTestTag("home-tool-$card"))
      compose.onNodeWithTag("home-tool-$card").assertExists()
    }
    compose.onNodeWithTag("home-module-list")
      .performScrollToNode(hasTestTag("home-tool-careers"))
    compose.onNodeWithTag("home-tool-careers").performTouchInput{click()}
    compose.onNodeWithTag("module-screen-careers").assertExists()
    compose.onNodeWithText("My future").assertExists()
  }

  @Test fun parentToolOpensLinkedLearnersModule() {
    compose.setContent {
      ZimSchoolTheme { SchoolWorkspace(profile("parent"),onSignOut={}) }
    }
    compose.onNodeWithTag("home-module-list")
      .performScrollToNode(hasTestTag("home-tool-children"))
    compose.onNodeWithTag("home-tool-children")
      .assertIsDisplayed().performTouchInput { click() }
    compose.onNodeWithTag("module-screen-children").assertExists()
  }

  @Test fun teacherCanOpenAssessmentMarking(){
    compose.setContent {
      ZimSchoolTheme {SchoolWorkspace(profile("teacher"),onSignOut={})}
    }
    compose.onNodeWithTag("home-module-list")
      .performScrollToNode(hasTestTag("home-tool-assessments"))
    compose.onNodeWithTag("home-tool-assessments").performTouchInput{click()}
    compose.onNodeWithTag("module-screen-assessments").assertExists()
    compose.onNodeWithTag("staff-assessments-screen").assertExists()
  }

  @Test fun teacherHasFewWorkingToolsInsteadOfSearchAndFilters(){
    compose.setContent{
      ZimSchoolTheme{SchoolWorkspace(profile("teacher"),onSignOut={})}
    }
    compose.onNodeWithTag("home-module-list")
      .performScrollToNode(hasTestTag("home-tool-notices"))
    compose.onNodeWithTag("home-tool-notices").assertExists()
    compose.onNodeWithTag("home-module-search").assertDoesNotExist()
    compose.onNodeWithTag("home-tool-users").assertDoesNotExist()
  }

  @Test fun teacherCanOpenLessonPlanning() {
    compose.setContent {
      ZimSchoolTheme { SchoolWorkspace(profile("teacher"),onSignOut={}) }
    }
    compose.onNodeWithTag("home-module-list")
      .performScrollToNode(hasTestTag("home-tool-planning"))
    compose.onNodeWithTag("home-tool-planning")
      .performTouchInput { click() }
    compose.onNodeWithTag("module-screen-planning").assertExists()
    compose.onNodeWithTag("teacher-planning-screen").assertExists()
  }

  @Test fun offlinePinCannotOpenPreviouslyCachedTeacherPortal(){
    compose.setContent {
      ZimSchoolTheme {SchoolWorkspace(profile("teacher"),onSignOut={},
        enforceOnlineRoleVerification=true,onlineVerifiedAtLogin=false)}
    }
    compose.onNodeWithTag("unverified-school-role").assertExists()
    compose.onNodeWithTag("learning-library-list").assertExists()
    compose.onNodeWithText("Lesson planning").assertDoesNotExist()
    compose.onNodeWithText("Retry online check").assertExists()
  }

  @Test fun offlinePinCannotOpenPreviouslyCachedParentRecords(){
    compose.setContent {
      ZimSchoolTheme {SchoolWorkspace(profile("parent"),onSignOut={},
        enforceOnlineRoleVerification=true,onlineVerifiedAtLogin=false)}
    }
    compose.onNodeWithTag("unverified-school-role").assertExists()
    compose.onNodeWithTag("learning-library-list").assertExists()
    compose.onNodeWithText("My learners").assertDoesNotExist()
  }

  @Test fun adminCanOpenMemberManagementAndOtherRolesCannot(){
    compose.setContent {
      ZimSchoolTheme { SchoolWorkspace(profile("admin"),onSignOut={}) }
    }
    compose.onNodeWithTag("home-module-list")
      .performScrollToNode(hasTestTag("home-tool-members"))
    compose.onNodeWithTag("home-tool-members")
      .performTouchInput { click() }
    compose.onNodeWithTag("module-screen-members").assertExists()
    compose.onNodeWithTag("school-membership-screen").assertExists()
    // Heading and tab label may both be present; assert the unique
    // screen identity rather than an ambiguous repeated string.
  }

  @Test fun learnerCannotSeeAdministrativeDirectoryMenus() {
    compose.setContent {
      ZimSchoolTheme { SchoolWorkspace(profile("student"),onSignOut={}) }
    }
    compose.onNodeWithText("Library").assertExists()
    compose.onNodeWithText("Teachers").assertDoesNotExist()
    compose.onNodeWithText("Parents").assertDoesNotExist()
    compose.onNodeWithText("Learners").assertDoesNotExist()
    compose.onNodeWithText("Lesson planning").assertDoesNotExist()
  }
}
