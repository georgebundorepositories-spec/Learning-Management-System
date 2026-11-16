# ZimSchool 2024–2030 Heritage-Based Curriculum Expansion

## Current revision (16 November 2026)

- **88 original curriculum units** in `packages/curriculum/catalog.json`, `extension.json`, `source-guided-a.json` and `source-guided-b.json`, plus the existing optional **12 Android secondary/demo starter lessons**.
- **25 new source-guided, original lessons** expand the earlier 63. These include ECD A Science, Grade 1 English, Grade 4 English, Grade 5 English, Grade 5 Mathematics, Grade 5 Shona, Grade 3/5/7 Science, with **138 new stand-alone practice items** and 25 substantial applied 20-mark assignments with staff marking guidance.
- **13 earlier deep-dive workshops** add vocabulary, three learning stages, worked reasoning, guided questions/hints, independent practice, misconception checks and reflective recap to selected existing units, in `deep-dive.json`.
- **All 88 units available to web + Android** via shared JSON imported into the Next.js app and Android assets; Android also reads the deep-dive JSON for 13 extended lessons.
- The supplied textbook PDFs were **inspected for unit/topic selection and teaching progression only**; no commercially copyrighted passages, exercise sequences, copyrighted images or chapters are republished. Treat textbook reference labels as *inspiration for topical coverage*, not a claim of licensing or verbatim matching.
- Online learners see exercises but not the staff marking guide. Staff can publish grade/subject-matched assignments via existing authorised school controls. On Android, staff-only presentation of answer keys relies on the validated local role and is not a security boundary against somebody who extracts APK assets: do **not** distribute high-stakes exam solutions in offline assets.
- Lesson progress remains school/user-scoped. The new packs do not create school accounts, issue graded assignments automatically or bypass class/subject authorisation.

## Syllabi reviewed from the user's 16 November uploads

| School level | Source | Mapped topics |
|---|---|---|
| ECD A–Grade 2 | `818573946-Infant-Indigenous-Language-Syllabus(1).pdf` | comprehension, language usage/structures, composition and culture |
| ECD A–Grade 2 | `Infant English Language Syllabus(1).pdf` | listening, speaking, reading, writing and early phonics |
| ECD A–Grade 2 | `Infant Science and Technology Syllabus(2).pdf` | hygiene, food, plants, environment, tools, energy, safety, innovation |
| Grades 3–7 | `Junior English Syllabus(1)(1).pdf`, `Junior English Syllabus(2).pdf` | listening, speaking, reading, writing and language structures |
| Grades 3–7 | `Junior Indigenous Languages(1).pdf` | comprehension/summary, language usage/structures, composition, cultural aspects |
| Grades 3–7 | `Junior mathematics syllabus(1).pdf` | number, operations, measures, relationships |
| Grades 3–7 | `Junior Science and Technology(1).pdf`, `SCIENCE AND TECHNOLOGY JUNIOR GRADE 3 - 7 HBC(1).pdf` | eight science and technology topic strands |

## Textbook topics consulted (six user uploads)

| Book | Topic guidance | Original LMS content |
|---|---|---|
| `995201702-Plus-One-Science-Tech-ECD-a-TG(1).pdf` | handwashing, food sources, plants and gardening | three detailed ECD A Science lessons |
| `729171969-Grade-1-Ventures-English-1.pdf` | greetings, family words, early short vowels | three detailed Grade 1 English lessons |
| `981912480-Plus-One-HBC-English-Grade-4-LB(1).pdf` | verbs, letter writing, homophones | three detailed Grade 4 English lessons |
| `HBC Ventures G5 English.pdf` | similes, summaries, sentence construction | three detailed Grade 5 English lessons |
| `HBC ventures G5 maths.pdf` | whole numbers, rounding, Roman numerals, fraction and decimal operations | five detailed Grade 5 Mathematics lessons |
| `HBC Ventures G5 Shona.pdf` | understanding and summarising texts, figures of speech, letter writing | three original Grade 5 Shona lessons |

Further Grade 3, 5 and 7 Science lessons are syllabus-guided only, because no matching textbook chapter was supplied. Avoid representing those as textbook-derived.

## User experience

Web:
- School-scoped login → `/dashboard/learning` → `/dashboard/curriculum`, with grade, subject and search filters.
- A unit opens goals, four explanatory paragraphs (for source-guided packs), a worked example, self-check quiz, five or six original practice questions, an applied assignment and syllabus/textbook topic provenance.
- Teacher/admin/head see teaching answer/rubric. Learners see questions and instructions; only authorised teachers can publish class-specific coursework.

Android:
- The same source-guide packs are imported from the shared `packages/curriculum` directory as **offline** JSON assets at build time. This means a **new Android APK must be installed** to receive content changes.
- 13 extended workshops additionally show vocabulary, three stages, multi-step reasoning, guided clues and independent practice. Teachers may consult visible marking notes.
- No learner database, private course submissions or publisher scans are bundled.

## Safe release checks

1. Type-check Next.js, unit tests, build Cloudflare Worker and smoke-test public pages (no student data).
2. Run Android offline catalog emulator tests for **88 unique lessons**, Android unit tests, debug build, APK privileged-key audit and minified release size check.
3. Merge only when critical checks are green. GitHub-to-Cloudflare Workers Builds then handles a production deployment; check its actual result before saying the website is live.
4. Android production release requires **permanent owner-held signing credentials** and a distribution channel. GitHub QA APKs are debug-signed and are not a Google Play release.
5. Require qualified teacher review of local language accuracy and assessment suitability, particularly for younger learners.

The uploaded textbooks are copyrighted and stay outside public web and Android bundles.
