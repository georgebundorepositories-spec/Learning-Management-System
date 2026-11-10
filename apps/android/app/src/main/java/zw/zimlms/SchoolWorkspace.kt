package zw.zimlms

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.SessionBroker
import zw.zimlms.learning.HeritageCurriculum
import zw.zimlms.learning.StarterLesson
import zw.zimlms.learning.StarterLessons
import zw.zimlms.learning.LessonProgressVault
import zw.zimlms.learning.LessonCloudSync
import zw.zimlms.portals.PortalPerson
import zw.zimlms.portals.SchoolDirectory
import zw.zimlms.portals.SchoolDirectoryRepository
import zw.zimlms.portals.WorkspaceNavigation
import zw.zimlms.portals.WorkspaceTab
import zw.zimlms.ui.AppPanel
import zw.zimlms.ui.BrandMark
import zw.zimlms.ui.Eyebrow
import zw.zimlms.ui.MiniStat
import zw.zimlms.ui.SectionTitle
import zw.zimlms.ui.StateNotice
import zw.zimlms.ui.WorkspaceIcon
import zw.zimlms.ui.ZimColors
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Tablet-friendly app shell: one scroll container per screen, predictable
 * bottom destinations, and a full navigation drawer for secondary modules.
 * All school directory requests are server-JWT + row-level-security scoped.
 */
