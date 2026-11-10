package zw.zimlms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import zw.zimlms.learning.HeritageCurriculum
import zw.zimlms.learning.StarterLesson
import zw.zimlms.learning.StarterLessons
import zw.zimlms.portals.PlanTeachingPair
import zw.zimlms.portals.SavedTeacherPlan
import zw.zimlms.portals.SchoolDirectory
import zw.zimlms.portals.TeacherPlanDraft
import zw.zimlms.portals.TeacherPlanRepository
import zw.zimlms.portals.teacherPlanPairs
import zw.zimlms.portals.validateTeacherPlanDraft
import zw.zimlms.ui.AppPanel
import zw.zimlms.ui.Eyebrow
import zw.zimlms.ui.StateNotice

private data class PlanEditRequest(
 val existing:SavedTeacherPlan?=null,val preferredLessonId:String?=null
)

/** Public lesson guides remain available offline, but private school plans
 * are read/written online only with the current encrypted session + RLS.
 * Staff plan text is never saved to device preferences or plaintext files.
 */
@Composable
fun TeacherPlanningPage(
 profile:CachedProfile,directory:SchoolDirectory?,modifier:Modifier=Modifier
) {
 if(profile.role !in setOf("teacher","head","admin")){
  StateNotice("Staff-only workspace",
   "Lesson planning is not available to learner or parent accounts.")
  return
 }
 val context=LocalContext.current.applicationContext
 val lessons=remember(context){
  StarterLessons.lessons+runCatching{HeritageCurriculum.load(context)}.getOrDefault(emptyList())
 }
 val repo=remember(context,profile.userId) {
  runCatching {
   TeacherPlanRepository(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    SessionBroker(context,BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY))
  }.getOrNull()
 }
 val pairs=remember(directory,profile.userId,lessons) {
  teacherPlanPairs(profile,directory,lessons)
 }
 var saved by remember(profile.userId,profile.schoolId) {
  mutableStateOf<List<SavedTeacherPlan>?>(null)
 }
 var busy by remember(profile.userId) {mutableStateOf(false)}
 var networkNotice by remember(profile.userId){mutableStateOf<String?>(null)}
 var refresh by remember(profile.userId){mutableIntStateOf(0)}
 var opened by remember(profile.userId){mutableStateOf<String?>(null)}
 var editing by remember(profile.userId){mutableStateOf<PlanEditRequest?>(null)}

 LaunchedEffect(profile.userId,profile.schoolId,refresh) {
  busy=true
  if(repo==null){
   saved=null
   networkNotice="Cloud plans need the configured school connection. Offline teaching guides remain available."
  }else{
   runCatching {repo.load(profile)}.fold(
    onSuccess={saved=it;networkNotice=null},
    onFailure={
     saved=null
     networkNotice="Saved plans are unavailable. Reconnect and refresh before editing."
    }
   )
  }
  busy=false
 }

 val chosen=lessons.firstOrNull {it.id==opened}
 LazyColumn(
  modifier=modifier.fillMaxSize().testTag("teacher-planning-screen"),
  contentPadding=PaddingValues(horizontal=18.dp,vertical=20.dp),
  verticalArrangement=Arrangement.spacedBy(12.dp)
 ) {
  item(key="header"){
   Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
    Eyebrow("STAFF LESSON PREPARATION")
    Text("Lesson planning",style=MaterialTheme.typography.headlineMedium)
    Text("Review offline teaching guides. Teachers can save and revise private plans online. " +
      "A draft or ready plan is never published to learners automatically.")
    OutlinedButton(
     onClick={refresh++},enabled=!busy&&repo!=null,
     modifier=Modifier.fillMaxWidth()
    ){Text(if(busy)"Loading school plans…" else "Refresh cloud lesson plans")}
   }
  }
  networkNotice?.let{notice->item(key="network-notice"){StateNotice("Online plans",notice)}}
  if(profile.role=="teacher"&&directory==null) item(key="directory-notice"){
   StateNotice("Teaching assignments not loaded",
    "The school directory must be loaded before creating a plan. Return to Dashboard and refresh when online.")
  }
  if(profile.role=="teacher"&&pairs.isEmpty()&&directory!=null) item(key="unassigned"){
   StateNotice("No matching teaching assignment",
    "Ask your school administrator to assign your class and subject. Curriculum-aligned plans can only be saved for authorised teaching groups.")
  }
  if(profile.role=="teacher"&&pairs.isNotEmpty()&&repo!=null&&editing==null)
   item(key="create"){
    OutlinedButton(onClick={editing=PlanEditRequest(preferredLessonId=opened)},
     modifier=Modifier.fillMaxWidth().testTag("teacher-create-plan")){
     Text("Create a private lesson plan")
    }
   }
  val activeEditor=editing
  if(activeEditor!=null&&repo!=null&&profile.role=="teacher"){
   item(key="editor"){
    TeacherPlanEditor(
     profile=profile,repo=repo,pairs=pairs,request=activeEditor,
     onCancel={editing=null},
     onSaved={editing=null;refresh++}
    )
   }
  }else{
   item(key="saved-heading"){
    Text(if(profile.role=="teacher")"My saved school plans" else "School lesson plans",
     style=MaterialTheme.typography.titleLarge)
   }
   if(saved?.isEmpty()==true)item(key="empty-cloud"){
    StateNotice("No saved plans",
     "Your school has not saved any lesson plans visible to this account.")
   }
   if(saved!=null){
    items(saved!!,key={ "saved-"+it.id }){plan->
     AppPanel(Modifier.fillMaxWidth()){
      Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
       Eyebrow((plan.plannedOn?:"Not scheduled")+" · "+plan.status.uppercase())
       Text(plan.title,style=MaterialTheme.typography.titleMedium)
       Text("Objectives: "+plan.objective,maxLines=3)
       Text("Teaching activities: "+plan.steps,maxLines=5)
       if(plan.reflection.isNotBlank())Text("Reflection: "+plan.reflection,maxLines=4)
       if(profile.role=="teacher"&&plan.teacherId==profile.userId&&
          pairs.any{it.classroom.id==plan.classId&&it.subject.id==plan.subjectId}&&repo!=null){
        OutlinedButton(onClick={editing=PlanEditRequest(existing=plan)},
         modifier=Modifier.fillMaxWidth()){Text("Revise my plan")}
       }
      }
     }
    }
   }
   if(chosen!=null){
    item(key="guide-back"){
     OutlinedButton(onClick={opened=null}){Text("All teaching guides")}
    }
    item(key="guide"){
     AppPanel(Modifier.fillMaxWidth()){
      Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
       Eyebrow(chosen.level+" · "+chosen.subject)
       Text(chosen.title,style=MaterialTheme.typography.titleLarge)
       Text("Learning objective: "+chosen.objective)
       Text("Teacher demonstration: "+chosen.workedExample)
       if(chosen.practicalActivity.isNotBlank())
        Text("Practical activity: "+chosen.practicalActivity)
       if(chosen.assignmentInstructions.isNotBlank())
        Text("Possible task: "+chosen.assignmentInstructions)
       chosen.exercises.forEachIndexed {index,exercise->
        Text((index+1).toString()+". "+exercise.question+
         " — Teacher answer: "+exercise.answer)
       }
       if(profile.role=="teacher"&&repo!=null&&
          pairs.any{pair->pair.lessons.any{it.id==chosen.id}}){
        OutlinedButton(
         onClick={editing=PlanEditRequest(preferredLessonId=chosen.id)},
         modifier=Modifier.fillMaxWidth()
        ){Text("Build plan from this guide")}
       }
       Text("Issue graded coursework separately through an authorised assignment tool.")
      }
     }
    }
   }else{
    item(key="guide-heading"){
     Text("Offline teaching guides",style=MaterialTheme.typography.titleLarge)
    }
    items(lessons,key={ "guide-"+it.id }){lesson->
     AppPanel(Modifier.fillMaxWidth()){
      Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
       Eyebrow(lesson.level+" · "+lesson.subject)
       Text(lesson.title,style=MaterialTheme.typography.titleMedium)
       Text(lesson.objective,maxLines=2)
       OutlinedButton(onClick={opened=lesson.id}){Text("Review teaching guide")}
      }
     }
    }
   }
  }
 }
}

