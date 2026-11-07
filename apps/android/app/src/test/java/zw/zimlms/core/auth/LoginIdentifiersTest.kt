package zw.zimlms.core.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LoginIdentifiersTest {
  @Test fun normalizesLocalNumber() {
    assertEquals("+263771234567", LoginIdentifiers.parse("077 123 4567").value)
  }
  @Test fun keepsE164Number() {
    assertEquals("+263712345678", LoginIdentifiers.parse("+263712345678").value)
  }
  @Test fun parsesEmail() {
    assertEquals("email", LoginIdentifiers.parse("parent@example.org").key)
  }
  @Test fun rejectsInvalidNumber() {
    assertThrows(IllegalArgumentException::class.java) { LoginIdentifiers.parse("123") }
  }
}
