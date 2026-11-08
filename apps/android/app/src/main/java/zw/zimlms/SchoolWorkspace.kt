package zw.zimlms

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.SessionBroker
import zw.zimlms.learning.StarterLesson
import zw.zimlms.learning.StarterLessons
import zw.zimlms.portals.PortalPerson
import zw.zimlms.portals.SchoolDirectory
import zw.zimlms.portals.SchoolDirectoryRepository
import zw.zimlms.portals.WorkspaceNavigation
import zw.zimlms.portals.WorkspaceTab

/**
 * First functional role-aware app shell.
 *
 * The administrator sees authorised school directory records from Supabase,
 * while staff, parents and learners see ONLY their own RLS-scoped information.
 * Learning lessons are original, bundled and fully accessible offline.
 * An admin viewing parent/learner directories is NOT impersonating those users.
 */
@Composable
fun SchoolWorkspace(profile: CachedProfile) {
  val context = LocalContext.current.applicationContext
  val tabs = remember(profile.role) { WorkspaceNavigation.tabs(profile.role) }
  var selected by remember(profile.userId, profile.role) { mutableStateOf(WorkspaceTab.HOME) }
  var reloadId by remember(profile.userId) { mutableIntStateOf(0) }
  var directory by remember(profile.userId, profile.schoolId) { mutableStateOf<SchoolDirectory?>(null) }
  var loading by remember(profile.userId) { mutableStateOf(false) }
  var directoryNotice by remember(profile.userId) {
    mutableStateOf("School records require a connection. Bundled learning materials work offline.")
  }

  LaunchedEffect(profile.userId, profile.schoolId, reloadId) {
    loading = true
    val result = runCatching {
      val broker = SessionBroker(context, BuildConfig.SUPABASE_URL,
        BuildConfig.SUPABASE_PUBLISHABLE_KEY)
      val repository = SchoolDirectoryRepository(
        BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY, broker)
      repository.load(profile)
    }
    result.fold(
      onSuccess = {
        directory = it
        directoryNotice = if (it.partial)
          "Showing the first 500 authorised records in a large school."
        else if (it.isDemo)
          "Fictional demonstration data. Synthetic learner, parent and teacher accounts are locked."
        else
          "School information loaded securely. Personal records are not stored for offline browsing."
      },
      onFailure = {
        directoryNotice = "Could not load live directories: ${it.message ?: "Network unavailable"}. " +
          "The offline learning library remains accessible."
      }
    )
    loading = false
  }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.fillMaxWidth()) {
    HorizontalDivider()
    Text(WorkspaceNavigation.heading(profile.role),
      style = MaterialTheme.typography.headlineSmall)
    Text(directory?.schoolName ?: "Your verified school account",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.primary)
    if (directory?.isDemo == true) {
      Text("DEMO SCHOOL • Fictional learner, staff and guardian records",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.tertiary)
    }
    Text(directoryNotice, style = MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
      tabs.forEach { tab ->
        FilterChip(
          selected = selected == tab,
          onClick = { selected = tab },
          label = { Text(tab.label) }
        )
      }
    }

    when (selected) {
      WorkspaceTab.HOME -> {
        PortalHome(profile, directory,
          onSelect = { target -> if (target in tabs) selected = target })
        OutlinedButton(enabled = !loading, onClick = { reloadId++ }) {
          Text(if (loading) "Refreshing…" else "Refresh school records")
        }
      }
      WorkspaceTab.LEARNERS ->
        AdminPeoplePortal("Learner register", "student", directory)
      WorkspaceTab.TEACHERS ->
        AdminPeoplePortal("Teacher register", "teacher", directory)
      WorkspaceTab.PARENTS ->
        AdminPeoplePortal("Parent and guardian register", "parent", directory)
      WorkspaceTab.CLASSES ->
        ClassCatalog(directory)
      WorkspaceTab.MY_CLASSES ->
        MyClasses(profile, directory)
      WorkspaceTab.CHILDREN ->
        ParentLearners(directory)
      WorkspaceTab.LEARNING ->
        OfflineLearningLibrary(directory)
      WorkspaceTab.CALENDAR -> {
        if (profile.role in setOf("admin", "head", "teacher")) {
          SchoolCalendarPanel(profile)
        } else {
          Text("The editable calendar is only available to authorised staff.")
        }
      }
    }
  }
}