@Composable
private fun PlanPicker(label:String,value:String,options:List<Pair<String,String>>,
 onChoose:(String)->Unit
){
 var expanded by remember{mutableStateOf(false)}
 val shown=options.firstOrNull{it.first==value}?.second?:"Choose "+label
 Column(verticalArrangement=Arrangement.spacedBy(3.dp)){
  Text(label,style=MaterialTheme.typography.labelMedium)
  Box{
   OutlinedButton(
    modifier=Modifier.fillMaxWidth(),enabled=options.isNotEmpty(),
    onClick={expanded=true}
   ){Text(shown,maxLines=2)}
   DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){
    options.forEach{option->
     DropdownMenuItem(text={Text(option.second)},onClick={
      onChoose(option.first)
      expanded=false
     })
    }
   }
  }
 }
}

private fun suggestedSteps(lesson:StarterLesson):String=buildString{
 append(lesson.explanation.joinToString("\n\n"))
 append("\n\nTeacher demonstration: ")
 append(lesson.workedExample)
 if(lesson.practicalActivity.isNotBlank()){
  append("\n\nPractical activity: ")
  append(lesson.practicalActivity)
 }
}.take(6000)

private fun suggestedAssessment(lesson:StarterLesson):String=
 lesson.exercises.mapIndexed{index,e->(index+1).toString()+". "+e.question}
  .joinToString("\n").take(3000)

