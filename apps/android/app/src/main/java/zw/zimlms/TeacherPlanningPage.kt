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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.learning.HeritageCurriculum
import zw.zimlms.learning.StarterLessons
import zw.zimlms.ui.AppPanel
import zw.zimlms.ui.Eyebrow
import zw.zimlms.ui.StateNotice

/** Staff-only offline preparation guides. This module never publishes student work. */
@Composable
fun TeacherPlanningPage(profile:CachedProfile,modifier:Modifier=Modifier) {
  if(profile.role !in setOf("teacher","head","admin")) {
    StateNotice("Staff-only workspace","Lesson planning is not available to learner or parent accounts.")
    return
  }
  val context=LocalContext.current.applicationContext
  val lessons=remember(context) {
    StarterLessons.lessons+runCatching{HeritageCurriculum.load(context)}.getOrDefault(emptyList())
  }
  var opened by remember {mutableStateOf<String?>(null)}
  val chosen=lessons.firstOrNull{it.id==opened}
  LazyColumn(modifier=modifier.fillMaxSize().testTag("teacher-planning-screen"),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=20.dp),
    verticalArrangement=Arrangement.spacedBy(12.dp)) {
    item(key="heading") {
      Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Eyebrow("STAFF LESSON PREPARATION")
        Text("Lesson planning",style=MaterialTheme.typography.headlineMedium)
        Text("Review teaching objectives, activities and answer guides. This is a private preparation catalogue, not published learner coursework.")
      }
    }
    if(chosen!=null) {
      item(key="back"){OutlinedButton(onClick={opened=null}){Text("All lesson plans")}}
      item(key="plan"){
        AppPanel(Modifier.fillMaxWidth()) {
          Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Eyebrow("${chosen.level} · ${chosen.subject}")
            Text(chosen.title,style=MaterialTheme.typography.titleLarge)
            Text("Learning objective: ${chosen.objective}")
            Text("Teacher demonstration: ${chosen.workedExample}")
            if(chosen.practicalActivity.isNotBlank())Text("Practical activity: ${chosen.practicalActivity}")
            if(chosen.assignmentInstructions.isNotBlank())Text("Draft class task: ${chosen.assignmentInstructions}")
            chosen.exercises.forEachIndexed{index,exercise->
              Text("${index+1}. ${exercise.question} — Teacher answer: ${exercise.answer}")
            }
            Text("To issue graded coursework, sign in to the web staff portal and explicitly publish to an assigned class.")
          }
        }
      }
    }else{
      items(lessons,key={it.id}){lesson->
        AppPanel(Modifier.fillMaxWidth()) {
          Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Eyebrow("${lesson.level} · ${lesson.subject}")
            Text(lesson.title,style=MaterialTheme.typography.titleMedium)
            Text(lesson.objective,maxLines=2)
            OutlinedButton(onClick={opened=lesson.id}){Text("Prepare this lesson")}
          }
        }
      }
    }
  }
}
