package zw.zimlms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.SessionBroker
import zw.zimlms.portals.SchoolCoursework
import zw.zimlms.portals.SchoolCourseworkAnswer
import zw.zimlms.portals.SchoolCourseworkReview
import zw.zimlms.portals.validCourseworkMark
import zw.zimlms.portals.SchoolCourseworkRepository
import zw.zimlms.portals.SchoolCourseworkSnapshot
import zw.zimlms.portals.SchoolDirectory
import zw.zimlms.ui.AppPanel
import zw.zimlms.ui.Eyebrow
import zw.zimlms.ui.StateNotice

@Composable
fun SchoolCourseworkPage(profile:CachedProfile,directory:SchoolDirectory?,
  modifier:Modifier=Modifier) {
  val context=LocalContext.current.applicationContext
  val repo=remember(profile.userId) {
    SchoolCourseworkRepository(BuildConfig.SUPABASE_URL,
      BuildConfig.SUPABASE_PUBLISHABLE_KEY,
      SessionBroker(context,BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY))
  }
  var snapshot by remember(profile.userId) {mutableStateOf<SchoolCourseworkSnapshot?>(null)}
  var loading by remember(profile.userId) {mutableStateOf(false)}
  var error by remember(profile.userId) {mutableStateOf<String?>(null)}
  var refresh by remember(profile.userId) {mutableIntStateOf(0)}

  LaunchedEffect(profile.userId,refresh) {
    loading=true
    runCatching {repo.load(profile)}.fold(
      onSuccess={snapshot=it;error=null},
      onFailure={
        snapshot=null
        error="Coursework is unavailable. Check your network and school permissions."
      }
    )
    loading=false
  }

  LazyColumn(modifier=modifier.fillMaxSize(),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(12.dp)){
    item {
      Eyebrow("CLASS ASSIGNMENTS")
      Text("Coursework",style=MaterialTheme.typography.headlineMedium)
      Text("Class tasks and private answers.",
        style=MaterialTheme.typography.bodyMedium)
    }
    item {
      StateNotice("Online coursework",
        "Assignments and private answers require an active school connection. " +
        "Do not assume an answer was submitted until the school confirms it.")
      OutlinedButton(onClick={refresh++},enabled=!loading,modifier=Modifier.fillMaxWidth()){
        Text(if(loading)"Checking assignments…" else "Refresh assignments")
      }
    }
    if(error!=null)item{StateNotice("Unable to load",error!!)}
    else if(snapshot?.assignments?.isEmpty()==true && !loading)item{
      StateNotice("No assignments","No coursework is available for your account.")
    }
    val data=snapshot
    if(data!=null) {
      items(data.assignments,key={it.id}){assignment->
        val answers=data.answers.filter{it.assignmentId==assignment.id}
        val own=answers.firstOrNull{it.learnerId==profile.userId}
        val ownReview=data.reviews.firstOrNull{it.submissionId==own?.id}
        AppPanel(Modifier.fillMaxWidth()){
          Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text((directory?.className(assignment.classId)?:"Class")+
              " · "+(directory?.subjectName(assignment.subjectId)?:"Subject"),
              style=MaterialTheme.typography.labelSmall,
              color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(assignment.title,style=MaterialTheme.typography.titleLarge)
            Text(assignment.instructions,style=MaterialTheme.typography.bodyMedium)
            Text("Due: "+(assignment.dueOn?:"No due date specified")+
              " · Maximum: "+assignment.maxScore,
              style=MaterialTheme.typography.bodySmall)
            if(profile.role=="student") {
              if(ownReview!=null) {
                Text("Graded: "+ownReview.score+" / "+assignment.maxScore,
                  style=MaterialTheme.typography.titleMedium)
                Text(ownReview.feedback.ifBlank{"No written feedback recorded."},
                  style=MaterialTheme.typography.bodyMedium)
                StateNotice("Answer reviewed",
                  "Your submitted answer is locked. Contact your teacher about a new assignment.")
              }else{
                CourseworkAnswerEditor(profile,repo,assignment,own,onSaved={refresh++})
              }
            }else if(answers.isNotEmpty()) {
              Text("Submitted answers (${answers.size})",
                style=MaterialTheme.typography.titleSmall)
              answers.take(40).forEach{answer->
                val learnerLabel=if(profile.role=="parent")"Linked learner"
                  else "Learner reference "+answer.learnerId.takeLast(8)
                Text(learnerLabel,style=MaterialTheme.typography.labelMedium)
                Text(answer.text,style=MaterialTheme.typography.bodyMedium)
                val review=data.reviews.firstOrNull{it.submissionId==answer.id}
                if(review!=null) {
                  Text("Mark: "+review.score+" / "+assignment.maxScore,
                    style=MaterialTheme.typography.titleSmall)
                  Text(review.feedback.ifBlank{"No written feedback recorded."},
                    style=MaterialTheme.typography.bodySmall)
                }else{
                  Text("Awaiting teacher review",
                    style=MaterialTheme.typography.bodySmall)
                }
                if(profile.role in setOf("admin","head","teacher")){
                  CourseworkGradeEditor(profile,repo,assignment,answer,review,onSaved={refresh++})
                }
              }
              if(answers.size>40)Text("Additional responses are available in the web portal.",
                style=MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CourseworkAnswerEditor(
  profile:CachedProfile, repo:SchoolCourseworkRepository,
  assignment:SchoolCoursework,own:SchoolCourseworkAnswer?,
  onSaved:()->Unit
) {
  val scope=rememberCoroutineScope()
  var answer by remember(profile.userId,assignment.id,own?.updatedAt) {
    mutableStateOf(own?.text?:"")
  }
  var saving by remember(profile.userId,assignment.id){mutableStateOf(false)}
  var error by remember(profile.userId,assignment.id){mutableStateOf<String?>(null)}
  OutlinedTextField(value=answer,onValueChange={if(it.length<=5000)answer=it},
    label={Text("My answer")},minLines=3,maxLines=8,
    modifier=Modifier.fillMaxWidth())
  OutlinedButton(
    onClick={
      saving=true
      error=null
      scope.launch {
        runCatching{
          repo.submit(profile,assignment.id,answer,own?.id)
        }.fold(
          onSuccess={onSaved()},
          onFailure={error="Could not save your answer. Check your connection and retry."}
        )
        saving=false
      }
    },
    enabled=!saving && answer.trim().isNotEmpty(),
    modifier=Modifier.fillMaxWidth()
  ){Text(if(saving)"Saving…" else if(own!=null)"Update my answer" else "Submit my answer")}
  if(own!=null)Text("Previously saved to school",
    style=MaterialTheme.typography.labelSmall,
    color=MaterialTheme.colorScheme.onSurfaceVariant)
  if(error!=null)StateNotice("Not submitted",error!!)
}

@Composable
private fun CourseworkGradeEditor(
  profile:CachedProfile,repo:SchoolCourseworkRepository,
  assignment:SchoolCoursework,answer:SchoolCourseworkAnswer,
  review:SchoolCourseworkReview?,onSaved:()->Unit
) {
  val scope=rememberCoroutineScope()
  var mark by remember(profile.userId,answer.id,review?.reviewedAt){
    mutableStateOf(review?.score?:"")
  }
  var feedback by remember(profile.userId,answer.id,review?.reviewedAt){
    mutableStateOf(review?.feedback?:"")
  }
  var saving by remember(profile.userId,answer.id){mutableStateOf(false)}
  var error by remember(profile.userId,answer.id){mutableStateOf<String?>(null)}
  val valid=validCourseworkMark(mark.trim(),assignment.maxScore)
  OutlinedTextField(value=mark,onValueChange={if(it.length<=12)mark=it},
    label={Text("Mark out of "+assignment.maxScore)},
    singleLine=true,modifier=Modifier.fillMaxWidth(),
    isError=mark.isNotBlank()&&!valid)
  OutlinedTextField(value=feedback,onValueChange={if(it.length<=2000)feedback=it},
    label={Text("Teacher feedback")},minLines=2,maxLines=5,
    modifier=Modifier.fillMaxWidth())
  OutlinedButton(
    onClick={
      error=null
      saving=true
      scope.launch {
        runCatching{repo.review(profile,answer.id,mark,assignment.maxScore,
          feedback,review?.id)}.fold(
          onSuccess={onSaved()},
          onFailure={error="Review was not saved. Check mark limits, permissions and connection."}
        )
        saving=false
      }
    },enabled=!saving&&valid,modifier=Modifier.fillMaxWidth()
  ){
    Text(if(saving)"Saving review…" else if(review==null)"Record mark" else "Update mark and feedback")
  }
  if(error!=null)StateNotice("Review not saved",error!!)
}
