# Phase 20 — One-lesson, role-first LMS interface

This development UI is deliberately designed for Zimbabwe's Group C/D schools, including low-bandwidth connections, shared devices and staff/parents with limited computer experience.

## Design rules
1. One homepage per authenticated designation. Maximum five task choices; no database/table names, KPI clutter, advanced filters or decorative fake links on the landing screen.
2. Large, readable plain-English touch targets. One action at a time; a consistent "Home" path. The same concept should also guide the Android first screen.
3. Do not create new privileges by hiding or showing buttons. Underlying screens and Supabase RLS still enforce permissions. Unsupported features are not presented as operational.
4. Keep menus light: links are loaded only after an individual group is opened. No background requests for every page.
5. Never claim offline school records sync, paid fees or messaging when those capabilities are not verified.

## Role map (web)
- Learner: My Learning, My Results, School News & Events, My School Life, My Future.
- Teacher: My Classroom, Plan & Teach, Mark Work, School News & Events.
- Parent: My Child, Support Learning, School News & Events, School Fees.
- School administrator: People & Classes, Teaching & Learning, School News & Events, School Overview, School Fees.
- Head: similar oversight but no member-invitation privilege.
- Bursar: Fee Invoices and School News; Clerk: School News & Events.

## School life, communication and safeguarding
- School Life displays approved, published sports/cultural/meeting events for the signed-in school only.
- Personal club membership cannot be reliably derived from events, so it is explicitly **not yet available**. Do not invent membership counts.
- Student discussions remain **inactive** until staff moderation, identity checks, reporting, retention, safeguarding review, abuse controls and parent/guardian consent policies are implemented and tested. In particular, do not enable unsupervised student-to-student direct messages.
- My Future provides original exploratory prompts and practical small activities, not personalized career recommendations or unverified qualifications.

## Acceptance gates
- Automated role-map tests must prove five-or-fewer homepage choices, hidden privileged links, unknown-role denial and correct link authorization.
- Web type-check/unit/production/demo smoke must pass on this exact commit.
- QA must perform 320px phone, touch, browser back, keyboard, 200% text and low-bandwidth checks with real consented accounts.
- Roll out the same information architecture on Android after automated emulator checks. This phase does not supersede the final release gate.