@Composable
private fun PortalHome(profile: CachedProfile, directory: SchoolDirectory?,
  onSelect: (WorkspaceTab) -> Unit) {
  val admin = profile.role in setOf("admin", "head")
  Text("Your modules", style = MaterialTheme.typography.titleLarge)
  Text(
    if (admin)
      "View school registers, classes, learning materials and the event calendar."
    else when (profile.role) {
      "teacher" -> "See your teaching assignments and offline starter lessons."
      "student" -> "Read learning materials and check your own class."
      "parent" -> "View linked learner classes and explore learning materials."
      else -> "Your school information is protected by account permissions."
    },
    style = MaterialTheme.typography.bodyMedium
  )
  val options = when (profile.role) {
    "admin", "head" -> listOf(
      WorkspaceTab.LEARNERS, WorkspaceTab.TEACHERS, WorkspaceTab.PARENTS,
      WorkspaceTab.CLASSES, WorkspaceTab.LEARNING, WorkspaceTab.CALENDAR)
    "teacher" -> listOf(WorkspaceTab.MY_CLASSES,WorkspaceTab.LEARNING,WorkspaceTab.CALENDAR)
    "student" -> listOf(WorkspaceTab.MY_CLASSES,WorkspaceTab.LEARNING)
    "parent" -> listOf(WorkspaceTab.CHILDREN,WorkspaceTab.LEARNING)
    else -> emptyList()
  }
  options.forEach { target ->
    ElevatedCard(onClick = { onSelect(target) },
      modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(target.label, style = MaterialTheme.typography.titleMedium)
        Text(
          when (target) {
            WorkspaceTab.LEARNERS -> "Authorised learner directory and account status"
            WorkspaceTab.TEACHERS -> "School teachers and active/inactive profiles"
            WorkspaceTab.PARENTS -> "Authorised guardians linked to this school"
            WorkspaceTab.CLASSES -> "Class and subject catalogues"
            WorkspaceTab.MY_CLASSES -> "Your assigned or enrolled class"
            WorkspaceTab.CHILDREN -> "Linked learners and their class groups"
            WorkspaceTab.LEARNING -> "12 complete offline starter lessons with self-check quizzes"
            WorkspaceTab.CALENDAR -> "Staff events and offline-first calendar sync"
            else -> "School module"
          },
          style = MaterialTheme.typography.bodySmall
        )
      }
    }
  }
  if (directory != null) {
    HorizontalDivider()
    Text("School overview", style = MaterialTheme.typography.titleMedium)
    val summary = if (admin) {
      val learners = directory.peopleByRole("student").size
      val teachers = directory.peopleByRole("teacher").size
      val parents = directory.peopleByRole("parent").size
      "${directory.classes.size} classes • ${directory.subjects.size} subjects • " +
        "$learners learners • $teachers teachers • $parents guardians"
    } else {
      "${directory.classes.size} school classes • ${directory.subjects.size} subjects"
    }
    Text(summary, style = MaterialTheme.typography.bodyMedium)
    if (directory.isDemo) Text(
      "These registers contain fictional demonstration identities. The content library is sample learning content, not an official syllabus.",
      style = MaterialTheme.typography.bodySmall
    )
  }
}

@Composable
private fun AdminPeoplePortal(title: String, role: String, directory: SchoolDirectory?) {
  Text(title, style = MaterialTheme.typography.titleLarge)
  Text("Read-only, school-scoped register. Staff account invitations and enrolment editing are not yet available from this screen.",
    style = MaterialTheme.typography.bodySmall)
  if (directory == null) {
    OfflineDirectoryNotice()
    return
  }
  val people = directory.peopleByRole(role)
  Text("${people.size} records", style = MaterialTheme.typography.labelLarge)
  if (people.isEmpty()) {
    Text("No records are available to your account.")
    return
  }
  people.forEach { person -> PersonTile(person, directory.isDemo) }
}

@Composable
private fun PersonTile(person: PortalPerson, demo: Boolean) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(person.name, style = MaterialTheme.typography.titleSmall)
      Text(if (person.active) "Active school profile" else "Inactive account — cannot sign in",
        style = MaterialTheme.typography.bodySmall)
      if (demo) Text("Fictional record", style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.tertiary)
    }
  }
}

@Composable
private fun ClassCatalog(directory: SchoolDirectory?) {
  Text("Classes and subjects", style = MaterialTheme.typography.titleLarge)
  if (directory == null) { OfflineDirectoryNotice(); return }
  Text("${directory.classes.size} classes", style = MaterialTheme.typography.labelMedium)
  directory.classes.forEach { group ->
    Card(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(group.name, style = MaterialTheme.typography.titleMedium)
        Text("Grade: ${group.grade}", style = MaterialTheme.typography.bodySmall)
      }
    }
  }
  HorizontalDivider()
  Text("${directory.subjects.size} subjects", style = MaterialTheme.typography.titleMedium)
  directory.subjects.forEach { subject ->
    AssistChip(onClick = {}, label = { Text(subject.name) },
      enabled = false)
  }
}

