# ZimSchool LMS — Android 0.3.0 design overhaul and functional LMS slice

**Branch:** `feature/android-portals-learning` → draft PR #3 based on the unfinished Phase 1 sync branch.

**Scope:** Replace the vertical-scroll prototype with a consistent, responsive school-management app shell and continue the existing school/learning workflows. This is a development build, not a school-ready production release.

## What was repaired

The old version rendered the calendar and every module inside `LoginScreen`'s single `verticalScroll` and used one horizontally scrolling strip of equally weighted filter chips to navigate seven or more destinations. It had default Material 3 typography/surfaces, inconsistent spacing, little hierarchy and no separate authenticated navigation container.

The 0.3 preview now has:
- A single full-screen authenticated `Scaffold` with a dedicated 68dp header, branded account badge, compact bottom navigation for the four high-priority destinations, and an all-modules drawer.
- One scroll container **per screen**. Registers, classes, assignments and lessons use `LazyColumn`/lazy rows with stable item keys instead of rendering every record in an enormous parent `Column`.
- A coordinated light/dark Material 3 theme with charcoal/teal/amber tokens, custom typographic hierarchy, rounded panels, consistent empty/offline states, and compact metric and module cards.
- A redesigned sign-in screen separate from the signed-in app; existing PIN, Keystore, session refresh and background sync controls remain.
- Navigation and account controls labelled for accessibility, role-specific destinations and Android back navigation returning home.
- No external web fonts or heavy icon libraries required for low-end 2GB Android hardware.

## Functional school modules

- **School overview** with real, RLS-scoped class/subject and authorised student/teacher counts.
- **Administrator registers** for learners, teachers and guardians, strictly read-only; synthetic school profiles are labelled fictional and inactive.
- **Class and subject directory**, read-only.
- **Teacher portal** with assigned classes/subjects; **parent portal** with linked learner classes; **learner portal** with own enrolled class. These remain role-limited and do not offer impersonation.
- **12 authored learning lessons**, including explanations, worked examples and interactive three-answer quizzes, bundled for offline use.
- **Device-local completion tracking** of correct quiz answers, encrypted using Android Keystore AES-GCM, bound to the user+school identity. This is not synced across devices and is not report-card grading.
- **Teacher assignment administrator screen**: creates, reassigns and removes links between existing teachers, classes, subjects and terms using the user's Supabase JWT. Writes are constrained by existing school RLS; no service role key is shipped. Changes require online server acknowledgement, and reassign/remove operations require confirmation. A hosted admin transaction checked create/reassign/remove permissions and rolled back all changes.
- Existing Phase 1 calendar module with Room queue and JWT-backed sync remains accessible for staff.

## Verification

CI must pass all four jobs (debug/unit, unsigned release size, Android 8 API 26 emulator, Android 10 API 29 emulator). New Compose UI tests cover navigation, drawer signout, and learner role restriction. Encrypted lesson progress is tested on emulators for tampering, cross-account isolation and idempotent completion.

Hosted RLS transaction check against the assigned administrator account was run with **ROLLBACK**: create, change and delete teaching assignments permitted within the assigned demo school; live dummy records were not modified.

## Limits and next gates

This is NOT yet a full teacher/learner/parent LMS. Enrolment invitations, grading, attendance, online teacher-uploaded files, PDFs/video content, course assignments/submission tracking, parent messaging, payment integration and reports remain to be implemented. Lesson progress currently stays encrypted on **this device only**.

The database directory is only loaded with a renewable JWT; it is not saved unencrypted for offline browsing. The full 72-hour physical two-device mixed-entity test and production APK signing are still open. UI texts in this preview are mainly English; complete Shona/Ndebele localisation remains a release gate.

**Safe preview installation:** This branch uses the separate debug application ID `zw.zimlms.preview` and launcher label `ZimSchool Preview`; your existing `zw.zimlms` application and its offline queue are not replaced. The preview uses its **own empty local store** and does not inherit the old app's offline events. Do not uninstall your original app while there are unsynchronised changes. Replacing *another older preview build* can still encounter a signing mismatch because GitHub's debug key may change.


## Practical quality gate

The previous release run was successful before the overhaul; the first new 0.3 UI run exposed four incorrect `Row` vertical alignment values, corrected in source. New Compose UI and lesson-progress instrumentation were added, and a clean **post-fix** workflow must pass before sharing a recommended install link.
