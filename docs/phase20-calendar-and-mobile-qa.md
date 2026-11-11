# Phase 20 Android QA and calendar access

The first Android emulator run found one test-only failure: the career guidance title is visible in two separate places. The corrected test asserts a unique descriptive sentence. The application built and its unit tests passed.

Android version 0.19 uses the isolated QA package zw.zimlms.preview.schoolhome19. Earlier previews use different debug signing keys; this avoids installation conflicts and protects any existing local preview data. Previous package-local offline lesson progress is not automatically transferred to this new test package.

The original calendar SELECT policy excluded learners and parents even from approved school-wide events. The new migration permits read-only same-school access to approved, published, non-deleted events of permitted visibility. Target audience role restrictions remain in force. Class-targeted custom audiences stay hidden until verified learner/class filtering exists. Admins and event-author teachers retain their original access.

The hosted school has not yet published any approved calendar events; no clubs or memberships can be asserted from event titles. Student discussions are not enabled without school moderation, reporting and minor safety consent. Run SQL tests and hosted permission checks before deploying the new policy.

The production Vercel URL, school safeguarding sign-off and owner-signed release remain blocked.
