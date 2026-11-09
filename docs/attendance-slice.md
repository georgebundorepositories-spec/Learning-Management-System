# Phase 1 attendance foundation — online-only preview

Status: **REVIEW / CI REQUIRED**. This is not a complete offline attendance module.

## What was implemented

- Android school administrator/heads menu: **Attendance**.
- Select one of the currently authenticated school's classes.
- View the current Africa/Harare school day and the enrolled roster (school-specific).
- Read existing attendance statuses for that class/day and show marked vs total.
- Submit one confirmed record per learner/date with four values: Present, Absent, Late, Excused.
- Update an existing row through PostgREST only if its UUID, student, class and date match.
- JWT-backed requests use the encrypted user session, not a service-role secret.
- A failed network/server response is shown to the user; an edit is **not** labelled saved until the returned row confirms it.
- No attendance or student directory data is written into the unencrypted Room cache. Private student names remain in RAM.
- A matching, source-controlled schema migration and pgTAP attendance RLS regression were added to avoid divergence between hosted LMS Omni and a clean Supabase installation.

## Security scope

Supabase already has `public.attendance`, a unique constraint on `(class_id, student_user_id, date)`, an enrollment/class validation trigger, and three scoped RLS policies. The client supplies `marked_by=auth.uid()`, scoped class/school UUIDs and validated status values, but **only the server policies decide whether writes are allowed**.

The preview currently permits `admin`/`head` to load the learner roster because the existing `student_profiles` RLS intentionally denies broad student identities to teachers. A teacher's general right to manage assigned-class attendance is already in server policies, but a privacy-reviewed assigned-class roster RPC and teacher UI are still needed; do not bypass the student-profile RLS to make this work.

A live, transaction-only SQL canary checked a school administrator write and cross-school read isolation with fictional users; it rolled back all writes. The hosted attendance table has **zero persisted rows**.

## Known limits

- Attendance editing is **online-only**; offline queue, server op-log idempotency, optimistic version resolution and multi-device 72-hour convergence have NOT been implemented for attendance.
- Historical-date picker, bulk mark-all, notes, exports, weekly reports, parent attendance view and student privacy controls are future work.
- No actual learner data is introduced; the existing fictional school records remain inactive.
- The attendance schema was already provisioned on hosted Supabase, but had no migration file. The new migration is a non-destructive reproduction for clean Supabase CLI tests and new deployments.
- Run `supabase test db`, Android build/unit tests and API26/API29 emulators before distributing an APK. Even when CI passes, physical-device sign-in and real server-backed UI interaction must be accepted separately.

## Follow-on scope

1. Approve assigned-class teacher roster RPC and verify own-class-only data access.
2. Add attendance server sync operations with stable client op IDs, conflict handling, compact delta pull, and encrypted local retention.
3. Extend the same authenticated sync architecture to grades and sports fixtures.
4. Test mixed attendance/grade/calendar edits on two physical devices for a real 72-hour disconnected period.
