# Phase 12 — Android cloud teacher plans and secure invitation acceptance

Status: development preview on draft PR #10. Not a production release.

## Android v0.14

- School-designated teachers can create and revise private lesson plans on Android while online.
- Class, subject and source curriculum lesson are chosen from the JWT-scoped school directory, with the same grade and subject name aliases as the web app. Only assigned teaching pairs are offered; Supabase policies still independently enforce all writes.
- Lesson title, date, objectives, teacher/learner activities, materials, assessment, reflection and status (`draft`/`ready`) are supported.
- Plans are loaded directly from `public.teacher_lesson_plans` with the current encrypted-session access token and are held only in memory. No plaintext offline plan cache, automatic submission, or optimistic "saved" state. No local-only unsynced plans are invented.
- Head and administrator roles can read school plans; only an assigned, active teacher can create/revise plans. Students, parents and unrelated teachers cannot read private teaching preparations, including through direct Data API requests.
- The 75 starter/curriculum teaching guides remain available offline to staff even when the private cloud plan service cannot be reached.
- The Android debug build has unique package suffix `zw.zimlms.preview.plans14` to avoid overwriting previous v0.12/v0.13 local progress. Previous offline data is **not** automatically imported; sync v0.12 student lesson completions before changing installations.
- These edits are online-only. Cross-device editing must be verified on real teacher accounts before production release.
- Unit tests in `TeacherPlanEligibilityTest.kt` cover grade/subject matching, subject isolation, homeroom permissions, other roles and date/field validation. The Android CI workflow also builds an installable debug APK and runs API 26/29 emulator checks.

## Invitation acceptance

- New invitation recipients can open `/accept-invite` and set their own password. The web page handles Supabase's default invitation URL fragment, PKCE code and `token_hash/type=invite` flows. It removes consumed invitation credentials from the URL, verifies the authenticated user against the active school profile, and checks password confirmation and strength.
- Public self-registration remains disabled. School roles are not read from user-editable metadata or form submissions.
- The Edge Function invitation source requires a deployment-specified `ZIMSCHOOL_WEB_ORIGIN` (single trusted HTTPS **origin only**). It constructs `<origin>/accept-invite` and passes it as `redirectTo` to `inviteUserByEmail`. Client-supplied return URLs are never accepted. Without a configured verified origin, the updated function fails closed **before sending any invitation**.
- Configure `ZIMSCHOOL_WEB_ORIGIN` in Supabase Edge Function secrets, set Auth Site URL and redirect allow-list to the same actual deployed HTTPS origin and `/accept-invite`, confirm outgoing SMTP delivery and perform a real teacher/student/parent invitation, password setup and sign-in test. These settings are **not automatically configured by this commit**.
- The existing hosted `invite-school-member` function remains on its earlier deployment until there is a confirmed web origin to configure and approve; do not claim end-to-end invitations are ready.
- Supabase invitation confirmation may open a temporary authenticated session before the password has been chosen. Do not treat password setup alone as a replacement for admission checks, account verification, MFA, short JWT expiration or privacy review. Current account authorization still uses live RLS and active profiles.

## Verification and release boundaries

- Phase 11 security migration is deployed and tested. Android and web code must each pass CI for the latest Phase 12 commit before distributing the corresponding APK.
- Build/CI statuses must be checked against the *specific latest commit*. Vercel preview workflow may succeed while actual deployment steps are skipped without Vercel credentials.
- Two additional active user accounts still require real name, email, Zimbabwe mobile number and role, and recipients' informed consent as relevant. Do not activate inactive fictional accounts or invent account credentials.
- A signed distributable Android APK, genuine web deployment, SMTP verification, family/teacher phone testing and Supabase leaked-password protection still block release.