@Composable
private fun MyClasses(profile: CachedProfile, directory: SchoolDirectory?) {
  Text("My classes", style = MaterialTheme.typography.titleLarge)
  if (directory == null) { OfflineDirectoryNotice(); return }
  when (profile.role) {
    "teacher" -> {
      if (directory.assignments.isEmpty()) {
        Text("No teaching assignments have been linked to your active profile.")
      }
      directory.assignments.distinct().forEach { assignment ->
        Card(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(directory.className(assignment.classId),
              style = MaterialTheme.typography.titleMedium)
            Text(directory.subjectName(assignment.subjectId),
              style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
    "student" -> {
      if (directory.linkedLearners.isEmpty()) Text("Your class has not been assigned.")
      directory.linkedLearners.forEach {
        Text("Enrolled class: ${directory.className(it.classId)}")
      }
    }
    else -> ClassCatalog(directory)
  }
}

@Composable
private fun ParentLearners(directory: SchoolDirectory?) {
  Text("My learners", style = MaterialTheme.typography.titleLarge)
  Text("For privacy, this preview shows linked class groups but does not expose the private records of unrelated learners.",
    style = MaterialTheme.typography.bodySmall)
  if (directory == null) { OfflineDirectoryNotice(); return }
  if (directory.linkedLearners.isEmpty()) {
    Text("No learners are linked to your parent account yet.")
  }
  directory.linkedLearners.forEachIndexed { index, child ->
    Card(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text("Linked learner ${index + 1}", style = MaterialTheme.typography.titleMedium)
        Text("Class: ${directory.className(child.classId)}")
      }
    }
  }
}

@Composable
private fun OfflineDirectoryNotice() {
  Text("This directory needs a verified online session. It is never saved as unencrypted offline personal data. The learning library is available without internet.",
    style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun OfflineLearningLibrary(directory: SchoolDirectory?) {
  var search by rememberSaveable { mutableStateOf("") }
  var level by rememberSaveable { mutableStateOf("All levels") }
  var openedId by rememberSaveable { mutableStateOf<String?>(null) }
  val levels = remember { listOf("All levels") + StarterLessons.lessons.map { it.level }.distinct() }
  val lesson = StarterLessons.lessons.firstOrNull { it.id == openedId }
  Text("Learning-material library", style = MaterialTheme.typography.titleLarge)
  Text("12 original starter lessons, examples and practice quizzes. Entirely available offline.",
    style = MaterialTheme.typography.bodySmall)
  Text("Sample content only — school-approved syllabus mapping and uploaded materials will follow.",
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.tertiary)

  if (lesson != null) {
    OutlinedButton(onClick = { openedId = null }) { Text("Back to library") }
    LessonReader(lesson)
    return
  }

  OutlinedTextField(
    value = search,
    onValueChange = { search = it.take(100) },
    label = { Text("Search subject or topic") },
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
  )
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    levels.forEach { item ->
      FilterChip(
        selected = level == item,
        onClick = { level = item },
        label = { Text(item) }
      )
    }
  }
  val matching = StarterLessons.lessons.filter { candidate ->
    (level == "All levels" || candidate.level == level) &&
    (search.isBlank() || listOf(candidate.title,candidate.subject,candidate.level).any {
      it.contains(search.trim(),ignoreCase=true)
    })
  }
  Text("${matching.size} lessons", style = MaterialTheme.typography.labelMedium)
  if (matching.isEmpty()) Text("No lessons match these filters.")
  matching.forEach { item ->
    ElevatedCard(onClick = { openedId = item.id },
      modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("${item.level} • ${item.subject}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary)
        Text(item.title, style = MaterialTheme.typography.titleMedium)
        Text(item.objective, style = MaterialTheme.typography.bodySmall)
        Text("Read lesson and practise →",
          style = MaterialTheme.typography.labelMedium)
      }
    }
  }
}

@Composable
private fun LessonReader(lesson: StarterLesson) {
  var chosenIndex by remember(lesson.id) { mutableIntStateOf(-1) }
  var checked by remember(lesson.id) { mutableStateOf(false) }
  Text("${lesson.level} • ${lesson.subject}",
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.primary)
  Text(lesson.title, style = MaterialTheme.typography.headlineSmall)
  Text("Learning aim", style = MaterialTheme.typography.titleMedium)
  Text(lesson.objective)
  HorizontalDivider()
  Text("Read and understand", style = MaterialTheme.typography.titleMedium)
  lesson.explanation.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
  HorizontalDivider()
  Text("Worked example", style = MaterialTheme.typography.titleMedium)
  Text(lesson.workedExample)
  HorizontalDivider()
  Text("Try it yourself", style = MaterialTheme.typography.titleMedium)
  Text(lesson.question, fontWeight = FontWeight.SemiBold)
  lesson.choices.forEachIndexed { index, answer ->
    FilterChip(
      selected = chosenIndex == index,
      onClick = { chosenIndex = index; checked = false },
      label = { Text(answer) },
      modifier = Modifier.fillMaxWidth()
    )
  }
  Button(enabled = chosenIndex >= 0, onClick = { checked = true }) {
    Text("Check my answer")
  }
  if (checked) {
    Text(if (chosenIndex == lesson.correctIndex) "Correct answer!" else "Not quite — review the explanation.",
      color = if (chosenIndex == lesson.correctIndex)
        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
      style = MaterialTheme.typography.titleMedium)
    Text(lesson.feedback)
    OutlinedButton(onClick = { chosenIndex = -1; checked = false }) {
      Text("Try again")
    }
  }
}
