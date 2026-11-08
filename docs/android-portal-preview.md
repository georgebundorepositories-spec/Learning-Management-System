# ZimSchool LMS Android 0.2.0 — portal and learning preview

Branch: `feature/android-portals-learning`, based on the unmerged Phase 1 Android sync implementation.
Release status: **DEVELOPMENT / DEMONSTRATION ONLY**, pending CI and hosted device tests.

## What the previous APK was missing

The v0.1 Android app navigated directly to a calendar-only panel after login. The absence of learner, teacher, parent and learning menus was an implementation gap, not a login permission failure. Giving a user the `admin` role does not create UI modules.

## New screens in this branch

- **School administration dashboard:** school overview; clickable menus for learner, teacher and parent registers, classes, offline learning and staff calendar.
- **Learner register:** read-only, own-school Supabase `users` records (display names and active/inactive status). Administrators can see explicitly labelled fictional learner identities from the demo school.
- **Teacher register:** read-only teacher identities and active status.
- **Parent/guardian register:** read-only guardian identities and active status.
- **Class/subject catalogue:** live school classes and subjects, scoped by Supabase RLS.
- **Teacher portal:** assigned classes and subjects from `class_subject_teacher`, with bundled learning lessons and staff calendar.
- **Learner portal:** own enrolled class plus learning library (no read access to arbitrary other learners).
- **Parent portal:** only classes of learners linked through `student_guardian_link`, without exposing unrelated learner data.
- **Offline learning library:** 12 fully authored starter lessons (ECD through Form 4). Each has an objective, explanations, a worked example, a three-choice self-check and feedback. There are search and grade filters. Bundled lessons work without an internet connection.

## What is intentionally NOT claimed or implemented

- Real student registration/invitations, parent account activation and teacher assignment CRUD in Android. Portal registers are **read-only**.
- Per-learner grades and attendance, assignments submitted and marked, report cards, fee payments, sports fixtures and messaging.
- Uploading teachers' PDFs/videos or server-managed curriculum catalogue, lesson assignment and completion tracking. Lessons are **original demonstration content**, not official ZIMSEC textbooks.
- Offline caching of private learner/guardian/staff records. The directory is fetched with a renewable authenticated JWT and kept in RAM only; no plaintext directory database is created.
- Full 72-hour two-device, mixed-entity sync or reliable offline background operation on real low-end hardware.
- A production signed APK. The CI debug signing key may differ between builds; Android may require uninstalling an older test build, which can erase locally queued events. **Never uninstall while unsynced work remains**; export/reconcile first.

## Safety and test gates

The portal menus follow the verified Supabase `public.users.role`, not a user-editable setting. The server's row-level policies (RLS) remain the authority for every directory request. This work must stay on a development branch until compilation, Android 8/10 emulator checks and fresh-device sign-in tests pass. The public fictional web demo and hosted database are not modified by this branch.

When building, GitHub Actions publishes `zimschool-phase0-debug-apk` from the `feature/android-portals-learning` run. The app label remains **ZimSchool LMS** and version code is incremented to 2 (`0.2.0-portals-preview`).
