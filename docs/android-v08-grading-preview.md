# ZimSchool LMS — v0.8 Coursework Grading Preview

Date: 10 November 2026  
Branch: `integration/zimschool-web-android-v05`  
Release classification: **Development preview, not production**

## New workflow

A teacher assigned to a class and its subject can publish a coursework assignment and specify a maximum mark. A learner assigned to that class can submit and revise a text answer **until the answer is graded**. The assigned teacher can record or edit a numeric mark and up to 2,000 characters of feedback. Learners and their linked guardians can view the mark and feedback but cannot edit them.

- Web: `/dashboard/coursework` now offers a marking form for each eligible submission.
- Android: `Coursework` shows marks and feedback and has authenticated grading controls for authorised staff.
- All grade writes go through the signed-in session and the database's class+subject RLS policy.
- The database rejects marks exceeding an assignment's configured maximum and prevents students from changing a reviewed answer.
- Grade ownership cannot be reassigned by authenticated clients via column privileges.
- Private coursework, marks and feedback are not persisted in the Android unencrypted Room cache.

## Backend hardening and verification

Hosted Supabase migrations applied and checked into GitHub with identical migration version identifiers:

- `phase6_coursework_grading_feedback`: assignment maximum marks, independent grade/review table, student/guardian/teacher RLS and graded-answer lock.
- `phase6_review_lock_non_recursive`: internal guarded lookup to prevent recursive RLS policy evaluation.

Rollback-only Supabase tests verified:

1. A class-assigned teacher can record and update a mark.
2. A learner and their linked guardian can read the mark, with no unrelated-guardian or cross-school visibility.
3. Unassigned teachers cannot read or grade the submission.
4. Grades above the assignment maximum are rejected.
5. Learner edits succeed before grading, but fail afterward.
6. Guardian attempts to alter a score fail.
7. All fictional canary records and temporary user activation changes are rolled back.

Database regression: `supabase/tests/phase6_coursework_grading.test.sql`.  
Android decimal regression: `CourseworkMarkValidationTest.kt`.

## Production limitations

This feature is **text-only and online-only**. It is not a general offline assessment grading system. There is no secure file upload, plagiarism workflow, teacher-controlled reopening of a reviewed submission, grade release schedule, or high-volume pagination beyond the current visible window.

**Android v0.8 is an isolated debug test package**, not a stable production-signed installer. Keep the application off real students' devices until full offline convergence, real-account E2E permissions, privacy and physical-device release gates have passed.

The web portal still has no verified public Vercel deployment URL. The guarded deployment workflow requires the repository's Vercel project secrets and a project configured with `apps/web` as root. The existing signed Android candidate workflow separately requires an owner-controlled persistent signing keystore.
