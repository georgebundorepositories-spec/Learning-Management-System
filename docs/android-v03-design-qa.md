# ZimSchool Preview 0.3 — Android interface correction and QA

Date: 8 November 2026.
Branch: `feature/android-portals-learning`. PR #3 targets the draft Phase 1 sync branch, **not production main**.

## User-reported problem

The earlier Android 0.2 preview displayed modules as cramped, repetitive Compose lists/forms with calendar content dominating the page. The interface lacked a consistent information architecture and clear screen separation. This file records what has been changed in code, **not an unverified claim of visual perfection on a physical device**.

## Changes in this preview

- Full-screen `Scaffold`: stable branded top app bar, role-specific bottom navigation, separate drawer for secondary modules.
- Consistent Material3 design tokens for light and dark modes: forest/teal accents, neutral layered surfaces, accessible on-surface text, purposeful typography scale.
- Administrator home: school hero, live server-scoped metrics, useful module entry tiles; real classes, teacher and parent registers, read-only learner directory, assignment-management page, offline lesson library and calendar.
- Dedicated module screens use lazy lists and spacing instead of embedding all content in a single uncontrolled vertical column.
- Offline learning reader includes 12 original lessons, practice quizzes and encrypted local completion tracking.
- Calendar redesigned with event/pending metrics, collapsible editor, event cards, clear sync status and review/outbox feedback.
- Shared-device sign-out is available in both the drawer and home; login uses an explicit full-screen composition.
- The previous design CI exposed a small-screen navigation defect: on Android 8 the long drawer pushed sign-out beyond the viewport. Fixed by making *only the module menu* scrollable and keeping account controls pinned. The UI test now checks actual visibility and touch, not off-screen semantics alone.
- Android preview's debug application ID is `zw.zimlms.preview` and label `ZimSchool Preview`, to coexist with the earlier test app and avoid destructive uninstall. Release variant remains unsigned in CI.

## Verification

- Earlier compilation and release-size jobs passed in run #42483250056, but API 26 and 29 failed two UI assertions. Never label this run as successful.
- UI regression fix and calendar overhaul are on the new branch head. GitHub Actions run #42511778167 is the verification target for Kotlin compilation, API 26/29 emulator interactions and size. **Record results only after completion**.
- A build passing does not establish how every handset looks. Physical Android 8/2GB visual review, keyboard overlap, minimum touch-target size, font scaling, light/dark contrast and cold-start measurement are still required.

## Overall build boundaries

Implemented: authenticated school directories, classroom/subject lists, admin teaching assignments, calendar events and queued offline edits, 12 original offline lessons, progress tracking and role-specific screens.

Not implemented: active invitation/registration from Android, assessment/gradebook, attendance, homework submissions, teacher resource uploads, parent messages, fee payments and receipts, offline sync for non-calendar entities and 72-hour physical two-device acceptance. Do not add functional-looking controls without working server handlers and authorization tests.

## Installation safety

Use the debug artifact only after successful CI. The new preview installs alongside the earlier app and does not automatically migrate or erase its offline queue. An old signing-key mismatch is not a reason to wipe an installation with unsynchronised school work.
