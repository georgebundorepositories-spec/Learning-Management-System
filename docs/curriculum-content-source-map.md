# ZimSchool — Heritage-Based Curriculum Catalogue (November 2026)

## Status
- **63 original lessons**, from **ECD A through Grade 7**, spanning **English Language, Mathematics, Science and Technology, and Indigenous Languages**.
- Every lesson includes an objective, two substantive instructional paragraphs, a worked example, a 3-option check question with feedback, **three original practice exercises and a marking guide**, a **10-mark class assignment** with a 4+4+2 rubric, a practical/media suggestion, and the syllabus/topic used.
- **Web**: `/dashboard/curriculum` allows authorised accounts to browse, filter by grade/subject and open a complete lesson. Staff see answer guides and can publish a template to an appropriately configured school class, with existing coursework RLS used to authorise the publication. Learners do **not** see the staff answer guide on the web lesson page.
- **Android**: the same `packages/curriculum/catalog.json` is compiled as an asset, so the new lessons, quizzes, exercises and assignments are available **offline**. The app's original 12 starter lessons remain available.
- Assignments are **templates**, not automatically issued to every school. Teacher approval, mapped classes and active subject assignments are required before publication. Web learning material and Android offline progress are different systems; assignment submissions and grade reports remain school-scoped in Supabase.

## Primary authoritative syllabus sources (uploaded by project owner)
1. `Infant English Language Syllabus.pdf`: ECD A–Grade 2 listening, speaking, reading and writing.
2. `Infant Science and Technology Syllabus(1).pdf`: ECD A–Grade 2 health, food, plants, environmental awareness, tools and innovations.
3. `818573946-Infant-Indigenous-Language-Syllabus.pdf`: ECD A–Grade 2 Indigenous Languages, including cultural aspects and appropriate registers.
4. `Junior English Syllabus.pdf`: Grade 3–7 reading, writing, speaking and listening; `Junior English Syllabus(1).pdf` is an additional supplied version of the same syllabus.
5. `Junior mathematics syllabus.pdf`: Grade 3–7 Number, Operations, Measures and Relationships.
6. `Junior Science and Technology.pdf` and `SCIENCE AND TECHNOLOGY JUNIOR GRADE 3 - 7 HBC.pdf`: two supplied Grade 3–7 2024–2030 Science and Technology syllabus files; their eight strands include Health and Hygiene, Food and Nutrition, Crops/Plants/Animals, Environmental Conservation, Tools, Energy, Disaster Resilience and Educational Technology.
7. `Junior Indigenous Languages.pdf`: Grade 3–7 comprehension, usage, structure, composition and cultural aspects.

## Additional publisher examples consulted for topic selection (not reproduced)
- `971890994-PlusOne-HBC-Science-and-Tech-Grade-5-TG.pdf`: Grade 5 Teacher's Guide (108 PDF pages), including digestion, water pollution and solar energy.
- `981912480-Plus-One-HBC-English-Grade-4-LB.pdf`: Grade 4 English excerpt (39 PDF pages), including nouns and informal letters.
- `985872926-Compressed-Ecd-B-English-HBC-Plus-One-Work-Book-4442-PDF.pdf`: scanned ECD B English workbook (75 PDF pages), including environmental sounds, greetings, phonics and tracing.
- `675425424-ECD-B-English.pdf`: scanned ECD B English workbook (61 PDF pages), with picture-reading, families, sounds and greetings.
- `640825817-Plus-One-Maths-Grade-6-Revision.pdf`: Grade 6 Mathematics revision excerpt (35 PDF pages), covering whole numbers, fractions, ratios and related topics.
- `669763525-Science-Tech-Grade-7-textbook-sample.pdf`: Grade 7 Science and Technology sample (65 PDF pages), including human body, energy, and sustainable resource management.
- `698036265-PLUS-ONE-GRADE6-SCIE-TECH.pdf`: scanned Grade 6 Science excerpt (44 PDF pages), covering nutrition and water.
- `698036268-PLUS-ONE-GRADE6-ENGLISH.pdf`: scanned Grade 6 English excerpt (49 PDF pages), including formal letters and report writing.

The supplied publisher editions and excerpts are copyrighted; **they are not uploaded to the publicly served LMS, duplicated as lessons, or embedded in the Android APK**. No external media rights were established. The catalogue's `mediaActivity` provides a prompt for original student/teacher diagrams, safe in-class demonstrations, or recordings used only with appropriate consent. Historical syllabus topics are used as curriculum alignment, **not** represented as ministry-approved ZimSchool lesson plans.

## November 2026 extension from newly attached sources
- Added **16 original teaching units** in `packages/curriculum/extension.json`. Web and Android load this alongside the base 47-unit `catalog.json` (total **63 units**). Android bundles both files in its offline APK.
- Added ECD A listening instructions; ECD B picture vocabulary and Indigenous-language greetings; Grade 2 water/soil protection; Grade 3 emergency helpers; Grade 4 informal letters and collective nouns; Grade 5 water pollution and solar energy; Grade 6 ratios, tables, balanced diets and factual reports; Grade 7 breathing, safe waste reuse and Indigenous-language folktale summarising.
- Each unit provides objective, two original instructional explanations, worked example, 3-choice check question and feedback, three exercises with expected answers, teacher publishable 10-mark assignment and practical media suggestion.
- The supplied publishers' scans include copyright notices; **no publisher images, diagrams, scans or audio were copied into LMS assets**. Media prompts request original teacher-created illustrations or consented school recordings.
- Curriculum competency is referenced by syllabus titles but not certified as ministry-approved individual lesson plans. Verify the pedagogical fit and local language with school teachers.
- This update does not auto-assign work or alter school records. Authorised teachers must publish a template to a matched grade/class/subject and Supabase RLS remains active.

## Teacher workflow
1. Sign in with an active teacher, head or admin school account.
2. Open **Curriculum Lessons & Exercises** from the dashboard or Learning module.
3. Filter by grade and subject; select the lesson, check its learning objective, instructions, answer key, rubric and material suggestion.
4. Under **Publish to your class**, select the correct class and subject. If none is offered, configure classes and teaching assignments first.
5. Click **Publish this assignment**. It enters the usual coursework module, where learners can submit answers and authorised staff grade them. No templates auto-post into other schools.

## Release and quality gates
- Shared content shape and grade/subject checks: `apps/web/lib/curriculum/catalog.test.ts`
- Android shared-asset shape, offline availability and cross-grade coverage: `HeritageCurriculumInstrumentedTest.kt`
- Android full lesson/exercise/assignment navigation: `WorkspaceUiInstrumentedTest.kt`
- Existing authenticated coursework/RLS tests remain unchanged. Do **not** treat green CI as physical-device deployment approval. Content should be checked by practising teachers, including proper cultural and home-language adaptation.
