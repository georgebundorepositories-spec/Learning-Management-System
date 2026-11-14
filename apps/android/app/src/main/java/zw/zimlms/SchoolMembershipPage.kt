package zw.zimlms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.SessionBroker
import zw.zimlms.portals.GuardianMembershipLink
import zw.zimlms.portals.PortalPerson
import zw.zimlms.portals.SchoolDirectory
import zw.zimlms.portals.SchoolMembershipRepository
import zw.zimlms.portals.SchoolMembershipSnapshot
import zw.zimlms.ui.AppPanel
import zw.zimlms.ui.Eyebrow
import zw.zimlms.ui.StateNotice

/** Android counterpart to the web membership manager.
 * Every write is online-only, school-scoped and requires an active admin JWT.
 * No personal directory or relationship data is written to plaintext disk.
 */
@Composable
fun SchoolMembershipPage(
 profile:CachedProfile,directory:SchoolDirectory?,modifier:Modifier=Modifier,
 onSaved:()->Unit
){
 if(profile.role!="admin"){
  StateNotice("Administrator access required",
   "Only active school administrators can change enrolments or family relationships.")
  return
 }
 val context=LocalContext.current.applicationContext
 val scope=rememberCoroutineScope()
 val repo=remember(context,profile.userId){
  runCatching{
   SchoolMembershipRepository(BuildConfig.SUPABASE_URL,
    BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    SessionBroker(context,BuildConfig.SUPABASE_URL,
     BuildConfig.SUPABASE_PUBLISHABLE_KEY))
  }.getOrNull()
 }
 val students=directory?.people?.filter{it.role=="student"&&it.active}.orEmpty()
 val parents=directory?.people?.filter{it.role=="parent"&&it.active}.orEmpty()
 val teachers=directory?.people?.filter{it.role=="teacher"&&it.active}.orEmpty()
 val classes=directory?.classes.orEmpty()
 var snapshot by remember(profile.userId,profile.schoolId){
  mutableStateOf<SchoolMembershipSnapshot?>(null)
 }
 var refreshing by remember(profile.userId){mutableStateOf(false)}
 var saving by remember(profile.userId){mutableStateOf(false)}
 var reload by remember(profile.userId){mutableIntStateOf(0)}
 var notice by remember(profile.userId){mutableStateOf<String?>(null)}
 var learnerChoice by remember(directory){mutableStateOf(students.firstOrNull()?.id.orEmpty())}
 var classChoice by remember(directory){mutableStateOf("")}
 var classForTeacher by remember(directory){mutableStateOf(classes.firstOrNull()?.id.orEmpty())}
 var teacherChoice by remember(directory){
  mutableStateOf(classes.firstOrNull()?.teacherId.orEmpty())
 }
 var linkStudent by remember(directory){mutableStateOf(students.firstOrNull()?.id.orEmpty())}
 var linkParent by remember(directory){mutableStateOf(parents.firstOrNull()?.id.orEmpty())}
 var verifiedRelationship by remember(profile.userId){mutableStateOf(false)}
 var pendingUnlink by remember(profile.userId){
  mutableStateOf<GuardianMembershipLink?>(null)
 }

 LaunchedEffect(profile.userId,profile.schoolId,directory,reload){
  if(directory==null||repo==null){
   snapshot=null
   notice="Connect to your verified school account to manage member relationships."
  }else{
   refreshing=true
   runCatching{repo.load(profile,directory)}.fold(
    onSuccess={
      snapshot=it
      classChoice=it.enrolledClasses[learnerChoice].orEmpty()
      notice=null
    },
    onFailure={
     snapshot=null
     notice="Unable to load member relationships. Reconnect and refresh."
    }
   )
   refreshing=false
  }
 }

 fun save(change:suspend()->Unit,success:String){
  if(saving||repo==null||directory==null)return
  saving=true
  notice=null
  scope.launch{
   val result=runCatching{change()}
   if(result.isSuccess){
    notice=success
    val current=runCatching{repo.load(profile,directory)}
    current.fold(onSuccess={snapshot=it},onFailure={
     snapshot=null
     notice=success+" Refresh failed; reconnect to see the latest membership records."
    })
    onSaved()
   }else{
    notice="No change was confirmed. Check your school permissions, member status and connection."
   }
   saving=false
  }
 }
 if(pendingUnlink!=null){
  val link=pendingUnlink!!
  AlertDialog(
   onDismissRequest={pendingUnlink=null},
   title={Text("Revoke guardian access?")},
   text={Text("This will immediately remove the parent's access to the linked learner's private records. Only revoke after confirming the relationship should end.")},
   confirmButton={
    TextButton(onClick={
     pendingUnlink=null
     if(repo!=null)save({repo.revoke(profile,link)},"Guardian relationship revoked.")
    },enabled=!saving){Text("Revoke access")}
   },
   dismissButton={TextButton(onClick={pendingUnlink=null}){Text("Cancel")}}
  )
 }

 val canEdit=directory!=null&&repo!=null&&snapshot!=null&&!saving&&!refreshing
 LazyColumn(modifier=modifier.fillMaxSize().testTag("school-membership-screen"),
  contentPadding=PaddingValues(horizontal=18.dp,vertical=20.dp),
  verticalArrangement=Arrangement.spacedBy(13.dp)){
  item(key="heading"){
   Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
    Eyebrow("AUTHORISED SCHOOL ADMINISTRATION")
    Text("School memberships",style=MaterialTheme.typography.headlineMedium)
    Text("Assign learner classes and class teachers, and manage verified parent–learner relationships.")
    StateNotice("Private online changes",
     "Member data stays on the school server. A change takes effect only after server confirmation.")
    OutlinedButton(onClick={reload++},enabled=!saving&&!refreshing&&repo!=null,
     modifier=Modifier.fillMaxWidth()){
     Text(if(refreshing)"Loading memberships…" else "Refresh school memberships")
    }
   }
  }
  notice?.let{message->item(key="notice"){StateNotice("School status",message)}}
  if(snapshot?.partial==true)item(key="partial"){
   StateNotice("Partial school directory",
    "Showing up to 500 records. Use the web administrator portal for larger schools.")
  }
  if(directory==null||repo==null||snapshot==null){
   item(key="offline"){StateNotice("Online administrator session required",
    "Connect and refresh the school directory to manage memberships. Existing relationships cannot be edited offline.")}
  }else{
   item(key="enrol"){
    AppPanel(Modifier.fillMaxWidth()){
     Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
      Text("1. Learner enrolment",style=MaterialTheme.typography.titleLarge)
      MembershipChoice("Active learner",learnerChoice,students.map{it.id to it.name}){
       learnerChoice=it
       classChoice=snapshot?.enrolledClasses?.get(it).orEmpty()
      }
      MembershipChoice("Assign to class",classChoice,
       listOf("" to "No class")+
        classes.map{it.id to it.name}){classChoice=it}
      val existing=snapshot?.enrolledClasses?.get(learnerChoice).orEmpty()
      Text("Current: "+(classes.firstOrNull{it.id==existing}?.name?:"No class"),
       style=MaterialTheme.typography.bodySmall)
      OutlinedButton(
       onClick={if(repo!=null)save(
        {repo.enrol(profile,learnerChoice,classChoice.ifBlank{null})},
        "Learner enrolment updated on the school server."
       )else {}},
       enabled=canEdit&&learnerChoice.isNotBlank()&&classChoice!=existing,
       modifier=Modifier.fillMaxWidth().testTag("admin-save-enrolment")
      ){Text("Save learner class")}
      if(students.isEmpty())Text("No active learners are available.")
     }
    }
   }
   item(key="homeroom"){
    AppPanel(Modifier.fillMaxWidth()){
     Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
      Text("2. Homeroom teacher",style=MaterialTheme.typography.titleLarge)
      MembershipChoice("Class",classForTeacher,
       classes.map{it.id to it.name}){chosen->
        classForTeacher=chosen
        teacherChoice=classes.firstOrNull{it.id==chosen}?.teacherId.orEmpty()
       }
      MembershipChoice("Active teacher",teacherChoice,
       listOf("" to "No homeroom teacher")+
        teachers.map{it.id to it.name}){teacherChoice=it}
      val existing=classes.firstOrNull{it.id==classForTeacher}?.teacherId.orEmpty()
      Text("Current: "+(teachers.firstOrNull{it.id==existing}?.name?:"Not assigned"),
       style=MaterialTheme.typography.bodySmall)
      OutlinedButton(
       onClick={if(repo!=null)save(
        {repo.homeroom(profile,classForTeacher,teacherChoice.ifBlank{null})},
        "Homeroom teaching assignment updated."
       )else {}},
       enabled=canEdit&&classForTeacher.isNotBlank()&&teacherChoice!=existing,
       modifier=Modifier.fillMaxWidth().testTag("admin-save-homeroom")
      ){Text("Save class teacher")}
     }
    }
   }
   item(key="guardian"){
    AppPanel(Modifier.fillMaxWidth()){
     Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
      Text("3. Link a parent to a learner",style=MaterialTheme.typography.titleLarge)
      MembershipChoice("Active learner",linkStudent,
       students.map{it.id to it.name}){linkStudent=it;verifiedRelationship=false}
      MembershipChoice("Active parent or guardian",linkParent,
       parents.map{it.id to it.name}){linkParent=it;verifiedRelationship=false}
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
       Checkbox(checked=verifiedRelationship,onCheckedChange={verifiedRelationship=it})
       Text("I have verified this guardian relationship and the school's required consent.",
        modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
      }
      val already=snapshot?.guardianLinks?.any{
       it.studentId==linkStudent&&it.guardianId==linkParent}==true
      if(already)Text("This guardian is already linked to this learner.")
      OutlinedButton(
       onClick={
        if(repo!=null)save({repo.link(profile,linkStudent,linkParent)},
         "The parent was linked to this learner.")
        verifiedRelationship=false
       },enabled=canEdit&&linkStudent.isNotBlank()&&linkParent.isNotBlank()&&
        verifiedRelationship&&!already,
       modifier=Modifier.fillMaxWidth().testTag("admin-link-guardian")
      ){Text("Link verified guardian")}
     }
    }
   }
   item(key="existing"){
    Text("Existing parent–learner links",style=MaterialTheme.typography.titleLarge)
   }
   if(snapshot?.guardianLinks?.isEmpty()==true)item(key="none"){
    StateNotice("No linked guardians","There are no active relationships in this school directory.")
   }
   items(snapshot?.guardianLinks.orEmpty(),key={it.id}){link->
    val student=students.firstOrNull{it.id==link.studentId}
    val parent=parents.firstOrNull{it.id==link.guardianId}
    if(student!=null&&parent!=null){
     AppPanel(Modifier.fillMaxWidth()){
      Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
       Text(parent.name,style=MaterialTheme.typography.titleMedium)
       Text("Linked learner: "+student.name)
       OutlinedButton(
        onClick={pendingUnlink=link},enabled=canEdit,
        modifier=Modifier.fillMaxWidth()
       ){Text("Revoke guardian relationship")}
      }
     }
    }
   }
  }
 }
}

@Composable
private fun MembershipChoice(
 label:String,selected:String,options:List<Pair<String,String>>,onChoose:(String)->Unit
){
 var expanded by remember{mutableStateOf(false)}
 Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
  Text(label,style=MaterialTheme.typography.labelMedium)
  Box{
   OutlinedButton(onClick={expanded=true},enabled=options.isNotEmpty(),
    modifier=Modifier.fillMaxWidth()){
    Text(options.firstOrNull{it.first==selected}?.second?:"Choose "+label,maxLines=2)
   }
   DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){
    options.take(200).forEach{item->
     DropdownMenuItem(text={Text(item.second)},onClick={
      onChoose(item.first)
      expanded=false
     })
    }
   }
  }
  if(options.size>200)Text("Only the first 200 choices are shown here; use the web portal for the full register.",
   style=MaterialTheme.typography.bodySmall)
 }
}
