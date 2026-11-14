package zw.zimlms.core.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class SchoolRoleVerifierTest{
 private val person=CachedProfile("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
  "School Teacher","teacher","bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")
 @Test fun exactActiveRoleIsAccepted(){
  assertEquals(SchoolRoleStatus.VERIFIED,evaluateSchoolRole(
   person,person.userId,person.schoolId,"teacher",true,false))
 }
 @Test fun roleChangesInvalidateCachedDesignation(){
  for(role in listOf("student","parent","admin","head")){
   assertEquals(SchoolRoleStatus.CHANGED,evaluateSchoolRole(
    person,person.userId,person.schoolId,role,true,false))
  }
 }
 @Test fun revokedCrossSchoolAndWrongIdentityAreDenied(){
  assertEquals(SchoolRoleStatus.DEACTIVATED,evaluateSchoolRole(
   person,person.userId,person.schoolId,"teacher",false,false))
  assertEquals(SchoolRoleStatus.DEACTIVATED,evaluateSchoolRole(
   person,person.userId,person.schoolId,"teacher",true,true))
  assertEquals(SchoolRoleStatus.DEACTIVATED,evaluateSchoolRole(
   person,person.userId,"cccccccc-cccc-4ccc-8ccc-cccccccccccc","teacher",true,false))
  assertEquals(SchoolRoleStatus.DEACTIVATED,evaluateSchoolRole(
   person,"dddddddd-dddd-4ddd-8ddd-dddddddddddd",person.schoolId,"teacher",true,false))
 }
}