@Composable
fun SchoolWorkspace(profile: CachedProfile, onSignOut: () -> Unit) {
  val context = LocalContext.current.applicationContext
  val scope = rememberCoroutineScope()
  val drawer = rememberDrawerState(DrawerValue.Closed)
  val tabs = remember(profile.role) { WorkspaceNavigation.tabs(profile.role) }
  val mainTabs = remember(profile.role) {
    when (profile.role) {
      "admin", "head" -> listOf(WorkspaceTab.HOME, WorkspaceTab.LEARNERS,
        WorkspaceTab.LEARNING, WorkspaceTab.CALENDAR)
      "teacher" -> listOf(WorkspaceTab.HOME, WorkspaceTab.MY_CLASSES,
        WorkspaceTab.LEARNING, WorkspaceTab.CALENDAR)
      "parent" -> listOf(WorkspaceTab.HOME, WorkspaceTab.CHILDREN,WorkspaceTab.LEARNING)
      "student" -> listOf(WorkspaceTab.HOME, WorkspaceTab.MY_CLASSES,WorkspaceTab.LEARNING)
      else -> listOf(WorkspaceTab.HOME, WorkspaceTab.LEARNING)
    }.filter { it in tabs }
  }
  var selectedKey by rememberSaveable(profile.userId,profile.role) {
    mutableStateOf(WorkspaceTab.HOME.name)
  }
  val selected = tabs.find { it.name == selectedKey } ?: WorkspaceTab.HOME
  var retryNumber by remember(profile.userId) { mutableIntStateOf(0) }
  var directory by remember(profile.userId,profile.schoolId) {
    mutableStateOf<SchoolDirectory?>(null)
  }
  var loading by remember(profile.userId) { mutableStateOf(false) }
  var status by remember(profile.userId) {
    mutableStateOf("Connecting to your school…")
  }

  LaunchedEffect(profile.userId,profile.schoolId,retryNumber) {
    loading = true
    val result = runCatching {
      withContext(Dispatchers.IO) {
        val broker = SessionBroker(context, BuildConfig.SUPABASE_URL,
          BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        SchoolDirectoryRepository(BuildConfig.SUPABASE_URL,
          BuildConfig.SUPABASE_PUBLISHABLE_KEY,broker).load(profile)
      }
    }
    result.fold(
      onSuccess = {
        directory = it
        status = if (it.isDemo)
          "Demo school • fictional records, protected by school permissions"
        else if (it.partial)
          "Showing the first 500 permitted records"
        else "School records are current"
      },
      onFailure = {
        // Do not keep an old account's records on screen after an auth/network failure.
        directory = null
        status = "School directory unavailable; offline lessons still work"
      }
    )
    loading = false
  }

  fun navigate(target: WorkspaceTab) {
    if (target in tabs) selectedKey = target.name
  }
  BackHandler(enabled=selected!=WorkspaceTab.HOME || drawer.isOpen) {
    if (drawer.isOpen) scope.launch { drawer.close() }
    else selectedKey=WorkspaceTab.HOME.name
  }

  ModalNavigationDrawer(
    drawerState=drawer,
    drawerContent={
      ModalDrawerSheet(
        drawerContainerColor=MaterialTheme.colorScheme.surface,
        modifier=Modifier.width(300.dp)
      ) {
        Row(Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=24.dp),
          verticalAlignment=Alignment.CenterVertically,
          horizontalArrangement=Arrangement.spacedBy(12.dp)) {
          BrandMark()
          Column {
            Text("ZimSchool",style=MaterialTheme.typography.titleLarge)
            Text("LEARNING MANAGEMENT",style=MaterialTheme.typography.labelSmall,
              color=MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        // Keep the account controls pinned at the bottom on small phones.
        // The module list alone scrolls; otherwise nine administrator modules
        // push Sign out below the visible screen on Android 8 handsets.
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
          Text("WORKSPACE",modifier=Modifier.padding(start=24.dp,top=22.dp,bottom=9.dp),
            style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
          tabs.forEach { tab ->
            NavigationDrawerItem(
              label={ Text(tab.label,style=MaterialTheme.typography.labelLarge) },
              selected=tab==selected,
              onClick={
                navigate(tab)
                scope.launch { drawer.close() }
              },
              icon={ WorkspaceIcon(tab,color=if(tab==selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant) },
              modifier=Modifier.padding(horizontal=12.dp,vertical=2.dp),
              shape=RoundedCornerShape(14.dp)
            )
          }
        }
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        Text(profile.fullName,modifier=Modifier.padding(start=22.dp,top=16.dp),
          maxLines=1,overflow=TextOverflow.Ellipsis,
          style=MaterialTheme.typography.titleMedium)
        Text(WorkspaceNavigation.heading(profile.role),
          modifier=Modifier.padding(start=22.dp,top=3.dp),
          style=MaterialTheme.typography.bodySmall,
          color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick={
          // Fire the session-close action synchronously. A drawer animation
          // must never delay or suppress a user's explicit sign-out.
          onSignOut()
          scope.launch { drawer.close() }
        },modifier=Modifier.padding(start=12.dp,bottom=14.dp)
            .testTag("drawer-sign-out")) {
          Text("Sign out of this device")
        }
      }
    }
  ) {
    Scaffold(
      containerColor=MaterialTheme.colorScheme.background,
      topBar={
        Surface(
          color=MaterialTheme.colorScheme.surface,
          shadowElevation=2.dp
        ) {
          Row(
            modifier=Modifier.fillMaxWidth().height(68.dp).padding(horizontal=12.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(9.dp)
          ) {
            IconButton(
              onClick={
                if (selected == WorkspaceTab.HOME) {
                  scope.launch { drawer.open() }
                } else {
                  navigate(WorkspaceTab.HOME)
                }
              },
              modifier=Modifier.semantics {
                contentDescription=if(selected == WorkspaceTab.HOME)
                  "Open navigation menu" else "Back to dashboard"
              }
            ) {
              if (selected == WorkspaceTab.HOME) {
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                  repeat(3) {
                    Spacer(Modifier.width(21.dp).height(2.dp)
                      .clip(RoundedCornerShape(2.dp))
                      .background(MaterialTheme.colorScheme.onSurface))
                  }
                }
              } else {
                Text("←", style=MaterialTheme.typography.headlineMedium,
                  color=MaterialTheme.colorScheme.onSurface)
              }
            }
            BrandMark(Modifier.size(36.dp))
            Column(Modifier.weight(1f)) {
              Text("ZimSchool",style=MaterialTheme.typography.titleMedium)
              Text(if(selected != WorkspaceTab.HOME) selected.label.uppercase()
              else if(directory?.isDemo==true) "DEMONSTRATION SCHOOL"
              else "YOUR SCHOOL WORKSPACE",
                style=MaterialTheme.typography.labelSmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
              modifier=Modifier.size(40.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { scope.launch { drawer.open() } }
                .semantics { contentDescription="Open account menu" },
              contentAlignment=Alignment.Center
            ) {
              Text(profile.fullName.trim().take(1).uppercase().ifEmpty { "S" },
                style=MaterialTheme.typography.titleMedium,
                color=MaterialTheme.colorScheme.onPrimaryContainer)
            }
          }
        }
      },
      bottomBar={
        if (mainTabs.size>1) {
          NavigationBar(
            containerColor=MaterialTheme.colorScheme.surface,
            tonalElevation=2.dp
          ) {
            mainTabs.forEach { tab ->
              NavigationBarItem(
                selected=selected==tab,
                onClick={navigate(tab)},
                icon={ WorkspaceIcon(tab,color=if(selected==tab)
                  MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant) },
                label={ Text(
                  if(tab==WorkspaceTab.LEARNING) "Library"
                  else if(tab==WorkspaceTab.MY_CLASSES) "Classes"
                  else if(tab==WorkspaceTab.CHILDREN) "Family"
                  else tab.label,
                  maxLines=1,style=MaterialTheme.typography.labelSmall) },
                alwaysShowLabel=true
              )
            }
          }
        }
      }
    ) { insets ->
      Box(Modifier.fillMaxSize().testTag("module-screen-${selected.name.lowercase()}")) {
        when (selected) {
        WorkspaceTab.HOME -> HomePage(
          profile,directory,status,loading,
          modifier=Modifier.padding(insets),
          onNavigate={navigate(it)},
          onRefresh={retryNumber++},
          onSignOut=onSignOut
        )
        WorkspaceTab.LEARNING -> LearningPage(profile,Modifier.padding(insets))
        WorkspaceTab.CALENDAR -> {
          if (profile.role in setOf("admin","head","teacher")) {
            Column(Modifier.fillMaxSize().padding(insets)
              .verticalScroll(rememberScrollState()).padding(18.dp)) {
              SchoolCalendarPanel(profile)
              Spacer(Modifier.height(24.dp))
            }
          } else {
            StateNotice("Staff access required",
              "This calendar editor is available only to authorised school staff.")
          }
        }
        WorkspaceTab.LEARNERS -> PeoplePage(directory,"student",
          "Learner register","Enrolled learners",
          Modifier.padding(insets))
        WorkspaceTab.TEACHERS -> PeoplePage(directory,"teacher",
          "Teacher directory","Teaching staff",
          Modifier.padding(insets))
        WorkspaceTab.PARENTS -> PeoplePage(directory,"parent",
          "Parent & guardian directory","School families",
          Modifier.padding(insets))
        WorkspaceTab.CLASSES -> ClassPage(directory,Modifier.padding(insets))
        WorkspaceTab.ASSIGNMENTS -> SchoolAssignmentPage(
          profile,directory,Modifier.padding(insets),onSaved={retryNumber++})
        WorkspaceTab.ATTENDANCE -> SchoolAttendancePage(
          profile,directory,Modifier.padding(insets))
        WorkspaceTab.NOTICES -> SchoolNoticesPage(profile,Modifier.padding(insets))
        WorkspaceTab.FEES -> SchoolFeesPage(profile,Modifier.padding(insets))
        WorkspaceTab.RESOURCES -> SchoolResourcesPage(profile,directory,Modifier.padding(insets))
        WorkspaceTab.RESULTS -> SchoolPrivateResultsPage(profile,directory,Modifier.padding(insets))
        WorkspaceTab.COURSEWORK -> SchoolCourseworkPage(profile,directory,Modifier.padding(insets))
        WorkspaceTab.MY_CLASSES -> MyClassesPage(profile,directory,Modifier.padding(insets))
        WorkspaceTab.CHILDREN -> FamilyPage(directory,Modifier.padding(insets))
        }
      }
    }
  }
}

@Composable
private fun HomePage(
  profile: CachedProfile,
  directory: SchoolDirectory?,
  status: String,
  loading: Boolean,
  modifier: Modifier=Modifier,
  onNavigate: (WorkspaceTab)->Unit,
  onRefresh: ()->Unit,
  onSignOut: ()->Unit
) {
  val admin=profile.role in setOf("admin","head")
  val modules=WorkspaceNavigation.tabs(profile.role).filter { it!=WorkspaceTab.HOME }
  val date=remember { LocalDate.now(ZoneId.of("Africa/Harare"))
    .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")) }
  LazyColumn(
    modifier=modifier.fillMaxSize().testTag("home-module-list"),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(19.dp)
  ) {
    item(key="hello") {
      Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Eyebrow(date)
        Text("Welcome back, ${profile.fullName.trim().substringBefore(' ')}",
          style=MaterialTheme.typography.headlineMedium)
        Text(WorkspaceNavigation.heading(profile.role),
          style=MaterialTheme.typography.bodyMedium,
          color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    item(key="hero") { HomeHero(profile,directory) }
    item(key="online-status") {
      StateNotice(if(loading) "Connecting to school…" else
        if(directory!=null) "School connection ready" else "Offline mode",
        status,if(directory!=null) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.tertiary)
    }
    if (directory!=null) {
      item(key="metrics") {
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
          SectionTitle("School at a glance","Authorised records in your school")
          Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            MiniStat("Classes",directory.classes.size.toString(),"Academic groups",
              Modifier.weight(1f))
            MiniStat("Subjects",directory.subjects.size.toString(),"Available subjects",
              Modifier.weight(1f))
          }
          if (admin) {
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
              MiniStat("Learners",directory.peopleByRole("student").size.toString(),
                "Registered profiles",Modifier.weight(1f))
              MiniStat("Teachers",directory.peopleByRole("teacher").size.toString(),
                "Teaching profiles",Modifier.weight(1f))
            }
          }
        }
      }
    }
    item(key="modules-header") {
      SectionTitle("Explore your workspace",
        "Choose a tool to open a full-screen module")
    }
    items(modules.chunked(2),key={row->row.joinToString("-"){it.name}}) { row ->
      Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        row.forEach { tab ->
          DashboardModule(
            tab=tab,
            modifier=Modifier.weight(1f),
            onClick=onNavigate
          )
        }
        if(row.size==1) Spacer(Modifier.weight(1f))
      }
    }
    item(key="refresh") {
      OutlinedButton(enabled=!loading,onClick=onRefresh,
        modifier=Modifier.fillMaxWidth()) {
        Text(if(loading) "Checking school connection…" else "Refresh school information")
      }
    }
    item(key="sign-out") {
      OutlinedButton(
        onClick=onSignOut,
        modifier=Modifier.fillMaxWidth().testTag("home-sign-out"),
        shape=RoundedCornerShape(14.dp)
      ) {
        Text("Sign out of this device")
      }
    }
    item(key="footer") {
      Text(
        "ZimSchool preview • Directories are read-only and require an online session. " +
        "Bundled lessons are available offline. No learner data is stored in the app's directory cache.",
        style=MaterialTheme.typography.bodySmall,
        color=MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun HomeHero(profile: CachedProfile,directory: SchoolDirectory?) {
  Box(
    modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
      .background(Brush.linearGradient(listOf(
        Color(0xFF063B31),Color(0xFF08745E),Color(0xFF13947B)
      ))).padding(24.dp)
  ) {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
      Eyebrow("ZIMSCHOOL • SCHOOL HUB",Color(0xFFAEF5D9))
      Text(directory?.schoolName ?: "Learning without limits",
        color=Color.White,
        style=MaterialTheme.typography.headlineMedium)
      Text(
        when(profile.role) {
          "admin","head" -> "Your school, your people, and your learning resources in one place."
          "teacher" -> "Your teaching assignments and learning resources, made accessible."
          "student" -> "Keep learning and practising, even when internet access is unreliable."
          "parent" -> "Stay connected to your learner's school and learning journey."
          else -> "A simpler way to access your school."
        },
        color=Color(0xFFD5F6E9),
        style=MaterialTheme.typography.bodyMedium
      )
      Surface(color=Color.White.copy(alpha=.16f),
        shape=RoundedCornerShape(50.dp)) {
        Text(if(directory?.isDemo==true) "DEMO • FICTIONAL RECORDS"
        else "OFFLINE-FIRST PREVIEW",
          modifier=Modifier.padding(horizontal=12.dp,vertical=7.dp),
          color=Color.White,style=MaterialTheme.typography.labelSmall)
      }
    }
  }
}

@Composable
private fun DashboardModule(
  tab: WorkspaceTab,modifier: Modifier=Modifier,onClick:(WorkspaceTab)->Unit
) {
  // Material3's clickable Surface owns the touch target and accessibility
  // action. A clickable modifier on a nested, non-clickable Surface can leave
  // the visual card tappable in appearance only on some Android devices.
  Surface(
    onClick={ onClick(tab) },
    modifier=modifier
      .testTag("home-tool-${tab.name.lowercase()}")
      .semantics { contentDescription="Open ${tab.label}" },
    shape=RoundedCornerShape(20.dp),
    color=MaterialTheme.colorScheme.surface,
    tonalElevation=0.dp,
    shadowElevation=1.dp,
    border=androidx.compose.foundation.BorderStroke(
      1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Column(
      modifier=Modifier.fillMaxWidth().padding(18.dp).height(119.dp),
      verticalArrangement=Arrangement.SpaceBetween
    ) {
      Box(
        modifier=Modifier.size(42.dp)
          .clip(RoundedCornerShape(13.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment=Alignment.Center
      ) {
        WorkspaceIcon(tab,color=MaterialTheme.colorScheme.onPrimaryContainer)
      }
      Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Text(tab.label,style=MaterialTheme.typography.titleSmall,
          maxLines=1,overflow=TextOverflow.Ellipsis)
        Text(when(tab) {
          WorkspaceTab.LEARNERS -> "Learner register"
          WorkspaceTab.TEACHERS -> "Staff directory"
          WorkspaceTab.PARENTS -> "Family register"
          WorkspaceTab.CLASSES -> "Class and subjects"
          WorkspaceTab.ASSIGNMENTS -> "Manage teaching links"
          WorkspaceTab.ATTENDANCE -> "Mark today's register"
          WorkspaceTab.MY_CLASSES -> "Your assigned groups"
          WorkspaceTab.CHILDREN -> "Linked learners"
          WorkspaceTab.LEARNING -> "Offline lessons and practice"
          WorkspaceTab.CALENDAR -> "School events"
          WorkspaceTab.NOTICES -> "School announcements"
          WorkspaceTab.FEES -> "Private school charges"
          WorkspaceTab.RESOURCES -> "Teacher-shared materials"
          WorkspaceTab.RESULTS -> "Private assessment marks"
          WorkspaceTab.COURSEWORK -> "Class tasks and submissions"
          WorkspaceTab.HOME -> "Overview"
        },style=MaterialTheme.typography.bodySmall,
          color=MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines=1,overflow=TextOverflow.Ellipsis)
      }
    }
  }
}

@Composable
private fun PeoplePage(
  directory: SchoolDirectory?,
  role: String,
  heading: String,
  label: String,
  modifier: Modifier=Modifier
) {
  val people=remember(directory,role) { directory?.peopleByRole(role).orEmpty() }
  LazyColumn(
    modifier=modifier.fillMaxSize().testTag("people-directory-${role}"),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(12.dp)
  ) {
    item(key="heading") { Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
      Eyebrow("SCHOOL DIRECTORY")
      Text(heading,style=MaterialTheme.typography.headlineMedium)
      Text(if(directory!=null) "${people.size} $label"
        else "Available with a verified school connection",
        style=MaterialTheme.typography.bodyMedium,
        color=MaterialTheme.colorScheme.onSurfaceVariant)
    }}
    item(key="notice") {
      StateNotice("Protected and read-only",
        "This register shows only permitted school records. Enrolment and invitation management are not yet available in this version.")
    }
    if(directory==null) {
      item(key="offline") { OfflineDirectory() }
    } else if(people.isEmpty()) {
      item(key="empty") { StateNotice("No records yet",
        "No $label are available to your authorised account.") }
    } else {
      items(people,key={it.id},contentType={"person"}) { person ->
        PersonRow(person,directory.isDemo)
      }
    }
  }
}

@Composable
private fun PersonRow(person: PortalPerson, demo: Boolean) {
  AppPanel(Modifier.fillMaxWidth()) {
    Row(verticalAlignment=Alignment.CenterVertically,
      horizontalArrangement=Arrangement.spacedBy(12.dp)) {
      Box(Modifier.size(43.dp).clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment=Alignment.Center) {
        Text(person.name.take(1),style=MaterialTheme.typography.titleMedium,
          color=MaterialTheme.colorScheme.onPrimaryContainer)
      }
      Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(person.name,style=MaterialTheme.typography.titleSmall)
        Text(if(demo) "FICTIONAL PROFILE" else person.role.uppercase(),
          style=MaterialTheme.typography.labelSmall,
          color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Surface(color=if(person.active) MaterialTheme.colorScheme.primaryContainer
      else MaterialTheme.colorScheme.surfaceVariant,
        shape=RoundedCornerShape(8.dp)) {
        Text(if(person.active) "Active" else "Inactive",
          modifier=Modifier.padding(horizontal=9.dp,vertical=5.dp),
          style=MaterialTheme.typography.labelSmall,
          color=if(person.active) MaterialTheme.colorScheme.onPrimaryContainer
          else MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
private fun ClassPage(directory:SchoolDirectory?,modifier:Modifier=Modifier) {
  LazyColumn(
    modifier=modifier.fillMaxSize(),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(12.dp)
  ) {
    item(key="head") { Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
      Eyebrow("ACADEMIC STRUCTURE")
      Text("Classes & subjects",style=MaterialTheme.typography.headlineMedium)
      Text("Read-only school catalogue",style=MaterialTheme.typography.bodySmall,
        color=MaterialTheme.colorScheme.onSurfaceVariant)
    }}
    if(directory==null) item(key="offline"){OfflineDirectory()}
    else {
      item(key="classes-title"){ SectionTitle("Classes",
        "${directory.classes.size} academic groups")}
      items(directory.classes,key={"class-${it.id}"},contentType={"class"}){group->
        AppPanel(Modifier.fillMaxWidth()) {
          Row(verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment=Alignment.Center) {
              WorkspaceIcon(WorkspaceTab.CLASSES,
                color=MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column {
              Text(group.name,style=MaterialTheme.typography.titleMedium)
              Text(group.grade,style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
      item(key="subjects-title"){ SectionTitle("Subjects",
        "${directory.subjects.size} available subjects")}
      items(directory.subjects,key={"subject-${it.id}"},contentType={"subject"}) {subject->
        AppPanel(Modifier.fillMaxWidth()) {
          Text(subject.name,style=MaterialTheme.typography.titleSmall)
        }
      }
    }
  }
}

@Composable
private fun MyClassesPage(
  profile:CachedProfile,directory:SchoolDirectory?,modifier:Modifier=Modifier
) {
  LazyColumn(
    modifier=modifier.fillMaxSize(),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(12.dp)
  ) {
    item(key="heading"){
      Eyebrow("ACADEMIC LIFE")
      Text("My classes",style=MaterialTheme.typography.headlineMedium)
    }
    if(directory==null) item(key="offline"){OfflineDirectory()}
    else if(profile.role=="teacher") {
      if(directory.assignments.isEmpty()) item(key="no-assignments"){
        StateNotice("No teaching groups yet",
          "Your school administrator needs to assign subjects and classes to your profile.")
      }
      items(directory.assignments.distinct(),key={"${it.classId}-${it.subjectId}"}) {assignment->
        AppPanel(Modifier.fillMaxWidth()) {
          Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Eyebrow("TEACHING GROUP")
            Text(directory.className(assignment.classId),
              style=MaterialTheme.typography.titleLarge)
            Text(directory.subjectName(assignment.subjectId),
              style=MaterialTheme.typography.bodyMedium)
          }
        }
      }
    } else if(profile.role=="student") {
      if(directory.linkedLearners.isEmpty()) item(key="not-enrolled"){
        StateNotice("No class assigned",
          "Your learner profile is not yet linked to an academic group.")
      }
      items(directory.linkedLearners,key={it.studentId}) {learner->
        AppPanel(Modifier.fillMaxWidth()) {
          Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
            Eyebrow("MY ENROLMENT")
            Text(directory.className(learner.classId),
              style=MaterialTheme.typography.titleLarge)
          }
        }
      }
    }
  }
}

@Composable
private fun FamilyPage(directory:SchoolDirectory?,modifier:Modifier=Modifier) {
  LazyColumn(
    modifier=modifier.fillMaxSize(),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(12.dp)
  ) {
    item(key="heading") {
      Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Eyebrow("PARENT PORTAL")
        Text("My learners",style=MaterialTheme.typography.headlineMedium)
        Text("Only learners linked to your verified parent account are shown.",
          style=MaterialTheme.typography.bodyMedium)
      }
    }
    if(directory==null) item(key="offline"){OfflineDirectory()}
    else if(directory.linkedLearners.isEmpty()) item(key="no-links"){
      StateNotice("No learner links",
        "Your school administrator has not linked a learner to this parent account.")
    } else {
      items(directory.linkedLearners,key={it.studentId}) { child->
        AppPanel(Modifier.fillMaxWidth()) {
          Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Eyebrow("LINKED LEARNER")
            Text("Enrolled learner",style=MaterialTheme.typography.titleMedium)
            Text("Class: ${directory.className(child.classId)}",
              style=MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}

@Composable
private fun OfflineDirectory() {
  StateNotice("Connection required",
    "School registers are not downloaded as unencrypted private data. " +
      "Reconnect with a verified school session or use the offline learning library.")
}

@Composable
private fun LearningPage(profile:CachedProfile,modifier:Modifier=Modifier) {
  val context=LocalContext.current.applicationContext
  // One versioned curriculum catalog for both Android and the web. Reading
  // app assets needs no connection, and completion remains scoped to the
  // encrypted local school/user progress vault.
  val catalogResult=remember(context) { runCatching { HeritageCurriculum.load(context) } }
  val allLessons=remember(catalogResult) {
    StarterLessons.lessons + catalogResult.getOrDefault(emptyList())
  }
  val scope=rememberCoroutineScope()
  val vault=remember { LessonProgressVault(context) }
  // Offline lessons must work in the demo/emulator even when school
  // Supabase credentials are not configured in the debug build.
  val cloud=remember(profile.userId,profile.schoolId) {
    if(profile.role=="student" && BuildConfig.SUPABASE_URL.startsWith("https://")
      && BuildConfig.SUPABASE_PUBLISHABLE_KEY.startsWith("sb_publishable_")) {
      LessonCloudSync(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        SessionBroker(context,BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY))
    } else null
  }
  var completed by remember(profile.userId,profile.schoolId) {
    mutableStateOf<Set<String>>(emptySet())
  }
  var progressError by remember(profile.userId) { mutableStateOf<String?>(null) }
  var syncMessage by remember(profile.userId) { mutableStateOf<String?>(null) }
  var syncing by remember(profile.userId) { mutableStateOf(false) }
  fun syncNow() {
    if(profile.role!="student" || syncing)return
    if(cloud==null){
      syncMessage="School sync is not configured. Offline lessons are still available."
      return
    }
    syncing=true
    scope.launch {
      runCatching { cloud.synchronize(profile,vault) }.fold(
        onSuccess={result->
          completed=vault.completed(profile.userId,profile.schoolId)
          syncMessage="School synced: ${result.total} lesson completions."
          progressError=null
        },
        onFailure={
          syncMessage="School sync unavailable. Completed lessons remain encrypted on this device."
        }
      )
      syncing=false
    }
  }
  LaunchedEffect(profile.userId,profile.schoolId) {
    runCatching {
      withContext(Dispatchers.IO) {vault.completed(profile.userId,profile.schoolId)}
    }.fold(
      onSuccess={local->
        completed=local
        if(profile.role=="student" && cloud!=null) {
          syncing=true
          runCatching {cloud.synchronize(profile,vault)}.fold(
            onSuccess={result->
              completed=vault.completed(profile.userId,profile.schoolId)
              syncMessage="School synced: ${result.total} lesson completions."
            },
            onFailure={
              syncMessage="Offline: local lesson progress is safe. Tap Sync when connected."
            }
          )
          syncing=false
        }
      },
      onFailure={progressError="Saved progress could not be read securely."}
    )
  }
  fun recordLesson(id:String) {
    scope.launch {
      runCatching {
        withContext(Dispatchers.IO) {
          vault.markCompleted(profile.userId,profile.schoolId,id)
        }
      }.fold(
        onSuccess={
          completed=it
          progressError=null
          if(profile.role=="student")syncNow()
        },
        onFailure={progressError="Could not save progress on this device."}
      )
    }
  }
  var query by rememberSaveable { mutableStateOf("") }
  var grade by rememberSaveable { mutableStateOf("All") }
  var opened by rememberSaveable { mutableStateOf<String?>(null) }
  val levels=remember { listOf("All")+
    allLessons.map { it.level }.distinct() }
  val results=remember(query,grade) {
    allLessons.filter { entry->
      (grade=="All" || entry.level==grade) &&
        (query.isBlank() || listOf(entry.title,entry.subject,entry.level).any {
          it.contains(query.trim(),ignoreCase=true)
        })
    }
  }
  val lesson=remember(opened) { allLessons.find { it.id==opened } }
  BackHandler(enabled=lesson!=null){opened=null}
  LazyColumn(
    modifier=modifier.fillMaxSize().testTag("learning-library-list"),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(13.dp)
  ) {
    if(lesson!=null) {
      item(key="return"){TextButton(onClick={opened=null}){Text("‹  All lessons")}}
      item(key="lesson-title"){
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
          Eyebrow("${lesson.level} · ${lesson.subject}")
          Text(lesson.title,style=MaterialTheme.typography.headlineMedium)
          StateNotice("Learning goal",lesson.objective)
        }
      }
      item(key="read-header"){
        if(lesson.id in completed) {
          StateNotice("Lesson completed",
            "Correct self-check saved locally; school progress syncs when online.")
        }
        SectionTitle("Read & understand")
      }
      items(lesson.explanation.size,key={ "paragraph-$it" }){index->
        AppPanel(Modifier.fillMaxWidth()) {
          Text(lesson.explanation[index],style=MaterialTheme.typography.bodyLarge)
        }
      }
      item(key="worked-example"){
        AppPanel(Modifier.fillMaxWidth()) {
          Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
            Eyebrow("WORKED EXAMPLE")
            Text(lesson.workedExample,style=MaterialTheme.typography.bodyLarge)
          }
        }
      }
      item(key="quiz"){
        QuizPanel(lesson,onCorrect={recordLesson(it)})
      }
      if(lesson.exercises.isNotEmpty()) {
        item(key="practice-exercises") {
          var showAnswers by rememberSaveable(lesson.id) { mutableStateOf(false) }
          AppPanel(Modifier.fillMaxWidth().testTag("lesson-exercises")) {
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
              Eyebrow("PRACTISE WHAT YOU LEARNED")
              SectionTitle("Three practice exercises")
              lesson.exercises.forEachIndexed {index,exercise->
                Text("${index+1}. ${exercise.question}",
                  style=MaterialTheme.typography.bodyLarge)
                if(showAnswers) {
                  Text("Suggested answer: ${exercise.answer}",
                    color=MaterialTheme.colorScheme.primary,
                    style=MaterialTheme.typography.bodyMedium)
                }
              }
              OutlinedButton(onClick={showAnswers=!showAnswers},
                modifier=Modifier.fillMaxWidth()) {
                Text(if(showAnswers) "Hide suggested answers" else "Reveal suggested answers")
              }
            }
          }
        }
      }
      if(lesson.assignmentInstructions.isNotBlank()) {
        item(key="class-assignment") {
          AppPanel(Modifier.fillMaxWidth().testTag("lesson-assignment")) {
            Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
              Eyebrow("CLASS ASSIGNMENT")
              Text(lesson.assignmentInstructions,
                style=MaterialTheme.typography.bodyLarge)
              StateNotice("Teacher publication",
                "This is an offline practice template. Teachers publish graded coursework to the class from the web LMS.")
            }
          }
        }
      }
      item(key="disclaimer"){
        Text("Original ZimSchool learning material · Not an official ZIMSEC textbook",
          style=MaterialTheme.typography.bodySmall,
          color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      item(key="library-heading") {
        Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
          Eyebrow("OFFLINE LEARNING")
          Text("Learning library",
            modifier=Modifier.testTag("learning-library-heading"),
            style=MaterialTheme.typography.headlineMedium)
          Text("Keep learning wherever you are.",
            style=MaterialTheme.typography.bodyLarge,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      if(catalogResult.isFailure) item(key="curriculum-status") {
        StateNotice("Curriculum unavailable",
          "The added curriculum could not be loaded on this device. Original offline starter lessons remain accessible.",
          MaterialTheme.colorScheme.error)
      }
      item(key="library-feature") {
        AppPanel(Modifier.fillMaxWidth()){
          Row(horizontalArrangement=Arrangement.spacedBy(14.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.tertiaryContainer),
              contentAlignment=Alignment.Center) {
              WorkspaceIcon(WorkspaceTab.LEARNING,
                modifier=Modifier.size(27.dp),
                color=MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Column {
              Text("${allLessons.size} ready-to-read lessons",
                style=MaterialTheme.typography.titleMedium)
              Text("Explanations, examples and practice questions. " +
                "All available without internet.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
      if(profile.role=="student") item(key="school-sync") {
        AppPanel(Modifier.fillMaxWidth().testTag("lesson-cloud-sync")) {
          Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Learning progress across devices",
              style=MaterialTheme.typography.titleSmall)
            Text(syncMessage?:"Correct self-checks are saved on this device. Connect to sync to your school account.",
              style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick={syncNow()},enabled=!syncing,
              modifier=Modifier.fillMaxWidth()) {
              Text(if(syncing)"Syncing with school…" else "Sync progress now")
            }
          }
        }
      }
      item(key="search") {
        OutlinedTextField(
          value=query,onValueChange={query=it.take(100)},
          modifier=Modifier.fillMaxWidth(),
          label={Text("Search lessons, subjects or topics")},
          placeholder={Text("Try Mathematics or Biology")},
          singleLine=true,
          shape=RoundedCornerShape(15.dp)
        )
      }
      item(key="filters") {
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
          items(levels,key={it}){level->
            FilterChip(selected=grade==level,onClick={grade=level},
              label={Text(if(level=="All") "All levels" else level)})
          }
        }
      }
      item(key="count") {
        Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
          SectionTitle("Browse lessons",
            "${results.size} resources • ${completed.size} of ${allLessons.size} completed")
          progressError?.let {StateNotice("Progress status",it,
            MaterialTheme.colorScheme.error)}
        }
      }
      if(results.isEmpty()) item(key="empty"){StateNotice("No matching lessons",
        "Try a different grade or subject.")}
      items(results,key={it.id},contentType={"lesson"}){item->
        AppPanel(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
          .clickable { opened=item.id }) {
          Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Eyebrow("${item.level} • ${item.subject}")
            Text(item.title,style=MaterialTheme.typography.titleMedium)
            Text(item.objective,style=MaterialTheme.typography.bodyMedium,
              color=MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(if(item.id in completed) "✓ Completed · Read again  ›"
            else "Start lesson  ›",style=MaterialTheme.typography.labelLarge,
              color=MaterialTheme.colorScheme.primary)
          }
        }
      }
      item(key="copyright"){
        Text("Original ZimSchool curriculum-aligned material. Staff can publish matching class assignments in the web LMS. No commercial workbook pages are bundled.",
          style=MaterialTheme.typography.bodySmall,
          color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
private fun QuizPanel(lesson: StarterLesson,onCorrect:(String)->Unit) {
  var answer by rememberSaveable(lesson.id) { mutableIntStateOf(-1) }
  var revealed by rememberSaveable(lesson.id) { mutableStateOf(false) }
  AppPanel(Modifier.fillMaxWidth()) {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
      Eyebrow("CHECK YOUR UNDERSTANDING")
      Text(lesson.question,style=MaterialTheme.typography.titleMedium)
      lesson.choices.forEachIndexed { index,choice->
        Surface(
          modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
            .clickable { answer=index; revealed=false },
          shape=RoundedCornerShape(13.dp),
          color=if(answer==index) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.55f)
        ) {
          Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(25.dp).clip(CircleShape)
              .background(if(answer==index) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.outlineVariant),
              contentAlignment=Alignment.Center) {
              Text(('A'+index).toString(),
                color=if(answer==index) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                style=MaterialTheme.typography.labelSmall)
            }
            Text(choice,style=MaterialTheme.typography.bodyMedium)
          }
        }
      }
      Button(onClick={
        revealed=true
        if(answer==lesson.correctIndex) onCorrect(lesson.id)
      },enabled=answer>=0,
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(13.dp)) {
        Text("Check answer")
      }
      if(revealed) {
        StateNotice(
          if(answer==lesson.correctIndex) "Correct! Well done." else "Keep practising",
          lesson.feedback,
          if(answer==lesson.correctIndex) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.tertiary
        )
        OutlinedButton(onClick={answer=-1;revealed=false},
          modifier=Modifier.fillMaxWidth()) { Text("Try another time") }
      }
    }
  }
}
