package zw.zimlms.learning

/**
 * Original, self-contained sample lessons for the fictional school demo.
 * These are not official ZIMSEC publications or a substitute for approved
 * syllabuses and textbook licensing. They contain NO personal school data
 * and are safe to bundle for offline reading.
 */
data class LessonExercise(val question: String, val answer: String)

data class StarterLesson(
  val id: String,
  val level: String,
  val subject: String,
  val title: String,
  val objective: String,
  val explanation: List<String>,
  val workedExample: String,
  val question: String,
  val choices: List<String>,
  val correctIndex: Int,
  val feedback: String,
  val exercises: List<LessonExercise> = emptyList(),
  val assignmentInstructions: String = "",
  val syllabusSource: String = "",
  val practicalActivity: String = "",
) {
  init {
    require(id.isNotBlank() && level.isNotBlank() && subject.isNotBlank())
    require(choices.size == 3 && correctIndex in choices.indices)
  }
}

object StarterLessons {
  val lessons: List<StarterLesson> = listOf(
    StarterLesson(
      "ecd-count-5", "ECD A", "Mathematics", "Counting objects from one to five",
      "Count up to five objects using one-to-one correspondence.",
      listOf(
        "Place five bottle tops on a table. Point to each top once as you say one, two, three, four, five. Moving an object to the side after counting helps you avoid counting it twice.",
        "Mix the bottle tops and count again. The total is still five even when the arrangement changes. Counting tells us how many objects there are, not where they sit.",
        "Ask a friend to give you three tops. Count the tops while putting them into a small group. Repeat with two, four and five tops."
      ),
      "There are four stones: ● ● ● ●. Touch each one once and say one, two, three, four. The last number is the total.",
      "How many objects are shown? ● ● ●",
      listOf("Two", "Three", "Five"), 1,
      "Each of the three circles is counted exactly once: one, two, three."
    ),
    StarterLesson(
      "g3-multiply", "Grade 3", "Mathematics", "Multiplication as equal groups",
      "Use repeated addition to solve equal-group problems.",
      listOf(
        "Multiplication is a quicker way to add equal groups. Three groups of four are written as 3 × 4. The total is 4 + 4 + 4 = 12.",
        "Draw circles around each group of counters. Check that every group contains the same number before multiplying. If one group has a different amount, count or add the groups separately.",
        "When arranging counters into rows, two rows of five contain ten counters. Turning the arrangement around gives five rows of two, which also totals ten."
      ),
      "Four baskets each hold three mangoes. 3 + 3 + 3 + 3 = 12 mangoes, so 4 × 3 = 12.",
      "How many pencils are in 5 packets with 2 pencils each?",
      listOf("7 pencils", "10 pencils", "12 pencils"), 1,
      "Five equal groups of two are 2 + 2 + 2 + 2 + 2 = 10."
    ),
    StarterLesson(
      "g3-reading", "Grade 3", "English Language", "Understanding the main idea",
      "Identify the main idea and supporting details in a short passage.",
      listOf(
        "Read this passage: 'Rudo planted beans in a small garden. Every morning she watered the plants. After a few weeks, green shoots appeared. She felt proud of her care.'",
        "The main idea is what the passage is mostly about. Here, the passage explains how Rudo cared for her bean plants and observed them growing.",
        "Supporting details give us more information about the main idea. Watering the plants every morning and seeing new shoots are two supporting details."
      ),
      "If a paragraph tells us about children collecting litter around their school, its main idea is the school clean-up, not the colour of one child's bag.",
      "What did Rudo do every morning?",
      listOf("Water the plants", "Pick ripe oranges", "Read at the library"), 0,
      "The second sentence says that Rudo watered the plants each morning."
    ),
    StarterLesson(
      "g3-heritage", "Grade 3", "Heritage Studies", "Family heritage and community stories",
      "Describe examples of cultural heritage respectfully.",
      listOf(
        "Heritage includes the stories, languages, skills, buildings and traditions passed from one generation to another. Families may keep heritage through songs, recipes, stories, totems or special celebrations.",
        "Different families have different histories. We should ask respectful questions rather than assume that every family follows the same traditions.",
        "A learner can record a story shared by an older relative, then write down what the story teaches about their community. Always ask permission before recording or sharing someone's words."
      ),
      "A grandmother teaches a child how to weave a basket. The skill and the story of how she learned it are both parts of heritage.",
      "Which example best shows a skill passed between generations?",
      listOf("Learning basket weaving from an elder", "Buying a new football", "Checking tomorrow's weather"), 0,
      "A craft taught by an older family member is a practical example of passed-on knowledge."
    ),
    StarterLesson(
      "g5-fractions", "Grade 5", "Mathematics", "Fractions of a whole",
      "Compare and calculate simple fractions of quantities.",
      listOf(
        "A fraction names equal parts of a whole. In 3/4, the denominator 4 tells us the whole is divided into four equal parts; the numerator 3 tells us how many parts are selected.",
        "To find 1/4 of 20, divide 20 into four equal groups. Each group contains five objects, so one quarter of twenty is five.",
        "To find 3/4 of 20, first find 1/4 = 5, then take three groups of five. The answer is fifteen."
      ),
      "One-half of 18 oranges is 9 oranges, because 18 ÷ 2 = 9.",
      "What is 3/4 of 12?",
      listOf("6", "9", "12"), 1,
      "A quarter of twelve is three, and three quarters are 3 × 3 = 9."
    ),
    StarterLesson(
      "g5-science", "Grade 5", "Science and Technology", "The water cycle",
      "Explain evaporation, condensation and precipitation.",
      listOf(
        "Heat from the Sun causes some water in rivers, soil and other wet surfaces to evaporate. Evaporation changes liquid water into invisible water vapour in the air.",
        "When water vapour cools, it can condense into tiny liquid droplets. Many droplets together form clouds. When droplets become large enough, they can fall as rain, called precipitation.",
        "Rainwater can flow into streams and rivers or soak into the ground. The same water can pass through the cycle many times."
      ),
      "Water on a wet classroom floor gradually disappears as it evaporates. Cooling water vapour can later form droplets on a cold surface.",
      "Which process changes liquid water into water vapour?",
      listOf("Condensation", "Evaporation", "Freezing"), 1,
      "Evaporation is the change from liquid water to water vapour."
    ),
    StarterLesson(
      "g7-percentage", "Grade 7", "Mathematics", "Percentages in everyday life",
      "Find percentages of money and quantities.",
      listOf(
        "Percent means 'out of one hundred'. Fifty percent is the same as one-half; twenty-five percent is the same as one-quarter.",
        "To calculate 10% of a quantity, divide it by ten. To calculate 20%, find 10% and multiply that answer by two.",
        "Percentages are used for discounts, attendance and examination results. Always identify the original amount before applying the percentage."
      ),
      "A bag costs $20. A 10% discount is $2, so the reduced price is $18.",
      "What is 25% of 80?",
      listOf("10", "20", "25"), 1,
      "Twenty-five percent is one-quarter. 80 ÷ 4 = 20."
    ),
    StarterLesson(
      "g7-environment", "Grade 7", "Science and Technology", "Protecting water sources",
      "Explain ways communities can reduce water pollution.",
      listOf(
        "Clean water is important for drinking, farming and hygiene. Water can become unsafe when rubbish, sewage or harmful chemicals enter rivers and wells.",
        "Preventing pollution means keeping litter out of waterways, protecting well openings, and disposing of waste carefully. Boiling water can kill many germs, but it does not remove every chemical contaminant.",
        "If water may be unsafe, use the locally recommended treatment method and guidance from responsible health or water authorities. Do not assume that clear-looking water is safe."
      ),
      "A community places covered bins away from a stream and repairs a leaking sanitation pipe. Both actions can help reduce water pollution.",
      "Which action helps protect a water source?",
      listOf("Throwing plastic into a river", "Covering wells and disposing of rubbish safely", "Pouring paint into a drain"), 1,
      "Keeping pollution out of wells and rivers helps protect water quality."
    ),
    StarterLesson(
      "f1-algebra", "Form 1", "Mathematics", "Solving simple equations",
      "Find unknown numbers in one-step equations.",
      listOf(
        "A letter such as x can stand for an unknown number. An equation states that two expressions have the same value.",
        "To keep an equation balanced, perform the same operation on both sides. If x + 5 = 12, subtract five from each side to get x = 7.",
        "Check an answer by substituting it into the original equation. If x = 7, then 7 + 5 = 12, so the equation is satisfied."
      ),
      "For x − 4 = 9, add four to both sides. The solution is x = 13.",
      "Solve x + 6 = 15.",
      listOf("9", "21", "6"), 0,
      "Subtract six from fifteen to obtain x = 9."
    ),
    StarterLesson(
      "f2-science", "Form 2", "Integrated Science", "Density of materials",
      "Calculate density from a material's mass and volume.",
      listOf(
        "Density tells us how much mass is packed into each unit of volume. Its formula is density = mass ÷ volume.",
        "Common classroom units are grams per cubic centimetre (g/cm³). Use consistent units when working out a result, and label the answer clearly.",
        "Two objects of the same size can have different masses because they contain different materials. A denser material has more mass in the same volume."
      ),
      "A small stone has mass 40 g and volume 20 cm³. Its density is 40 ÷ 20 = 2 g/cm³.",
      "A block has mass 60 g and volume 30 cm³. What is its density?",
      listOf("2 g/cm³", "30 g/cm³", "90 g/cm³"), 0,
      "Density is mass divided by volume: 60 ÷ 30 = 2 g/cm³."
    ),
    StarterLesson(
      "f3-biology", "Form 3", "Biology", "Cells as units of life",
      "Identify basic cell structures and their functions.",
      listOf(
        "Living organisms are made of cells. Cells have structures that help them perform life processes, although their shapes and specialised features vary.",
        "The cell membrane controls movement of substances into and out of many cells. The cytoplasm contains the material in which many cell reactions occur. The nucleus of a typical plant or animal cell contains genetic information.",
        "Many plant cells also have a cellulose cell wall and chloroplasts. Chloroplasts contain chlorophyll used to capture light energy for photosynthesis."
      ),
      "A typical leaf cell can contain chloroplasts, while a typical animal cheek cell does not.",
      "Which structure contains chlorophyll in a green plant cell?",
      listOf("Chloroplast", "Nucleus", "Cell membrane"), 0,
      "Chloroplasts contain the pigment chlorophyll."
    ),
    StarterLesson(
      "f4-probability", "Form 4", "Mathematics", "Basic probability",
      "Express simple chances as fractions.",
      listOf(
        "Probability describes how likely an outcome is. A probability of zero means impossible; a probability of one means certain.",
        "When equally likely outcomes are known, probability = number of favourable outcomes ÷ total possible outcomes.",
        "For a fair six-sided die, the probability of rolling a four is 1/6 because exactly one of the six equally likely faces shows four."
      ),
      "A bag contains three blue counters and two red counters. The probability of choosing blue at random is 3/5.",
      "A fair coin is tossed once. What is the probability of heads?",
      listOf("1/4", "1/2", "1"), 1,
      "A fair coin has two equally likely outcomes, heads and tails, so P(heads) = 1/2."
    )
  )

  init {
    require(lessons.map { it.id }.distinct().size == lessons.size)
  }
}
