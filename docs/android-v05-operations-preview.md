# ZimSchool Android v0.5 operations preview — 9 November 2026

**Release boundary: TEST BUILD ONLY. No production distribution, real payments, SMS or sensitive student onboarding.**

## Changes compared with the v0.4 attendance preview

- Teachers can open **Daily attendance** from their school workspace.
- The class selector only displays a teacher's subject-assigned or homeroom classes.
- Learner rosters are fetched via `public.get_attendance_roster(p_class_id)`, whose security-invoker public wrapper delegates to a narrowly scoped internal function. The RPC enforces active-user membership and assignment; the Android client does **not** fall back to a broad student directory read.
- Attendance reads and writes retain the existing server-enforced class/school/learner validation and a successful write must be confirmed before the UI reports saved.
- **Notices** shows current in-app school announcements permitted to that user by the hosted RLS policy.
- **Fee invoices** shows the authenticated student's or linked parent's private invoice list, or authorised staff's school invoices. It cannot perform payments or produce receipts.
- These additional HTTP responses are **RAM-only** and are never cached in unencrypted Room.
- UI has dedicated notices and fee screens, refresh and network-error states and accessible navigation.
- Preview APK uses `zw.zimlms.preview.operations` (debug) and `0.5.0-operations-preview`. This avoids replacing the v0.4 local test app. Android's ephemeral CI debug signing certificate is not production signing.

## Verified backend evidence

9 November 2026: a rollback-only hosted Supabase SQL test activated one fictional teacher temporarily, confirmed that the teacher could read an assigned ECD A class roster and that an unassigned class in the same school and a cross-school class returned zero records. The transaction was rolled back.

The published fees and notices policies already passed cross-school and unrelated-learner denial canaries. No test invoices/notices were retained. These tests do not substitute for real authenticated device testing.

## Acceptance tests still required

- All Android CI jobs must pass: Gradle unit tests, debug APK assembly, API 26 and API 29 instrumented tests and optimised unsigned release size.
- A real authorised teacher must sign in on physical Android and mark/refresh their assigned-class register. Denied teacher/class access must be verified through a real JWT request.
- Test school notices and linked-parent fees on all roles, including revoked and inactive accounts.
- Complete the Phase 1 72-hour two-device mixed-offline convergence test, low-bandwidth cold-start testing, local encryption review, and final app signing.
- Check a stable install/upgrade path, backend feature parity and the final secure production deployment URL.

## Where code lives

- Android branch: `feature/android-portals-learning` (draft PR #3).
- Web Phases 2–8 branch: `feature/role-portals-foundation` (draft PR #9). The branches are not yet integrated into a production release.
- Production portal URL: **unverified**. Merely linking GitHub to Vercel does not demonstrate that the Vercel project was imported and deployed.

Never describe the current preview as a finished production APK until these gates pass.
