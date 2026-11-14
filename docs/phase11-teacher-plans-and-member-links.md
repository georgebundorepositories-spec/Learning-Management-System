# Phase 11 — Private teacher plans and school membership controls

Development status: Draft pull request #10. Not a production deployment.

## Changes
- `/dashboard/planning`: teachers can save and revise lesson plans against an assigned class, subject, and a real 2024–2030 heritage-based curriculum unit. Plan fields include date, objectives, lesson steps, teaching materials, assessment prompts and post-lesson reflection. Status `draft` or `ready` **does not publish** to learners.
- School administrators and heads may view their own school's plan records; only the plan's active teacher may create or revise them, and only while they retain the associated class/subject teaching assignment.
- `/dashboard/users`: existing admin invitation flow is extended with class enrolment for active learners, homeroom teacher assignments, linking an active guardian to an active learner, and revoking a guardian relationship. Names, contacts and accounts are **not** generated automatically.
- Existing Android v0.13 has a staff-only offline preparation *reader*. Editing and school-cloud persistence for plans are **web-only in Phase 11**; do not claim Android editor parity.

## Database access and integrity
Migration `20261110200000_phase11_teacher_lesson_plans.sql` creates `public.teacher_lesson_plans` and applies RLS. Anonymous users have no SELECT access. Teachers can only read their own current school plans. Admins and heads can read in-school plans. INSERT/UPDATE require both signed-in teacher ownership and an assigned class/subject using the existing scoped `app_private.can_manage_assessment` predicate. UPDATE column grants prevent changing owner, school, class, source unit or subject. Students, parents and teachers of unrelated classes cannot view or edit each other's plans.

The teacher lesson plan route independently checks `users.role`, `is_active`, and `school_id` server-side on every save. Validating class/subject/unit matching is server-owned and repeated in the database where appropriate; an unknown lesson ID fails the foreign key. Parent/learner link operations also validate the target school and role before writing under caller JWT and RLS. Revoking a guardian relationship removes private authorised access to that learner.

## Verification
- Hosted Supabase migration applied and independently queried: RLS enabled; anonymous SELECT denied; owner reassignment column privileges denied; public schema has zero RLS-disabled tables.
- `supabase/tests/phase11_teacher_lesson_plans.test.sql` tests privileges and policies.
- `scripts/test-academic-auth-e2e.mjs` uses six short-lived, local-only, real JWT role sessions, tests teacher-only planning, foreign subject insert denial, parent insert denial, immutable teacher ownership, legitimate owner revisions, and immediate guardian revocation. Synthetic accounts are deleted in a `finally` block. The script refuses hosted Supabase URLs.
- CI status is determined by GitHub Actions, not inferred from file existence.

## Release blockers
Do not merge draft #10 to production until approved. Two new real member invitations require each recipient's name, email, unique Zimbabwe mobile number and designation; the live project currently has one active administrator and no active teachers/students/parents. The hosted SMTP and Auth Site URL, invitation acceptance, Android plan editing parity, public Vercel deployment, owner-signed Android release, physical device multi-account tests and minors' data consent are not established. Supabase Auth leaked-password protection is currently disabled.
