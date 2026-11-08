package zw.zimlms

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    compose.onNodeWithText("Learning library").assertExists()
    compose.onNodeWithText("12 ready-to-read lessons").assertExists()
    compose.onNodeWithText("Search lessons, subjects or topics").assertExists()
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
    compose.onNodeWithText("Sign out of this device").assertExists().performClick()
    compose.runOnIdle { assertTrue("Signout callback must fire",requested.get()) }
  }

  @Test fun learnerCannotSeeAdministrativeDirectoryMenus() {
    compose.setContent {
      ZimSchoolTheme { SchoolWorkspace(profile("student"),onSignOut={}) }
    }
    compose.onNodeWithText("Library").assertExists()
    compose.onNodeWithText("Teachers").assertDoesNotExist()
    compose.onNodeWithText("Parents").assertDoesNotExist()
    compose.onNodeWithText("Learners").assertDoesNotExist()
  }
}
