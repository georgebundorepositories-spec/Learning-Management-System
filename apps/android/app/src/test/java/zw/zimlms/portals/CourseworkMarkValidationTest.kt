package zw.zimlms.portals

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseworkMarkValidationTest {
  @Test fun acceptsZeroAndExactTwoDecimalGrades() {
    assertTrue(validCourseworkMark("0","100"))
    assertTrue(validCourseworkMark("18.75","20.00"))
    assertTrue(validCourseworkMark("20.00","20.00"))
  }

  @Test fun rejectsScoresAboveAssignmentMaximum() {
    assertFalse(validCourseworkMark("20.01","20.00"))
    assertFalse(validCourseworkMark("100.01","100"))
  }

  @Test fun rejectsNegativeNonNumericAndTooPreciseScores() {
    for (mark in listOf("-1", "3.141", "NaN", "Infinity", "1e3", "", "01", "1,5")) {
      assertFalse(validCourseworkMark(mark,"100"))
    }
  }

  @Test fun rejectsInvalidMaximum() {
    for (maximum in listOf("0","-5","10001","5.999","bad")) {
      assertFalse(validCourseworkMark("1",maximum))
    }
  }
}
