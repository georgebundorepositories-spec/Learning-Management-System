package zw.zimlms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import zw.zimlms.portals.AssessmentLearner
import zw.zimlms.portals.AssessmentRoster
import zw.zimlms.portals.AssessmentScore
import zw.zimlms.portals.SchoolAssessment
import zw.zimlms.portals.SchoolAssessmentRepository
import zw.zimlms.portals.SchoolDirectory
import zw.zimlms.portals.assessmentPairs
import zw.zimlms.portals.validAssessmentDraft
import zw.zimlms.portals.validCourseworkMark
import zw.zimlms.ui.AppPanel
import zw.zimlms.ui.Eyebrow
import zw.zimlms.ui.StateNotice
import java.time.LocalDate
import java.time.ZoneId

/** School grades remain on Supabase, never in an offline database. This page
 * is disabled if the current verified staff session cannot load school data.
 * Supabase RLS enforces each write even if the UI is manipulated.
 */
@Composable
fun SchoolAssessmentsPage(
 profile:CachedProfile,directory:SchoolDirectory?,modifier:Modifier=Modifier
){
 if(profile.role !in setOf("admin","head","teacher")){
  StateNotice("Staff access required",
   "Only authorised school staff can create assessments or record marks.")
  return
 }
 val context=LocalContext.current.applicationContext
 val scope=rememberCoroutineScope()
 val repo=remember(profile.userId){
  runCatching{
   SchoolAssessmentRepository(BuildConfig.SUPABASE_URL,
    BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    SessionBroker(context,BuildConfig.SUPABASE_URL,
     BuildConfig.SUPABASE_PUBLISHABLE_KEY))
  }.getOrNull()
 }
 val pairs=remember(directory,profile.userId,profile.role){
  assessmentPairs(profile,directory)
 }
 var pairKey by remember(directory){mutableStateOf(pairs.firstOrNull()?.key.orEmpty())}
 var title by remember(profile.userId){mutableStateOf("")}
 var maximum by remember(profile.userId){mutableStateOf("20")}
 var date by remember(profile.userId){
  mutableStateOf(LocalDate.now(ZoneId.of("Africa/Harare")).toString())
 }
 var assessments by remember(profile.userId){
  mutableStateOf<List<SchoolAssessment>>(emptyList())
 }
 var assessmentLoad by remember(profile.userId){mutableIntStateOf(0)}
 var loading by remember(profile.userId){mutableStateOf(false)}
 var saving by remember(profile.userId){mutableStateOf(false)}
 var notice by remember(profile.userId){mutableStateOf<String?>(null)}
 var selectedId by remember(profile.userId){mutableStateOf("")}
 var roster by remember(profile.userId){mutableStateOf<AssessmentRoster?>(null)}
 var rosterError by remember(profile.userId){mutableStateOf<String?>(null)}
 var rosterLoading by remember(profile.userId){mutableStateOf(false)}
 var rosterReload by remember(profile.userId){mutableIntStateOf(0)}

 LaunchedEffect(profile.userId,profile.schoolId,directory,assessmentLoad){
  if(directory==null||repo==null){
   assessments=emptyList()
   notice="Connect with your verified staff account to work with marks."
  }else{
   loading=true
   runCatching{repo.list(profile)}.fold(
    onSuccess={list->
     val allowed=pairs.map{it.key}.toSet()
     assessments=list.filter{(it.classId+":"+it.subjectId) in allowed}
     notice=null
    },
    onFailure={
     assessments=emptyList()
     notice="Assessments are unavailable. Check your school connection and permissions."
    }
   )
   loading=false
  }
 }
 val selected=assessments.firstOrNull{it.id==selectedId}
 LaunchedEffect(profile.userId,selectedId,selected,rosterReload){
  roster=null
  rosterError=null
  if(selected==null||repo==null||directory==null)return@LaunchedEffect
  rosterLoading=true
  runCatching{repo.roster(profile,selected)}.fold(
   onSuccess={roster=it},
   onFailure={
    roster=null
    rosterError="Could not load this class register and marks. Reconnect and retry."
   }
  )
  rosterLoading=false
 }

 fun createAssessment(){
  val pair=pairs.firstOrNull{it.key==pairKey}?:return
  if(saving||repo==null||directory==null)return
  saving=true
  notice=null
  scope.launch{
   runCatching{repo.create(profile,directory,pair,title,maximum,date)}.fold(
    onSuccess={newId->
     title=""
     selectedId=newId
     assessmentLoad++
     rosterReload++
     notice="Assessment saved to school. Select it to record learner marks."
    },
    onFailure={
     notice="Assessment not saved. Confirm your subject assignment, date, maximum mark and connection."
    }
   )
   saving=false
  }
 }

 LazyColumn(
  modifier=modifier.fillMaxSize().testTag("staff-assessments-screen"),
  contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
  verticalArrangement=Arrangement.spacedBy(13.dp)
 ){
  item(key="heading"){
   Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
    Eyebrow("STAFF ACADEMIC RECORDS")
    Text("Assessments & marks",style=MaterialTheme.typography.headlineMedium)
    Text("Create a class assessment and record individual scores and feedback.")
    StateNotice("Protected school records",
     "Every mark is sent to the school server under your verified teaching rights. No marks are saved offline.")
    OutlinedButton(onClick={assessmentLoad++},
     enabled=!loading&&!saving&&repo!=null,
     modifier=Modifier.fillMaxWidth()){
     Text(if(loading)"Refreshing…" else "Refresh assessments")
    }
   }
  }
  if(notice!=null)item(key="notice"){StateNotice("School status",notice!!)}
  if(directory==null||repo==null){
   item(key="offline"){
    StateNotice("Online school session required",
     "Use your verified school account and connect to load assessments.")
   }
  }else{
   if(pairs.isEmpty())item(key="missing"){
    StateNotice("No teaching assignments",
     "Your account has no authorised class and subject combination. Ask the school administrator to assign you.")
   }
   item(key="new"){
    AppPanel(Modifier.fillMaxWidth()){
     Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
      Text("Create a class assessment",style=MaterialTheme.typography.titleLarge)
      AssessmentDropdown("Class and subject",pairKey,
       pairs.map{it.key to it.label}){pairKey=it}
      OutlinedTextField(
       value=title,onValueChange={if(it.length<=160)title=it},
       label={Text("Assessment title")},singleLine=true,
       modifier=Modifier.fillMaxWidth())
      OutlinedTextField(
       value=maximum,onValueChange={if(it.length<=8)maximum=it},
       label={Text("Maximum score")},singleLine=true,
       modifier=Modifier.fillMaxWidth())
      OutlinedTextField(
       value=date,onValueChange={if(it.length<=10)date=it},
       label={Text("Assessment date (YYYY-MM-DD)")},singleLine=true,
       modifier=Modifier.fillMaxWidth())
      val pair=pairs.firstOrNull{it.key==pairKey}
      val canCreate=pair!=null&&validAssessmentDraft(pair,title,maximum,date)
      OutlinedButton(
       onClick={createAssessment()},
       enabled=canCreate&&!saving&&!loading,
       modifier=Modifier.fillMaxWidth().testTag("create-assessment")
      ){Text(if(saving)"Saving…" else "Save assessment")}
     }
    }
   }
   item(key="existing"){
    AppPanel(Modifier.fillMaxWidth()){
     Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
      Text("Enter or correct marks",style=MaterialTheme.typography.titleLarge)
      AssessmentDropdown("Select assessment",selectedId,
       assessments.map{it.id to (it.title+" · "+(directory.className(it.classId))+
        " · "+directory.subjectName(it.subjectId))}){
        selectedId=it
        rosterReload++
       }
      if(assessments.size>=100)Text(
       "Only the latest 100 assessments are available on this mobile page. Use the web portal for older records.",
       style=MaterialTheme.typography.bodySmall)
      if(assessments.isEmpty()&&!loading)Text(
       "There are no accessible assessments yet.",style=MaterialTheme.typography.bodySmall)
     }
    }
   }
   if(selected!=null){
    item(key="selected-info"){
     AppPanel(Modifier.fillMaxWidth()){
      Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
       Eyebrow("SELECTED CLASS ASSESSMENT")
       Text(selected.title,style=MaterialTheme.typography.titleLarge)
       Text(directory.className(selected.classId)+" · "+
        directory.subjectName(selected.subjectId))
       Text("Date: "+selected.assessmentDate+" • Maximum: "+selected.maxScore,
        style=MaterialTheme.typography.bodySmall)
       OutlinedButton(onClick={rosterReload++},
        enabled=!rosterLoading,modifier=Modifier.fillMaxWidth()){
        Text(if(rosterLoading)"Loading class register…" else "Refresh learner marks")
       }
      }
     }
    }
    rosterError?.let{message->
     item(key="roster-error"){StateNotice("Marks unavailable",message)}
    }
    if(roster?.partial==true)item(key="partial"){
     StateNotice("Large class",
      "Only the first 100 active learners can be marked here. Use the web portal for the complete class register.")
    }
    if(roster!=null&&roster?.learners?.isEmpty()==true){
     item(key="no-learners"){
      StateNotice("Class is empty",
       "No active enrolled learner is visible in this assessment's class.")
     }
    }
    val currentRoster=roster
    if(currentRoster!=null){
     items(currentRoster.learners,key={it.id}){learner->
      AssessmentMarkEditor(profile,repo,selected,learner,
       currentRoster.results[learner.id],onSaved={rosterReload++})
     }
    }
   }
  }
 }
}

