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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.SessionBroker
import zw.zimlms.portals.AcademicResource
import zw.zimlms.portals.PrivateAssessmentResult
import zw.zimlms.portals.SchoolAcademicRepository
import zw.zimlms.portals.SchoolDirectory
import zw.zimlms.ui.AppPanel
import zw.zimlms.ui.Eyebrow
import zw.zimlms.ui.StateNotice

/** Server-authenticated resources; no private school data is cached to disk. */
@Composable
fun SchoolResourcesPage(profile:CachedProfile,directory:SchoolDirectory?,
  modifier:Modifier=Modifier) {
  val context=LocalContext.current.applicationContext
  val uriHandler=LocalUriHandler.current
  val repo=remember(profile.userId){
    SchoolAcademicRepository(BuildConfig.SUPABASE_URL,
      BuildConfig.SUPABASE_PUBLISHABLE_KEY,
      SessionBroker(context,BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY))
  }
  var resources by remember(profile.userId){mutableStateOf<List<AcademicResource>>(emptyList())}
  var loading by remember(profile.userId){mutableStateOf(false)}
  var message by remember(profile.userId){mutableStateOf<String?>(null)}
  var revision by remember(profile.userId){mutableIntStateOf(0)}

  LaunchedEffect(profile.userId,revision) {
    loading=true
    runCatching { withContext(Dispatchers.IO){repo.resources(profile)} }.fold(
      onSuccess={resources=it;message=null},
      onFailure={
        resources=emptyList()
        message="Learning resources are unavailable. Check your school connection and permissions."
      }
    )
    loading=false
  }

  LazyColumn(modifier=modifier.fillMaxSize(),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(12.dp)) {
    item {
      Eyebrow("ACADEMIC MATERIALS")
      Text("School resources",style=MaterialTheme.typography.headlineMedium)
      Text("Teacher-shared learning links for your school.",
        style=MaterialTheme.typography.bodyMedium)
    }
    item {
      StateNotice("Online resources",
        "These school links require internet access. Your bundled lessons remain available offline.")
      OutlinedButton(onClick={revision++},enabled=!loading,modifier=Modifier.fillMaxWidth()){
        Text(if(loading)"Checking resources…" else "Refresh school resources")
      }
    }
    if(message!=null)item {StateNotice("Unable to load",message!!)}
    else if(resources.isEmpty()&&!loading)item {
      StateNotice("No resources","Your school has not shared any learning resources yet.")
    }
    items(resources,key={it.id}) { resource->
      AppPanel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
          Text(directory?.subjectName(resource.subjectId) ?: "School subject",
            style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
          Text(resource.title,style=MaterialTheme.typography.titleMedium)
          if(resource.description.isNotBlank())
            Text(resource.description,style=MaterialTheme.typography.bodyMedium)
          val url=resource.httpsUrl
          if(url!=null) {
            OutlinedButton(onClick={
              runCatching{uriHandler.openUri(url)}.onFailure{
                message="Could not open the resource link on this device."
              }
            },modifier=Modifier.fillMaxWidth()){
              Text("Open secure resource")
            }
          }
        }
      }
    }
  }
}

/** Learners and linked parents only. The database independently restricts
 * each score using its row-level security policy.
 */
@Composable
fun SchoolPrivateResultsPage(profile:CachedProfile,directory:SchoolDirectory?,
  modifier:Modifier=Modifier){
  val context=LocalContext.current.applicationContext
  val repo=remember(profile.userId){
    SchoolAcademicRepository(BuildConfig.SUPABASE_URL,
      BuildConfig.SUPABASE_PUBLISHABLE_KEY,
      SessionBroker(context,BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY))
  }
  var results by remember(profile.userId){mutableStateOf<List<PrivateAssessmentResult>>(emptyList())}
  var loading by remember(profile.userId){mutableStateOf(false)}
  var message by remember(profile.userId){mutableStateOf<String?>(null)}
  var revision by remember(profile.userId){mutableIntStateOf(0)}

  LaunchedEffect(profile.userId,revision) {
    loading=true
    runCatching{withContext(Dispatchers.IO){repo.privateResults(profile)}}.fold(
      onSuccess={results=it;message=null},
      onFailure={
        results=emptyList()
        message="Results are unavailable. Check your school membership and connection."
      }
    )
    loading=false
  }
  LazyColumn(modifier=modifier.fillMaxSize(),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(12.dp)) {
    item {
      Eyebrow("PRIVATE ACADEMIC RECORDS")
      Text("Assessment results",style=MaterialTheme.typography.headlineMedium)
      Text("Marks visible only to you and the learners linked to your account.",
        style=MaterialTheme.typography.bodyMedium)
    }
    item {
      StateNotice("Protected school results",
        "Scores are loaded securely from the school server. They are not saved for offline access.")
      OutlinedButton(onClick={revision++},enabled=!loading,modifier=Modifier.fillMaxWidth()){
        Text(if(loading)"Loading marks…" else "Refresh results")
      }
    }
    if(message!=null)item {StateNotice("Unable to load",message!!)}
    else if(results.isEmpty()&&!loading)item {
      StateNotice("No results","There are no published assessment scores visible to this account.")
    }
    items(results,key={it.id}) {result->
      val learnerLabel=if(profile.role=="student")"Your result" else {
        val index=directory?.linkedLearners?.indexOfFirst{it.studentId==result.learnerId}?:-1
        if(index>=0)"Linked learner ${index+1}" else "Linked learner"
      }
      AppPanel(Modifier.fillMaxWidth()){
        Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
          Text(learnerLabel+" · "+(directory?.subjectName(result.subjectId)?:"Subject"),
            style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
          Text(result.title,style=MaterialTheme.typography.titleMedium)
          Text(result.score+" / "+result.maximum,
            style=MaterialTheme.typography.headlineSmall)
          Text("Assessment date: "+result.assessmentDate,
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
          if(result.feedback.isNotBlank()) {
            Text("Teacher feedback: "+result.feedback,
              style=MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}