@Composable
private fun TeacherPlanEditor(
 profile:CachedProfile,repo:TeacherPlanRepository,pairs:List<PlanTeachingPair>,
 request:PlanEditRequest,onCancel:()->Unit,onSaved:()->Unit
){
 val original=request.existing
 val allowed=pairs.filter{original==null||
  it.classroom.id==original.classId&&it.subject.id==original.subjectId}
 if(allowed.isEmpty()){
  StateNotice("Teaching assignment revoked",
   "This class/subject is no longer assigned to your account. No change will be saved.")
  OutlinedButton(onClick=onCancel){Text("Close editor")}
  return
 }
 val initialPair=allowed.firstOrNull{
  p->request.preferredLessonId!=null &&
    p.lessons.any{it.id==request.preferredLessonId}
 }?:allowed.first()
 var pairKey by remember(original?.id,request.preferredLessonId){mutableStateOf(initialPair.key)}
 val pair=allowed.firstOrNull{it.key==pairKey}?:allowed.first()
 val initialLesson=original?.lessonId?:request.preferredLessonId?.takeIf{
  id->pair.lessons.any{it.id==id}
 }?:pair.lessons.first().id
 var lessonId by remember(original?.id,request.preferredLessonId){
  mutableStateOf(initialLesson)
 }
 val lesson=pair.lessons.firstOrNull{it.id==lessonId}?:pair.lessons.first()
 var title by remember(original?.id,request.preferredLessonId){
  mutableStateOf(original?.title?:lesson.title)
 }
 var objective by remember(original?.id,request.preferredLessonId){
  mutableStateOf(original?.objective?:lesson.objective)
 }
 var steps by remember(original?.id,request.preferredLessonId){
  mutableStateOf(original?.steps?:suggestedSteps(lesson))
 }
 var resources by remember(original?.id,request.preferredLessonId){
  mutableStateOf(original?.resources?:"")
 }
 var assessment by remember(original?.id,request.preferredLessonId){
  mutableStateOf(original?.assessment?:suggestedAssessment(lesson))
 }
 var reflection by remember(original?.id,request.preferredLessonId){
  mutableStateOf(original?.reflection?:"")
 }
 var plannedOn by remember(original?.id,request.preferredLessonId){
  mutableStateOf(original?.plannedOn?:"")
 }
 var status by remember(original?.id,request.preferredLessonId){
  mutableStateOf(original?.status?:"draft")
 }
 var saving by remember(original?.id,request.preferredLessonId){mutableStateOf(false)}
 var feedback by remember(original?.id,request.preferredLessonId){mutableStateOf<String?>(null)}
 val scope=rememberCoroutineScope()
 fun resetFromLesson(newId:String){
  val chosen=pair.lessons.firstOrNull{it.id==newId}?:return
  lessonId=newId
  title=chosen.title
  objective=chosen.objective
  steps=suggestedSteps(chosen)
  resources=""
  assessment=suggestedAssessment(chosen)
 }
 val draft=TeacherPlanDraft(pair.classroom.id,pair.subject.id,lesson.id,
  title,objective,steps,resources,assessment,reflection,plannedOn.ifBlank{null},status)
 AppPanel(Modifier.fillMaxWidth()){
  Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
   Eyebrow("PRIVATE TEACHER PLAN · SCHOOL CLOUD")
   Text(if(original==null)"New lesson plan" else "Revise lesson plan",
    style=MaterialTheme.typography.titleLarge)
   if(original==null){
    PlanPicker("Class and subject",pair.key,
     allowed.map{it.key to it.label},onChoose={next->
      val chosen=allowed.firstOrNull{it.key==next}?:return@PlanPicker
      pairKey=next
      val first=chosen.lessons.first()
      lessonId=first.id
      title=first.title
      objective=first.objective
      steps=suggestedSteps(first)
      resources=""
      assessment=suggestedAssessment(first)
    })
    PlanPicker("Curriculum lesson",lesson.id,
     pair.lessons.map{it.id to it.title},onChoose={resetFromLesson(it)})
   }else{
    Text(pair.label,style=MaterialTheme.typography.labelMedium)
    Text("Curriculum reference: "+original.lessonId)
   }
   OutlinedTextField(value=title,onValueChange={if(it.length<=180)title=it},
    label={Text("Lesson title")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(value=plannedOn,onValueChange={if(it.length<=10)plannedOn=it},
    label={Text("Lesson date YYYY-MM-DD (optional)")},
    singleLine=true,modifier=Modifier.fillMaxWidth())
   OutlinedTextField(value=objective,onValueChange={if(it.length<=3000)objective=it},
    label={Text("Learning objectives")},minLines=2,maxLines=5,
    modifier=Modifier.fillMaxWidth())
   OutlinedTextField(value=steps,onValueChange={if(it.length<=6000)steps=it},
    label={Text("Teacher and learner activities")},minLines=5,maxLines=9,
    modifier=Modifier.fillMaxWidth())
   OutlinedTextField(value=resources,onValueChange={if(it.length<=3000)resources=it},
    label={Text("Teaching materials")},minLines=2,maxLines=4,
    modifier=Modifier.fillMaxWidth())
   OutlinedTextField(value=assessment,onValueChange={if(it.length<=3000)assessment=it},
    label={Text("Assessment and evaluation")},minLines=2,maxLines=5,
    modifier=Modifier.fillMaxWidth())
   OutlinedTextField(value=reflection,onValueChange={if(it.length<=3000)reflection=it},
    label={Text("Post-lesson reflection")},minLines=2,maxLines=5,
    modifier=Modifier.fillMaxWidth())
   FilterChip(selected=status=="ready",onClick={
    status=if(status=="draft")"ready" else "draft"
   },label={Text(if(status=="ready")"Ready for teaching — private" else "Draft — private")})
   OutlinedButton(onClick={
    saving=true
    feedback=null
    scope.launch{
     runCatching {repo.save(profile,draft,original?.id)}.fold(
      onSuccess={onSaved()},
      onFailure={feedback="Plan was not saved. Check your connection and assignment; retry."}
     )
     saving=false
    }
   },enabled=!saving&&validateTeacherPlanDraft(draft),
    modifier=Modifier.fillMaxWidth().testTag("teacher-save-plan")){
    Text(if(saving)"Saving to school…" else if(original==null)"Save private plan" else "Save revisions")
   }
   OutlinedButton(onClick=onCancel,enabled=!saving,modifier=Modifier.fillMaxWidth()){
    Text("Cancel and return")
   }
   if(!validateTeacherPlanDraft(draft))Text(
    "Enter valid objectives (10+ characters), teaching steps (20+ characters), title and date.",
    style=MaterialTheme.typography.bodySmall,
    color=MaterialTheme.colorScheme.error)
   feedback?.let{StateNotice("Not saved",it)}
   Text("A plan is saved only after the school server confirms it. " +
    "This does not send a task to learners.",
    style=MaterialTheme.typography.bodySmall)
  }
 }
}
