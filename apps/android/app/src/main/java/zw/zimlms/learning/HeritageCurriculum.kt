package zw.zimlms.learning

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * One shared 2024–2030 original curriculum catalogue for web and Android.
 * The build packages JSON from ../../../packages/curriculum as offline assets.
 * No commercial workbook pages, student profiles or private school data are included.
 */
object HeritageCurriculum {
  private fun readObject(context:Context, filename:String): JSONObject =
    JSONObject(context.assets.open(filename).bufferedReader(Charsets.UTF_8).use { it.readText() })

  private fun pairs(json:JSONArray): List<Pair<String,String>> =
    (0 until json.length()).map { i ->
      val values=json.getJSONArray(i)
      values.getString(0) to values.getString(1)
    }

  private fun workshop(item: JSONObject): LessonDeepDive {
    val guide=item.getJSONArray("guided")
    val stepData=item.getJSONArray("steps")
    val done=item.getJSONArray("recap")
    val worked=item.getJSONArray("worked")
    return LessonDeepDive(
      starter=item.getString("starter"),
      vocabulary=pairs(item.getJSONArray("vocabulary")),
      steps=pairs(stepData),
      worked=(0 until worked.length()).map(worked::getString),
      guided=(0 until guide.length()).map { i ->
        val row=guide.getJSONArray(i)
        Triple(row.getString(0),row.getString(1),row.getString(2))
      },
      practice=pairs(item.getJSONArray("practice")),
      misconception=item.getString("misconception"),
      application=item.getString("application"),
      recap=(0 until done.length()).map(done::getString),
    )
  }

  fun load(context: Context): List<StarterLesson> {
    val packNames=listOf(
      "catalog.json",
      "extension.json",
      "source-guided-a.json",
      "source-guided-b.json"
    )
    val deepUnits=readObject(context,"deep-dive.json").getJSONArray("units")
    val deepById=(0 until deepUnits.length()).associate { i ->
      val item=deepUnits.getJSONObject(i)
      item.getString("id") to workshop(item)
    }
    val lessons=ArrayList<StarterLesson>(96)
    for(filename in packNames) {
      val units=readObject(context,filename).getJSONArray("units")
      for(i in 0 until units.length()) {
        val item=units.getJSONObject(i)
        val desc=item.getJSONArray("explanation")
        val explanation=(0 until desc.length()).map(desc::getString).toMutableList()
        val practical=item.optString("mediaActivity")
        if(practical.isNotBlank()) explanation.add("Practical activity: $practical")
        val choices=item.getJSONArray("choices")
        val exercises=item.getJSONArray("exercises")
        lessons.add(StarterLesson(
          id=item.getString("id"),
          level=item.getString("level"),
          subject=item.getString("subject"),
          title=item.getString("title"),
          objective=item.getString("objective"),
          explanation=explanation,
          workedExample=item.getString("workedExample"),
          question=item.getString("question"),
          choices=(0 until choices.length()).map(choices::getString),
          correctIndex=item.getInt("correctIndex"),
          feedback=item.getString("feedback"),
          exercises=(0 until exercises.length()).map { n ->
            val exercise=exercises.getJSONObject(n)
            LessonExercise(exercise.getString("question"),exercise.getString("answer"))
          },
          assignmentInstructions=item.getJSONObject("assignment").getString("instructions"),
          syllabusSource=item.optString("syllabusSource"),
          practicalActivity=practical,
          textbookReference=item.optString("textbookReference"),
          deepDive=deepById[item.getString("id")],
        ))
      }
    }
    require(lessons.size==88) { "Expected 88 verified shared curriculum units, got " + lessons.size }
    require(lessons.map { it.id }.distinct().size==lessons.size) {
      "HBC curriculum contains duplicate lesson IDs"
    }
    require(deepById.size==13 && lessons.count { it.deepDive!=null }==deepById.size) {
      "Missing or mismatched deep-dive workshops"
    }
    return lessons
  }
}