@Composable
private fun AssessmentMarkEditor(
 actor:CachedProfile,repo:SchoolAssessmentRepository?,
 assessment:SchoolAssessment,learner:AssessmentLearner,
 old:AssessmentScore?,onSaved:()->Unit
){
 val scope=rememberCoroutineScope()
 var score by remember(assessment.id,learner.id,old?.updatedAt){
  mutableStateOf(old?.score.orEmpty())
 }
 var feedback by remember(assessment.id,learner.id,old?.updatedAt){
  mutableStateOf(old?.feedback.orEmpty())
 }
 var saving by remember(assessment.id,learner.id){mutableStateOf(false)}
 var error by remember(assessment.id,learner.id){mutableStateOf<String?>(null)}
 val valid=validCourseworkMark(score.trim(),assessment.maxScore)
 AppPanel(Modifier.fillMaxWidth()){
  Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
   Text(learner.fullName,style=MaterialTheme.typography.titleMedium)
   Text(if(old==null)"Not yet marked" else "Previously recorded: "+old.score+
    " / "+assessment.maxScore,style=MaterialTheme.typography.bodySmall)
   OutlinedTextField(value=score,onValueChange={if(it.length<=10)score=it},
    label={Text("Score out of "+assessment.maxScore)},
    isError=score.isNotBlank()&&!valid,
    singleLine=true,modifier=Modifier.fillMaxWidth())
   OutlinedTextField(value=feedback,onValueChange={if(it.length<=2000)feedback=it},
    label={Text("Teacher feedback (optional)")},
    minLines=2,maxLines=5,modifier=Modifier.fillMaxWidth())
   OutlinedButton(
    onClick={
     if(repo!=null){
      saving=true
      error=null
      scope.launch{
       runCatching{
        repo.saveScore(actor,assessment,learner.id,score,feedback,old?.id)
       }.fold(
        onSuccess={onSaved()},
        onFailure={
         error="No score was saved. Check your teaching rights, class enrolment and connection."
        }
       )
       saving=false
      }
     }
    },
    enabled=!saving&&repo!=null&&valid,
    modifier=Modifier.fillMaxWidth()
   ){Text(if(saving)"Saving mark…" else if(old==null)"Record mark" else "Correct recorded mark")}
   error?.let{StateNotice("Unsaved result",it)}
  }
 }
}

@Composable
private fun AssessmentDropdown(
 label:String,selected:String,choices:List<Pair<String,String>>,onChoose:(String)->Unit
){
 var open by remember{mutableStateOf(false)}
 Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
  Text(label,style=MaterialTheme.typography.labelMedium)
  Box{
   OutlinedButton(
    onClick={open=true},enabled=choices.isNotEmpty(),
    modifier=Modifier.fillMaxWidth()){
    Text(choices.firstOrNull{it.first==selected}?.second?:"Choose "+label,
     maxLines=2)
   }
   DropdownMenu(expanded=open,onDismissRequest={open=false}){
    choices.take(100).forEach{choice->
     DropdownMenuItem(text={Text(choice.second)},onClick={
      onChoose(choice.first)
      open=false
     })
    }
   }
  }
  if(choices.size>100)Text("The first 100 selections are available here. Use the web portal to access more.",
   style=MaterialTheme.typography.bodySmall)
 }
}
