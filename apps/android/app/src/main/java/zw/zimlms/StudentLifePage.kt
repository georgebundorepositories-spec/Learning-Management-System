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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.SessionBroker
import zw.zimlms.portals.PortalSchoolEvent
import zw.zimlms.portals.SchoolOperationsRepository
import zw.zimlms.ui.AppPanel
import zw.zimlms.ui.Eyebrow
import zw.zimlms.ui.StateNotice

/** Displays genuine school announcements; does not fabricate club membership. */
@Composable
fun StudentSchoolLifePage(profile:CachedProfile,modifier:Modifier=Modifier){
  if(profile.role!="student"){
    StateNotice("Learner access only","This page is for enrolled learners.")
    return
  }
  val context=LocalContext.current.applicationContext
  val repo=remember(profile.userId,profile.schoolId){
    runCatching{
      SchoolOperationsRepository(BuildConfig.SUPABASE_URL,
        BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        SessionBroker(context,BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY))
    }.getOrNull()
  }
  var events by remember(profile.userId){mutableStateOf<List<PortalSchoolEvent>>(emptyList())}
  var failure by remember(profile.userId){mutableStateOf<String?>(null)}
  var loading by remember(profile.userId){mutableStateOf(false)}
  var refresh by remember(profile.userId){mutableIntStateOf(0)}
  LaunchedEffect(profile.userId,profile.schoolId,refresh){
    if(repo==null){
      events=emptyList()
      failure="This build has no school connection. Activities need internet."
      return@LaunchedEffect
    }
    loading=true
    runCatching{withContext(Dispatchers.IO){repo.publishedActivities(profile)}}.fold(
      onSuccess={events=it;failure=null},
      onFailure={events=emptyList();failure="Could not load school activities. Check connection and permissions."})
    loading=false
  }
  LazyColumn(modifier=modifier.fillMaxSize(),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(14.dp)){
    item{
      Eyebrow("LEARNER HOME")
      Text("My school life",style=MaterialTheme.typography.headlineMedium)
      Text("School-approved sports and cultural activities.",
        style=MaterialTheme.typography.bodyMedium)
    }
    item{
      OutlinedButton(onClick={refresh++},enabled=!loading&&repo!=null,
        modifier=Modifier.fillMaxWidth()){
        Text(if(loading)"Loading activities…" else "Refresh activities")
      }
    }
    if(failure!=null)item{StateNotice("School activities unavailable",failure!!)}
    else if(events.isEmpty()&&!loading)item{
      StateNotice("No published activities","Your school has not published any approved activities yet.")
    }
    items(events,key={it.id}){event->
      AppPanel(Modifier.fillMaxWidth()){
        Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
          Text(event.title,style=MaterialTheme.typography.titleMedium)
          Text(event.category+" · "+event.startsAt.take(10),
            style=MaterialTheme.typography.bodyMedium)
          if(!event.location.isNullOrBlank())Text(event.location)
        }
      }
    }
    item{
      StateNotice("My clubs","The school has not yet enabled verified club memberships. A listed event does not mean you joined that club.")
    }
    item{
      StateNotice("Student conversations","Peer discussion is not active yet. Staff moderation, reporting and safeguarding approval are required first. Ask your teacher for help.")
    }
  }
}

@Composable
fun StudentCareerPage(profile:CachedProfile,modifier:Modifier=Modifier){
  if(profile.role!="student"){
    StateNotice("Learner access only","This page is for enrolled learners.")
    return
  }
  val ideas=listOf(
    Triple("I enjoy solving problems","Science, technology and practical trades",
      "Try a science project. Ask which school subjects will help."),
    Triple("I enjoy helping people","Teaching, healthcare and community services",
      "Try a school-approved activity that helps others."),
    Triple("I enjoy numbers and business","Accounting, agriculture and entrepreneurship",
      "Plan a simple budget and check the costs."),
    Triple("I enjoy making things","Arts, design and construction",
      "Make a project and ask a teacher for feedback.")
  )
  LazyColumn(modifier=modifier.fillMaxSize(),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(14.dp)){
    item{
      Eyebrow("LEARNER HOME")
      Text("My future",style=MaterialTheme.typography.headlineMedium)
      Text("Explore your interests. You do not need to choose a career today.",
        style=MaterialTheme.typography.bodyMedium)
    }
    items(ideas){idea->
      AppPanel(Modifier.fillMaxWidth()){
        Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
          Text(idea.first,style=MaterialTheme.typography.titleMedium)
          Text("Explore: "+idea.second,style=MaterialTheme.typography.bodyMedium)
          Text("Try: "+idea.third,style=MaterialTheme.typography.bodyMedium)
        }
      }
    }
    item{
      StateNotice("Ask a teacher","Which subjects suit my interests? What clubs could I try? Where can I verify training requirements?")
    }
  }
}
