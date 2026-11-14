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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.SessionBroker
import zw.zimlms.portals.PortalAssignment
import zw.zimlms.portals.SchoolDirectory
import zw.zimlms.portals.SchoolDirectoryRepository
import zw.zimlms.ui.AppPanel
import zw.zimlms.ui.Eyebrow
import zw.zimlms.ui.SectionTitle
import zw.zimlms.ui.StateNotice
import java.time.LocalDate
import java.time.ZoneId

/**
 * Narrow, real administrator workflow using the existing class_subject_teacher
 * row-level policies. Only active verified school admins can reach server writes.
 */
@Composable
fun SchoolAssignmentPage(
  profile: CachedProfile,
  directory: SchoolDirectory?,
  modifier: Modifier = Modifier,
  onSaved: () -> Unit
) {
  val context=LocalContext.current.applicationContext
  val scope=rememberCoroutineScope()
  val teachers=remember(directory){directory?.peopleByRole("teacher").orEmpty()}
  val defaultTerm=remember(directory) {
    val today=LocalDate.now(ZoneId.of("Africa/Harare"))
    directory?.terms?.firstOrNull {
      runCatching {
        val start=LocalDate.parse(it.startDate)
        val end=LocalDate.parse(it.endDate)
        !today.isBefore(start) && !today.isAfter(end)
      }.getOrDefault(false)
    } ?: directory?.terms?.firstOrNull()
  }
  var classId by remember(directory) {mutableStateOf(directory?.classes?.firstOrNull()?.id.orEmpty())}
  var subjectId by remember(directory) {mutableStateOf(directory?.subjects?.firstOrNull()?.id.orEmpty())}
  var teacherId by remember(directory) {mutableStateOf(teachers.firstOrNull()?.id.orEmpty())}
  var termId by remember(directory) {mutableStateOf(defaultTerm?.id.orEmpty())}
  var saving by remember {mutableStateOf(false)}
  var feedback by remember {mutableStateOf<String?>(null)}
  var pendingDelete by remember {mutableStateOf<PortalAssignment?>(null)}
  var pendingReplace by remember {mutableStateOf<PortalAssignment?>(null)}

  fun repo()=SchoolDirectoryRepository(
    BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    SessionBroker(context,BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY)
  )

  fun save(existing:PortalAssignment?) {
    if(saving)return
    saving=true
    feedback=null
    scope.launch {
      runCatching {
        withContext(Dispatchers.IO){
          repo().upsertTeachingAssignment(profile,classId,subjectId,teacherId,termId,
            existing?.id?.takeIf { it.isNotBlank() })
        }
      }.fold(
        onSuccess={
          feedback=if(existing==null) "Teaching assignment added."
            else "Teaching assignment updated."
          onSaved()
        },
        onFailure={feedback=it.message ?: "Could not save teaching assignment."}
      )
      saving=false
    }
  }
  fun remove(item:PortalAssignment) {
    if(saving)return
    saving=true
    feedback=null
    scope.launch {
      runCatching {
        withContext(Dispatchers.IO){repo().deleteTeachingAssignment(profile,item.id)}
      }.fold(
        onSuccess={
          feedback="Assignment removed."
          onSaved()
        },
        onFailure={feedback=it.message ?: "Could not remove assignment."}
      )
      saving=false
    }
  }

  if(pendingReplace!=null) {
    val replacing=pendingReplace!!
    AlertDialog(
      onDismissRequest={pendingReplace=null},
      title={Text("Reassign this class?")},
      text={Text("This class, subject and term already have a teacher. " +
        "The existing assignment will be replaced. School permissions are enforced by the server.")},
      confirmButton={
        TextButton(onClick={pendingReplace=null;save(replacing)}){Text("Reassign")}
      },
      dismissButton={TextButton(onClick={pendingReplace=null}){Text("Cancel")}}
    )
  }
  if(pendingDelete!=null) {
    val removing=pendingDelete!!
    AlertDialog(
      onDismissRequest={pendingDelete=null},
      title={Text("Remove teaching assignment?")},
      text={Text("The teacher will no longer have this class/subject link. " +
        "This does not delete the teacher or the class.")},
      confirmButton={TextButton(onClick={pendingDelete=null;remove(removing)}) {
        Text("Remove")
      }},
      dismissButton={TextButton(onClick={pendingDelete=null}){Text("Cancel")}}
    )
  }

  LazyColumn(
    modifier=modifier.fillMaxSize(),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(14.dp)
  ) {
    item(key="header") {
      Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Eyebrow("ACADEMIC ADMINISTRATION")
        Text("Teaching assignments",style=MaterialTheme.typography.headlineMedium)
        Text("Link a teacher to a class, subject and school term.",
          style=MaterialTheme.typography.bodyMedium,
          color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    if(profile.role !in setOf("admin","head")) {
      item(key="no-access"){StateNotice("Access restricted",
        "Only authorised administrators can change teaching assignments.")}
    } else if(directory==null) {
      item(key="offline"){StateNotice("Online access required",
        "Connect with your verified school administrator session. " +
          "Assignments are never changed offline without a server acknowledgement.")}
    } else {
      item(key="notice") {
        StateNotice(
          if(directory.isDemo) "Fictional school demonstration" else "School assignment manager",
          "Changes are written to the verified school through Supabase RLS. " +
            "Inactive demo teachers cannot sign in.")
      }
      item(key="form") {
        AppPanel(Modifier.fillMaxWidth()) {
          Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            SectionTitle("Assign a teacher","Choose all four fields")
            ChoiceField("Class",classId,
              directory.classes.map { it.id to it.name },onChoose={classId=it})
            ChoiceField("Subject",subjectId,
              directory.subjects.map { it.id to it.name },onChoose={subjectId=it})
            ChoiceField("Teacher",teacherId,
              teachers.map { it.id to
                (it.name+if(it.active) "" else " (inactive)") },
              onChoose={teacherId=it})
            ChoiceField("Term",termId,
              directory.terms.map {
                it.id to "${it.name} · ${it.startDate.take(4)}"
              },onChoose={termId=it})
            val existing=directory.assignments.firstOrNull {
              it.classId==classId && it.subjectId==subjectId && it.termId==termId
            }
            val valid=classId.isNotBlank() && subjectId.isNotBlank() &&
              teacherId.isNotBlank() && termId.isNotBlank()
            Button(
              modifier=Modifier.fillMaxWidth(),
              enabled=valid && !saving &&
                (existing==null || existing.teacherId!=teacherId),
              shape=RoundedCornerShape(14.dp),
              onClick={
                if(existing==null)save(null)
                else pendingReplace=existing
              }
            ) {Text(if(saving) "Saving…" else if(existing==null)
              "Add teaching assignment" else if(existing.teacherId==teacherId)
              "Teacher already assigned" else "Reassign this class")}
            if(teachers.isEmpty()) Text("No school teachers available yet.",
              style=MaterialTheme.typography.bodySmall)
          }
        }
      }
      feedback?.let { notice ->
        item(key="feedback"){StateNotice("Server result",notice)}
      }
      item(key="list-heading") {
        SectionTitle("Current teaching links",
          "${directory.assignments.size} assignments")
      }
      if(directory.assignments.isEmpty()) item(key="empty"){
        StateNotice("No assignments","Use the form to create the first teaching link.")
      }
      items(directory.assignments,key={it.id.ifBlank {
        "${it.classId}:${it.subjectId}:${it.termId}"
      }},contentType={"assignment"}){ item->
        AppPanel(Modifier.fillMaxWidth()) {
          Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text(directory.className(item.classId),
              style=MaterialTheme.typography.titleMedium)
            Text(directory.subjectName(item.subjectId),
              style=MaterialTheme.typography.bodyMedium)
            Text("${directory.teacherName(item.teacherId)} • ${directory.termName(item.termId)}",
              style=MaterialTheme.typography.bodySmall,
              color=MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines=2,overflow=TextOverflow.Ellipsis)
            OutlinedButton(
              enabled=!saving && item.id.isNotBlank(),
              onClick={pendingDelete=item},
              shape=RoundedCornerShape(11.dp)
            ){Text("Remove assignment")}
          }
        }
      }
    }
  }
}

@Composable
private fun ChoiceField(
  label:String,
  selected:String,
  options:List<Pair<String,String>>,
  onChoose:(String)->Unit
) {
  var expanded by remember {mutableStateOf(false)}
  val chosen=options.firstOrNull {it.first==selected}?.second ?: "Choose $label"
  Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
    Text(label,style=MaterialTheme.typography.labelMedium)
    androidx.compose.foundation.layout.Box {
      OutlinedButton(
        onClick={expanded=true},
        modifier=Modifier.fillMaxWidth(),
        enabled=options.isNotEmpty(),
        shape=RoundedCornerShape(12.dp)
      ) {Text(chosen,maxLines=1,overflow=TextOverflow.Ellipsis)}
      DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
        options.take(100).forEach{option->
          DropdownMenuItem(text={Text(option.second)},
            onClick={onChoose(option.first);expanded=false})
        }
      }
    }
    if(options.size>100) Text("Showing the first 100 options. Use school web admin for larger lists.",
      style=MaterialTheme.typography.bodySmall)
  }
}
