package zw.zimlms.portals

import org.junit.Assert.*
import org.junit.Test

class WorkspaceDiscoveryTest{
 @Test fun searchNeverAddsHiddenRoleModules(){
  for(role in listOf("admin","head","teacher","student","parent","bursar","clerk","unverified")){
   val permitted=WorkspaceNavigation.tabs(role).toSet()
   for(group in WorkspaceGroup.entries){
    for(query in listOf("","learner","class","maths","guardian","results")){
     val shown=filterWorkspaceModules(role,query,group)
     assertTrue(shown.all{it in permitted})
     assertFalse(shown.contains(WorkspaceTab.HOME))
    }
   }
  }
  assertFalse(filterWorkspaceModules("parent","administrator").contains(WorkspaceTab.MEMBERS))
 }
 @Test fun modulesFilterByTitleDescriptionsAndGroup(){
  assertEquals(listOf(WorkspaceTab.ASSESSMENTS),
   filterWorkspaceModules("teacher","record marks"))
  assertEquals(listOf(WorkspaceTab.ATTENDANCE),
   filterWorkspaceModules("teacher","register",WorkspaceGroup.TEACHING))
  assertTrue(filterWorkspaceModules("student","private",WorkspaceGroup.FINANCE)
   .contains(WorkspaceTab.FEES))
  assertTrue(filterWorkspaceModules("parent","maths").isEmpty())
  assertTrue(filterWorkspaceModules("admin","  SCHOOL  ").isNotEmpty())
 }
 @Test fun adaptiveColumnsUseAvailableDpNotPhysicalPixels(){
  assertEquals(1,workspaceColumnCount(320f))
  assertEquals(1,workspaceColumnCount(599f))
  assertEquals(2,workspaceColumnCount(600f))
  assertEquals(2,workspaceColumnCount(899f))
  assertEquals(3,workspaceColumnCount(900f))
 }
 @Test fun staffDirectorySearchHandlesManyNamesAndNeverChangesOriginalList(){
  val role="student"
  val students=listOf(
   PortalPerson("1","Tariro Chuma",role,true),
   PortalPerson("2","Farai Moyo",role,true),
   PortalPerson("3","Tanaka Chuma",role,false),
   PortalPerson("4","Tariro Chuma","teacher",true)
  )
  assertEquals(3,filterPortalPeople(students,role,"").size)
  assertEquals(1,filterPortalPeople(students,role,"TaRIro CHUMA").size)
  assertEquals(2,filterPortalPeople(students,role,"chu").size)
  assertTrue(filterPortalPeople(students,role,"nonexistent").isEmpty())
  assertEquals(4,students.size)
 }
}
