package zw.zimlms.portals

import java.util.Locale

/** Narrow local navigation. It can only filter authorised role tabs, not grant
 * extra menu items or bypass backend school-level permissions.
 */
enum class WorkspaceGroup(val label:String){
 ALL("All"), PEOPLE("People"), TEACHING("Teaching"), LEARNING("Learning"),
 COMMUNICATION("Communication"), FINANCE("Finance")
}

fun workspaceGroup(tab:WorkspaceTab):WorkspaceGroup=when(tab){
 WorkspaceTab.LEARNERS,WorkspaceTab.TEACHERS,WorkspaceTab.PARENTS,
 WorkspaceTab.MEMBERS,WorkspaceTab.CHILDREN->WorkspaceGroup.PEOPLE
 WorkspaceTab.ASSIGNMENTS,WorkspaceTab.ATTENDANCE,WorkspaceTab.ASSESSMENTS,
 WorkspaceTab.PLANNING,WorkspaceTab.COURSEWORK,WorkspaceTab.MY_CLASSES,
 WorkspaceTab.CLASSES->WorkspaceGroup.TEACHING
 WorkspaceTab.LEARNING,WorkspaceTab.RESULTS->WorkspaceGroup.LEARNING
 WorkspaceTab.CALENDAR,WorkspaceTab.NOTICES,WorkspaceTab.RESOURCES->WorkspaceGroup.COMMUNICATION
 WorkspaceTab.FEES->WorkspaceGroup.FINANCE
 WorkspaceTab.HOME->WorkspaceGroup.ALL
}

fun workspaceModuleDescription(tab:WorkspaceTab):String=when(tab){
 WorkspaceTab.LEARNERS -> "Learner register"
 WorkspaceTab.TEACHERS -> "Staff directory"
 WorkspaceTab.PARENTS -> "Family register"
 WorkspaceTab.MEMBERS -> "Enrolments and guardian links"
 WorkspaceTab.CLASSES -> "Class and subjects"
 WorkspaceTab.ASSIGNMENTS -> "Manage teaching links"
 WorkspaceTab.ATTENDANCE -> "Mark today's register"
 WorkspaceTab.ASSESSMENTS -> "Prepare tests and record marks"
 WorkspaceTab.MY_CLASSES -> "Your assigned groups"
 WorkspaceTab.CHILDREN -> "Linked learners"
 WorkspaceTab.LEARNING -> "Offline lessons and practice"
 WorkspaceTab.PLANNING -> "Staff lesson preparation"
 WorkspaceTab.CALENDAR -> "School events"
 WorkspaceTab.NOTICES -> "School announcements"
 WorkspaceTab.FEES -> "Private school charges"
 WorkspaceTab.RESOURCES -> "Teacher-shared materials"
 WorkspaceTab.RESULTS -> "Private assessment marks"
 WorkspaceTab.COURSEWORK -> "Class tasks and submissions"
 WorkspaceTab.HOME -> "Overview"
}
fun filterWorkspaceModules(
 role:String,searchText:String="",group:WorkspaceGroup=WorkspaceGroup.ALL
):List<WorkspaceTab>{
 val terms=searchText.trim().lowercase(Locale.ROOT)
  .split(Regex("\\s+")).filter{it.isNotEmpty()}
 return WorkspaceNavigation.tabs(role).filter{tab->
  tab!=WorkspaceTab.HOME &&
   (group==WorkspaceGroup.ALL||workspaceGroup(tab)==group) &&
   terms.all{term->
    val haystack=(tab.label+" "+workspaceModuleDescription(tab)+" "+
      workspaceGroup(tab).label).lowercase(Locale.ROOT)
    haystack.contains(term)
   }
 }
}

fun filterPortalPeople(people:List<PortalPerson>,role:String,query:String):List<PortalPerson>{
 val words=query.trim().lowercase(Locale.ROOT).split(Regex("\\s+"))
  .filter{it.isNotEmpty()}
 return people.asSequence().filter{it.role==role}.filter{person->
  val name=person.name.lowercase(Locale.ROOT)
  words.all{name.contains(it)}
 }.toList()
}

/** 1 column on narrow handsets, 2 on regular tablets, 3 on wide landscape. */
fun workspaceColumnCount(availableWidthDp:Float):Int=when{
 availableWidthDp>=900f->3
 availableWidthDp>=600f->2
 else->1
}
