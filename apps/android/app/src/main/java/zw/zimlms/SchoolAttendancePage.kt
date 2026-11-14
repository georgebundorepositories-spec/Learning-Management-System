package zw.zimlms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.SessionBroker
import zw.zimlms.portals.AttendanceLearner
import zw.zimlms.portals.AttendanceMark
import zw.zimlms.portals.AttendanceRepository
import zw.zimlms.portals.SchoolDirectory
import zw.zimlms.portals.filterSchoolClasses
import zw.zimlms.ui.AppPanel
import zw.zimlms.ui.Eyebrow
import zw.zimlms.ui.MiniStat
import zw.zimlms.ui.SectionTitle
import zw.zimlms.ui.StateNotice
import java.time.LocalDate
import java.time.ZoneId

/** The first functional school attendance register; online-only by design. */
@Composable
fun SchoolAttendancePage(profile:CachedProfile,directory:SchoolDirectory?,
  modifier:Modifier=Modifier) {
  val context=LocalContext.current.applicationContext
  val scope=rememberCoroutineScope()
  val today=remember { LocalDate.now(ZoneId.of("Africa/Harare")) }
  // The UI lists only assigned classes for a teacher. The database
  // independently verifies every roster read and attendance write.
  val availableClasses=remember(directory,profile.role,profile.userId) {
    if(profile.role=="teacher") {
      val assignedIds=directory?.assignments?.map { it.classId }?.toSet().orEmpty()
      directory?.classes?.filter {
        it.id in assignedIds || it.teacherId==profile.userId
      }.orEmpty()
    } else directory?.classes.orEmpty()
  }
  var selectedClass by remember(availableClasses) {
    mutableStateOf(availableClasses.firstOrNull()?.id.orEmpty())
  }
  var classQuery by remember(availableClasses){mutableStateOf("")}
  val filteredClasses=remember(availableClasses,classQuery){
    filterSchoolClasses(availableClasses,classQuery)
  }
  var roster by remember(selectedClass) { mutableStateOf<List<AttendanceLearner>>(emptyList()) }
  var marks by remember(selectedClass) { mutableStateOf<List<AttendanceMark>>(emptyList()) }
  var loading by remember { mutableStateOf(false) }
  var savingId by remember { mutableStateOf<String?>(null) }
  var message by remember { mutableStateOf("Select a class and load today's register.") }
  var reload by remember { mutableStateOf(0) }

  fun repo()=AttendanceRepository(
    BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    SessionBroker(context,BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY)
  )

  LaunchedEffect(selectedClass,reload,profile.userId) {
    if(selectedClass.isEmpty() || profile.role !in setOf("admin","head","teacher"))return@LaunchedEffect
    loading=true
    val result=runCatching {
      withContext(Dispatchers.IO) {
        val api=repo()
        Pair(api.roster(profile,selectedClass),api.day(profile,selectedClass,today))
      }
    }
    result.fold(
      onSuccess={ roster=it.first; marks=it.second
        message="Register loaded from school server for $today." },
      onFailure={ roster=emptyList();marks=emptyList()
        message=it.message?:"Cannot reach school attendance server" }
    )
    loading=false
  }

  LazyColumn(
    modifier=modifier.fillMaxSize(),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(12.dp)
  ) {
    item {
      Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Eyebrow("ACADEMIC RECORDS")
        Text("Daily attendance",style=MaterialTheme.typography.headlineMedium)
        Text("Mark the current school day • $today • Zimbabwe time",
          style=MaterialTheme.typography.bodyMedium,
          color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    item {
      StateNotice("Online register — no offline attendance yet",
        "Each change is confirmed by the school server. Unsaved changes are not silently treated as recorded.")
    }
    if(profile.role !in setOf("admin","head","teacher")) {
      item { StateNotice("Restricted access",
        "Only authorised school staff can mark daily attendance.") }
    }else if(directory==null) {
      item { StateNotice("Connect to school",
        "Attendance needs your online, verified school staff session.") }
    }else {
      item {
        Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
          SectionTitle("Choose class","Only your authorised school classes")
          if(availableClasses.size>4){
            OutlinedTextField(
              value=classQuery,onValueChange={if(it.length<=80)classQuery=it},
              label={Text("Find a class")},
              singleLine=true,
              modifier=Modifier.fillMaxWidth()
            )
          }
          if(filteredClasses.isEmpty()){
            StateNotice("No matching classes","Try another class name or clear your filter.")
          }else{
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
              items(filteredClasses,key={it.id}){klass->
                FilterChip(selected=klass.id==selectedClass,
                  onClick={selectedClass=klass.id},
                  label={Text(klass.name)})
              }
            }
          }
        }
      }
      item {
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
          MiniStat("Learners",roster.size.toString(),"In class",Modifier.weight(1f))
          MiniStat("Marked",marks.size.toString(),"Today",Modifier.weight(1f))
        }
      }
      item {
        StateNotice(if(loading)"Loading register" else "School attendance",message)
        OutlinedButton(enabled=!loading && savingId==null,
          onClick={reload++},modifier=Modifier.fillMaxWidth()) {
          Text("Refresh today's register")
        }
      }
      if(roster.isEmpty() && !loading) item {
        StateNotice("No registered learners",
          "This class has no accessible learners, or attendance could not be loaded.")
      }
      items(roster,key={it.id}) { student ->
        val saved=marks.firstOrNull { it.studentId==student.id }
        AppPanel(Modifier.fillMaxWidth()) {
          Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(student.name,style=MaterialTheme.typography.titleMedium)
            Text("Today: ${saved?.status?.uppercase() ?: "NOT MARKED"}",
              style=MaterialTheme.typography.bodySmall,
              color=MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
              items(listOf("present","absent","late","excused")) { status ->
                FilterChip(
                  selected=saved?.status==status,
                  enabled=savingId==null && !loading,
                  onClick={
                    savingId=student.id
                    scope.launch {
                      val result=runCatching {
                        withContext(Dispatchers.IO) {
                          repo().mark(profile,student,today,status,saved)
                        }
                      }
                      result.fold(
                        onSuccess={
                          message="${student.name}: ${status.replaceFirstChar(Char::uppercase)} saved."
                          reload++
                        },
                        onFailure={message=it.message ?: "Could not save attendance"}
                      )
                      savingId=null
                    }
                  },
                  label={Text(status.take(1).uppercase()+status.drop(1),
                    style=MaterialTheme.typography.labelSmall)}
                )
              }
            }
          }
        }
      }
      item {
        Text("This is the first attendance workflow. Historical registers, bulk marking, offline sync, export and reports are not yet enabled.",
          style=MaterialTheme.typography.bodySmall,
          color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
