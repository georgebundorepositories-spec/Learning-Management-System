# Phase 9 — Private Learning Progress (10 November 2026)

## Scope
- New web route: `/dashboard/progress`, linked from the main role dashboard, learner/guardian home, learning materials and school admin reports.
- Administrator and head: enrolled learners in school classes. Teacher: only learners in classes visible to the teacher's homeroom or teaching assignments, with individual coursework/assessment records further restricted by database RLS.
- Student: own progress only. Parent: linked child/children only.
- The page is a **read-only snapshot** of *existing* persisted school database records. It introduces no parallel grade records, permission changes, public export, automatic grading or child-data tracking.
- Separate indicators: submitted/assigned coursework, reviewed coursework weighted by available marks, formal assessment marks weighted by available marks, and present/recorded attendance entries.
- Missing grades do **not** count as zeros. A percentage is shown only after at least one mark is recorded; unavailable/empty shows `Not yet marked`.
- Query caps: the most recent **500** items per relevant source; 150 classes; at most 500 roster rows. This is **not** a whole-term or audited school report. For larger schools, implement keyset pagination or database-side RLS-protected aggregation before releasing official reports.
- **No offline Android progress sync yet.** Android self-check completion stays in the encrypted device-local vault and is not counted in official school grades or web coursework.

## Source and security
- Uses existing: `classes`, `class_subject_teacher`, `student_profiles`, `student_guardian_link`, `users`, `school_assignments`, `assignment_submissions`, `assignment_reviews`, `assessments`, `assessment_results`, `attendance`.
- Session/role check uses `supabase.auth.getUser()` and active school membership. All queries run with the caller's authenticated Supabase session, never a service-role key.
- The roster is whitelisted by role before any record-fetching. Class/learner IDs are constrained using `in(...)` filters; the underlying tables' existing RLS remains the final guarantee.
- The web route uses neither client-supplied school IDs nor browser-side trusted role metadata. No school data is cached for use while offline.
- A class teacher might see a class roster while seeing only marks for specifically permitted subjects. Never interpret an absent result as a zero.

## Automated verification
- `apps/web/lib/progress/summary.test.ts`: verifies no implicit zero marks, weighted marks, class/learner filtering, and submission/attendance counters.
- Existing multi-account JWT academic integration test in `scripts/test-academic-auth-e2e.mjs` verifies related coursework and role RLS with local Supabase.
- `.github/workflows/web.yml` (or web CI) runs Vitest, TypeScript and Next production build.

## Release checklist
1. Sign in with **student**, **linked parent**, **unlinked parent**, **assigned teacher**, **teacher from another subject**, **head** and **admin** in a dedicated staging school with fictional student data.
2. Confirm each can view exactly their authorised roster and grades. Confirm an unrelated school cannot read any rows. Inspect missing-mark and partially graded behaviour.
3. Verify pagination behaviour before enrolling a school above the documented query caps.
4. Run 72-hour multi-device sync and privacy review. Do not treat a read-only web progress page as Android cross-device learning sync.
5. Obtain a verified Vercel URL, configure owner signing keys and finish production acceptance before promoting this draft branch.

## Current status
Development branch only; no new production deployment or migration is performed by this phase. The progress screen is a preview, **not** a Ministry/ZIMSEC certified report card.
