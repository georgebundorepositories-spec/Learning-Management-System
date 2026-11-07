package zw.zimlms.core.auth

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Hardware/Android Keystore-backed local credential checks, executed on an emulator. */
@RunWith(AndroidJUnit4::class)
class OfflineVaultInstrumentedTest {
  private lateinit var vault: OfflineVault

  @Before fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    vault = OfflineVault(context)
    vault.clear()
  }

  @After fun cleanup() { vault.clear() }

  @Test fun validPinUnlocksSameProfile() {
    val original = CachedProfile(
      userId = "ci-user", fullName = "Synthetic Test Teacher",
      role = "teacher", schoolId = "ci-school"
    )
    vault.enroll(original, "473821")
    val outcome = vault.unlock("473821")
    assertTrue("Stored PIN should unlock the device", outcome is OfflineLogin.Success)
    val profile = (outcome as OfflineLogin.Success).profile
    assertEquals(original.userId, profile.userId)
    assertEquals(original.schoolId, profile.schoolId)
    assertEquals(original.role, profile.role)
  }

  @Test fun invalidPinRejectedWithoutLeakingProfile() {
    val original = CachedProfile("ci-user", "Synthetic Test", "parent", "ci-school")
    vault.enroll(original, "123456")
    val outcome = vault.unlock("999999")
    assertTrue("Incorrect PIN must fail", outcome is OfflineLogin.Rejected)
  }

  @Test fun fifthInvalidPinLocksOutCorrectPin() {
    vault.enroll(CachedProfile("ci-user", "Synthetic Test", "teacher", "ci-school"), "654321")
    repeat(5) {
      assertTrue("Every invalid PIN fails", vault.unlock("111111") is OfflineLogin.Rejected)
    }
    val outcome = vault.unlock("654321")
    assertTrue("Correct PIN must not bypass lockout", outcome is OfflineLogin.Rejected)
    assertTrue((outcome as OfflineLogin.Rejected).reason.contains("15 minutes"))
  }

  @Test fun signOutClearsSavedOfflineAccess() {
    vault.enroll(CachedProfile("ci-user", "Synthetic Test", "admin", "ci-school"), "123456")
    vault.clear()
    assertTrue("Cleared vault must refuse previously saved PIN",
      vault.unlock("123456") is OfflineLogin.Rejected)
  }

  @Test fun rejectsWeakPinFormat() {
    try {
      vault.enroll(CachedProfile("ci-user", "Synthetic Test", "admin", "ci-school"), "12")
      fail("Short PIN must be rejected")
    } catch (_: IllegalArgumentException) {
      // Expected input validation.
    }
  }
}
