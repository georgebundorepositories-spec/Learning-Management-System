package zw.zimlms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zw.zimlms.attendance.AttendanceRepository
import zw.zimlms.attendance.RosterLearner
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.SessionBroker
import zw.zimlms.portals.SchoolDirectory
import zw.zimlms.ui.AppPanel
import zw.zimlms.ui.Eyebrow
import zw.zimlms.ui.SectionTitle
import zw.zimlms.ui.StateNotice
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val attendanceStatuses = listOf(
  "present" to "P", "absent" to "A", "late" to "L", "excused" to "E"
)
private val attendanceZone = ZoneId.of("Africa/Harare")
private val dayLabel = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

/**
 * Secure online roll-call editor. Works with school-scoped server RLS and
 * obtains learner names only from the restricted attendance roster RPC.
 * Until attendance is integrated into the offline sync engine, no unconfirmed
 * edits are represented as saved offline.
 */
@Composable
fun AttendancePage(
  profile: CachedProfile,
  directory: SchoolDirectory?,
  modifier: Modifier = Modifier
) {
  if (profile.role !in setOf("admin","head","teacher")) {
    StateNotice("School staff access required",
      "Only authorised school staff can mark class attendance.")
    return
  }

  val context=LocalContext.current.applicationContext
  val scope=rememberCoroutineScope()
  val repository=remember(profile.userId,profile.schoolId) {
    val broker=SessionBroker(context,BuildConfig.SUPABASE_URL,
      BuildConfig.SUPABASE_PUBLISHABLE_KEY)
    AttendanceRepository(
      BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY,
      accessToken={broker.accessToken(profile.userId,profile.schoolId)}
    )
  }
  val available=remember(profile.role,directory) {
    val classes=directory?.classes.orEmpty()
    if(profile.role=="teacher") {
      val assigned=directory?.assignments?.map { it.classId }?.toSet().orEmpty()
      classes.filter { it.id in assigned }
    } else classes
  }
  var selectedClassId by remember(profile.userId) { mutableStateOf("") }
  if (available.none { it.id==selectedClassId }) {
    selectedClassId=available.firstOrNull()?.id.orEmpty()
  }
  var date by remember(profile.userId) {
    mutableStateOf(LocalDate.now(attendanceZone))
  }
  var learners by remember(profile.userId) { mutableStateOf<List<RosterLearner>>(emptyList()) }
  var marks by remember(profile.userId) { mutableStateOf<Map<String,String>>(emptyMap()) }
  var loading by remember { mutableStateOf(false) }
  var saving by remember { mutableStateOf(false) }
  var dirty by remember { mutableStateOf(false) }
  var notice by remember {
    mutableStateOf("Connect to save the register. Unsubmitted choices are not stored offline.")
  }

  LaunchedEffect(profile.userId,profile.schoolId,selectedClassId,date) {
    learners=emptyList()
    marks=emptyMap()
    dirty=false
    if(selectedClassId.isBlank())return@LaunchedEffect
    loading=true
    val result=runCatching {
      withContext(Dispatchers.IO) {
        val roster=repository.loadRoster(profile,selectedClassId)
        val records=repository.loadDay(profile,selectedClassId,date)
        roster to records.associate { it.studentUserId to it.status }
      }
    }
    result.fold(
      onSuccess={ pair ->
        learners=pair.first
        marks=pair.second.filterKeys { id -> learners.any { it.userId==id } }
        notice=if(pair.first.isEmpty()) "No learners are enrolled in this class."
          else "Register loaded from the school server. Check each learner and save changes."
      },
      onFailure={
        notice="Could not open attendance online: ${it.message ?: "School connection unavailable"}"
      }
    )
    loading=false
  }

  fun save() {
    if(saving || loading || learners.isEmpty())return
    saving=true
    val expectedClass=selectedClassId
    val expectedDate=date
    val expectedMarks=marks.filterKeys { id -> learners.any { it.userId==id } }
    scope.launch {
      val result=runCatching {
        withContext(Dispatchers.IO) {
          repository.saveDay(profile,expectedClass,expectedDate,expectedMarks)
        }
      }
      result.fold(
        onSuccess={
          notice="Saved $it attendance records to the school server."
          dirty=false
        },
        onFailure={
          // No server ACK means no claim of a saved register.
          notice="Register not confirmed. Keep this screen open and retry online. ${it.message ?: ""}"
        }
      )
      saving=false
    }
  }

  LazyColumn(
    modifier=modifier.fillMaxSize(),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(14.dp)
  ) {
    item(key="header") {
      Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Eyebrow("CLASSROOM OPERATIONS")
        Text("Daily attendance",style=MaterialTheme.typography.headlineMedium)
        Text("Quick roll call for authorised school staff.",
          style=MaterialTheme.typography.bodyMedium,
          color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    item(key="online-notice") {
      StateNotice(
        if(directory?.isDemo==true) "Fictional demonstration learners" else "Online register",
        if(directory?.isDemo==true)
          "All learners in this demonstration are fictional, locked accounts. Entries are for testing only."
        else "Attendance is saved to the school server. Offline attendance sync is still being developed."
      )
    }
    if(directory==null) {
      item(key="no-directory") {
        StateNotice("School connection required",
          "Connect with your verified school account before opening the register.")
      }
      return@LazyColumn
    }
    if(available.isEmpty()) {
      item(key="no-classes") {
        StateNotice("No authorised classes",
          "An administrator must assign a class before a teacher can mark its attendance.")
      }
      return@LazyColumn
    }
    item(key="picker-heading") {
      SectionTitle("Class register","Choose the group you are marking")
    }
    item(key="class-picker") {
      LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        items(available,key={it.id}) { group ->
          FilterChip(
            selected=group.id==selectedClassId,
            onClick={if(!saving){selectedClassId=group.id}},
            label={Text(group.name)}
          )
        }
      }
    }
    item(key="date-picker") {
      AppPanel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
          Text("Attendance date",style=MaterialTheme.typography.labelLarge)
          Row(
            horizontalArrangement=Arrangement.SpaceBetween,
            modifier=Modifier.fillMaxWidth()
          ) {
            OutlinedButton(enabled=!saving,
              onClick={date=date.minusDays(1)}) { Text("Previous") }
            Column {
              Text(date.format(dayLabel),
                style=MaterialTheme.typography.titleMedium)
              Text("Zimbabwe time",
                style=MaterialTheme.typography.labelSmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(enabled=!saving,
              onClick={date=date.plusDays(1)}) { Text("Next") }
          }
          TextButton(enabled=!saving,
            onClick={date=LocalDate.now(attendanceZone)}) {
            Text("Return to today")
          }
        }
      }
    }
    item(key="stats") {
      val shown=learners.size
      val chosen=marks.count { (id,_) -> learners.any { it.userId==id } }
      AppPanel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
          Text("$chosen of $shown learners marked",
            style=MaterialTheme.typography.titleMedium)
          Text(attendanceStatuses.joinToString("   ") { (status,short) ->
            "$short: ${marks.values.count { it==status }}"
          },style=MaterialTheme.typography.bodySmall)
        }
      }
    }
    item(key="status-message") {
      StateNotice(if(loading) "Loading register…" else if(dirty) "Unsaved choices"
        else "Register status",notice)
    }
    if(loading) {
      item(key="loading"){Text("Loading class roster…")}
      return@LazyColumn
    }
    if(learners.isEmpty()) {
      item(key="empty") { Text("No learners loaded for this class.") }
      return@LazyColumn
    }
    item(key="bulk") {
      OutlinedButton(enabled=!saving,onClick={
        marks=learners.associate { it.userId to "present" }
        dirty=true
        notice="All ${learners.size} learners selected as present. Review before saving."
      },modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(13.dp)) {
        Text("Mark all present")
      }
    }
    items(learners,key={it.userId},contentType={"attendance-learner"}) { learner ->
      AppPanel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
          Text(learner.name,style=MaterialTheme.typography.titleMedium,
            fontWeight=FontWeight.SemiBold)
          Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            attendanceStatuses.forEach { (status,short) ->
              FilterChip(
                selected=marks[learner.userId]==status,
                onClick={
                  if(!saving) {
                    marks=marks+(learner.userId to status)
                    dirty=true
                  }
                },
                label={Text(short)},
                modifier=Modifier.semantics {
                  contentDescription="${learner.name}: $status"
                }
              )
            }
          }
        }
      }
    }
    item(key="submit") {
      Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(
          enabled=!loading && !saving && dirty &&
            marks.isNotEmpty() && marks.size==learners.size,
          onClick={save()},
          modifier=Modifier.fillMaxWidth(),
          shape=RoundedCornerShape(14.dp)
        ) {Text(if(saving) "Saving…" else "Save class attendance")}
        Text(
          "For safety, the register requires a server confirmation. Do not close this page after a failed save. Offline-first attendance is pending the Phase 1 mixed-entity sync gate.",
          style=MaterialTheme.typography.bodySmall,
          color=MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
