package zw.zimlms.portals

import org.junit.Assert.*
import org.junit.Test
import zw.zimlms.core.auth.CachedProfile

class SchoolMembershipValidationTest {
 private val school="bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
 private val admin=CachedProfile("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
  "Administrator","admin",school)
 private val learner=PortalPerson("cccccccc-cccc-4ccc-8ccc-cccccccccccc","Learner","student",true)
 private val parent=PortalPerson("dddddddd-dddd-4ddd-8ddd-dddddddddddd","Parent","parent",true)
 private val teacher=PortalPerson("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee","Teacher","teacher",false)
 @Test fun administratorMustHaveValidIdentity() {
  assertTrue(validMembershipActor(admin))
  assertFalse(validMembershipActor(admin.copy(role="head")))
  assertFalse(validMembershipActor(admin.copy(role="teacher")))
  assertFalse(validMembershipActor(admin.copy(role="student")))
  assertFalse(validMembershipActor(admin.copy(schoolId="wrong-school")))
 }
 @Test fun roleAndActivationAreNotInterchangeable() {
  val all=listOf(learner,parent,teacher)
  assertTrue(eligibleMember(all,learner.id,"student"))
  assertTrue(eligibleMember(all,parent.id,"parent"))
  assertFalse(eligibleMember(all,parent.id,"teacher"))
  assertFalse(eligibleMember(all,teacher.id,"teacher"))
  assertFalse(eligibleMember(all,"unrelated-id","student"))
 }
 @Test fun malformedIdentifiersAndForgedRelationshipsFailValidation() {
  assertTrue(validMembershipPair(learner.id,parent.id))
  assertFalse(validMembershipPair(learner.id,"../student_profiles"))
  assertFalse(validMembershipPair("",""))
  assertFalse(membershipUuid.matches("deadbeef"))
 }
}
