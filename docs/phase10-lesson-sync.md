# Phase 10 — Learner lesson completion synchronisation

Date: 10 November 2026  
Status: development preview, not a school production release  
Android: v0.12 lesson-sync preview (separate debug package)

## What Phase 10 does

- Android keeps lesson completion inside its existing AES-GCM, Android Keystore-backed local vault, scoped separately to `school_id` and `user_id`.
- **Bug fixed:** the old vault only accepted the original **12 starter lessons** even after 63 curriculum lessons were added. The vault now accepts all 75 bundled lesson IDs and can merge remote completions without losing offline ones.
- An authenticated **student** can sync completed quiz/self-check lessons to Supabase while online. On opening the Learning Library, after completing a self-check, and on tapping **Sync progress now**, the app downloads permitted remote IDs, merges them with the encrypted local set, and uploads missing IDs. Server-approved duplicates are harmless (HTTP 409 unique violation).
- If the connection, session or upload fails, locally stored progress remains and the UI reports that sync has not finished. Upload status is not falsely marked successful.
- The web curriculum quiz records a completion **only after the learner chooses the correct answer**. Parents and school staff can preview lesson quizzes but do not generate student completion records.
- The learner/guardian/staff **Learning Progress** web dashboard shows recorded self-check lessons separately from marked coursework and formal assessments.
- Progress sync is **not a grade**, a verified classroom assessment or proof of lesson mastery. A user can inspect a self-check's answers on the device; formal marks still require authorised teacher assessment.

## Authoritative data and access rules

- `public.lesson_catalog` contains 75 versioned, unique lesson IDs (12 starters plus 63 curriculum units). No student-submitted IDs outside this catalogue are accepted.
- `public.lesson_completions` primary key: `(school_id, student_user_id, lesson_id)`; completion time is server-set.
- RLS: active enrolled students may INSERT only their own completion in their own school; linked guardians, assigned staff and the learner may SELECT their authorised records. No authenticated UPDATE or DELETE permissions. Other-school users and unlinked guardians see none. No `service_role` credential in web or Android.
- The migration is `20261110150122_phase10_lesson_completion_sync.sql`, committed from the hosted project under `supabase/migrations`.
- Hosted role-permission tests ran inside a transaction and were **rolled back**; they do not create classroom activity records.
- pgTAP: `supabase/tests/phase10_lesson_completion_sync.test.sql`. Local GoTrue JWT integration extension: `scripts/test-academic-auth-e2e.mjs`.
- Android regression: `LessonProgressVaultInstrumentedTest` validates saving new curriculum IDs, encrypted storage, merge idempotence, per-user isolation, rejection of unknown lessons and tamper failure; `LessonSyncPlannerTest` validates convergent union logic.

## Scope and limitations

- This is **monotonic, add-only formative progress**. Deletion and regrading workflows are intentionally out of scope. Administrators must have a governed correction/erasure design before processing personal data subject to deletion rights.
- The app does not upload unsent coursework text or student grades while offline. It uploads only completed public lesson IDs tied to the signed-in school account.
- Android v0.12 is separately installed because the continuous-integration debug signing key is ephemeral; **device-local v0.11 progress does not automatically migrate into v0.12**. Existing local data in the older app is not copied. After first v0.12 sign-in, new completions will sync across later connected devices using the same account.
- No automatic background worker or guaranteed 72-hour two-device convergence is claimed; sync occurs when the Learning Library opens, after finishing a quiz, or when the student taps Sync.
- Server time is authoritative; the app does not upload locally fabricated completion timestamps.
- Future curriculum lessons require adding approved IDs via a versioned database migration. Unknown IDs fail closed and existing local progress remains.
- The `/dashboard/progress` page still caps results at 500 records per source; larger schools need school-scoped pagination/aggregation before the dashboard is used for official reporting.
- The user-supplied syllabi informed lesson topics, but neither textbook images nor copyrighted publisher pages are stored in the completion ledger.

## Acceptance before production

1. Test actual Android v0.12 APK on two physical devices: finish an offline lesson on device A; reconnect and sync; sign in with the same enrolled learner on device B; open Learning Library, confirm it imports the lesson.
2. Attempt the same using an unrelated school, unlinked guardian and teacher; verify they cannot impersonate a learner or insert a completion.
3. Retry sync after intermittent network loss, revoked credentials and a duplicate replay. Observe that encrypted local progress survives.
4. Check that the web learning dashboard shows the new lesson only after confirmed sync. Verify formal assessment marks do not change when a self-check is completed.
5. Complete owner-signed Android release, 72-hour offline acceptance, Vercel production deployment, privacy/consent and backup/restore testing. Do not merge the development PR before these gates pass.
