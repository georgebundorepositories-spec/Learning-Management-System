package zw.zimlms.learning

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LessonProgressVaultInstrumentedTest {
  private val context=ApplicationProvider.getApplicationContext<Context>()
  private val vault=LessonProgressVault(context)
  private val user="33333333-3333-4333-8333-333333333333"
  private val otherUser="44444444-4444-4444-8444-444444444444"
  private val school="22222222-2222-4222-8222-222222222222"
  private val otherSchool="55555555-5555-4555-8555-555555555555"
  private val prefs by lazy {context.getSharedPreferences(
    "zimschool_lesson_progress_v1",Context.MODE_PRIVATE)}

  @Before fun prepare(){prefs.edit().clear().commit()}
  @After fun cleanup(){prefs.edit().clear().commit()}

  @Test fun completionPersistsAcrossVaultInstancesWithoutPlaintext() {
    val id=StarterLessons.lessons[0].id
    assertTrue(vault.completed(user,school).isEmpty())
    assertEquals(setOf(id),vault.markCompleted(user,school,id))
    assertEquals(setOf(id),LessonProgressVault(context).completed(user,school))
    val stored=prefs.all.values.joinToString()
    assertFalse(stored.contains(id))
    assertFalse(stored.contains(user))
    assertFalse(stored.contains(school))
  }

  @Test fun schoolsAndUsersNeverShareLearningProgress() {
    val id=StarterLessons.lessons[0].id
    vault.markCompleted(user,school,id)
    assertTrue(vault.completed(otherUser,school).isEmpty())
    assertTrue(vault.completed(user,otherSchool).isEmpty())
  }

  @Test fun duplicateCorrectAnswersAreIdempotent() {
    val id=StarterLessons.lessons[0].id
    vault.markCompleted(user,school,id)
    assertEquals(setOf(id),vault.markCompleted(user,school,id))
  }

  @Test fun invalidLessonIdsAreRejected() {
    try {
      vault.markCompleted(user,school,"not-a-real-lesson")
      throw AssertionError("Unknown learning content must not be saved")
    }catch(expected:IllegalArgumentException) {
      assertTrue(expected.message!!.contains("Unknown"))
    }
  }

  @Test fun newlyAddedCurriculumLessonsPersistEncrypted() {
    val id="grade-6-maths-ratio-scale"
    assertTrue(vault.markCompleted(user,school,id).contains(id))
    assertTrue(LessonProgressVault(context).completed(user,school).contains(id))
    val encrypted=prefs.all.values.joinToString()
    assertFalse(encrypted.contains(id))
    assertFalse(encrypted.contains(user))
  }

  @Test fun cloudMergeCombinesBothDevicesWithoutDuplicatingOfflineProgress() {
    val starter=StarterLessons.lessons[0].id
    val remote="grade-7-science-respiratory-system"
    vault.markCompleted(user,school,starter)
    val merged=vault.mergeCompleted(user,school,setOf(remote,starter))
    assertEquals(setOf(starter,remote),merged)
    assertEquals(merged,vault.mergeCompleted(user,school,setOf(remote)))
    assertEquals(merged,LessonProgressVault(context).completed(user,school))
    assertTrue(vault.completed(otherUser,school).isEmpty())
    assertTrue(vault.completed(user,otherSchool).isEmpty())
  }

  @Test fun encryptedProgressTamperingFailsClosed() {
    val id=StarterLessons.lessons[0].id
    vault.markCompleted(user,school,id)
    val slot=prefs.all.keys.single()
    prefs.edit().putString(slot,"garbled-ciphertext").commit()
    try {
      vault.completed(user,school)
      throw AssertionError("Tampered progress must not be treated as empty")
    }catch(expected:IllegalStateException) {
      assertTrue(expected.message!!.contains("unavailable or damaged"))
    }
  }
}
