package zw.zimlms.learning

import android.content.Context
import org.json.JSONObject

/**
 * Loads the original 2024–2030 HBC curriculum catalog from the shared
 * repository asset. The same catalog powers the web LMS. No downloaded,
 * copyrighted textbook pages or learner records are stored here.
 */
object HeritageCurriculum {
  fun load(context: Context): List<StarterLesson> {
    val json = context.assets.open("catalog.json").bufferedReader(Charsets.UTF_8)
      .use { it.readText() }
    val root = JSONObject(json)
    val units = root.getJSONArray("units")
    val lessons = ArrayList<StarterLesson>(units.length())
    for (i in 0 until units.length()) {
      val item = units.getJSONObject(i)
      val text = item.getJSONArray("explanation")
      val explanation = (0 until text.length()).map { text.getString(it) }.toMutableList()
      val practical = item.optString("mediaActivity")
      if (practical.isNotBlank()) explanation.add("Practical activity: $practical")
      val choices = item.getJSONArray("choices")
      lessons.add(StarterLesson(
        id = item.getString("id"),
        level = item.getString("level"),
        subject = item.getString("subject"),
        title = item.getString("title"),
        objective = item.getString("objective"),
        explanation = explanation,
        workedExample = item.getString("workedExample"),
        question = item.getString("question"),
        choices = (0 until choices.length()).map { choices.getString(it) },
        correctIndex = item.getInt("correctIndex"),
        feedback = item.getString("feedback")
      ))
    }
    require(lessons.isNotEmpty()) { "HBC curriculum asset is empty" }
    require(lessons.map { it.id }.distinct().size == lessons.size) {
      "HBC curriculum contains duplicate lesson IDs"
    }
    return lessons
  }
}
