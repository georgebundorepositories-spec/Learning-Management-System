# Phase 19 — Date-aware, real attendance register

**Branch:** integration/zimschool-web-android-v05 (draft PR #10, not production).

The teacher/head/admin attendance page now uses the **Africa/Harare** calendar date, rather than UTC, so forms do not silently switch the day between 22:00 and midnight UTC. A shared validated date parser is used both when viewing a register and saving marks.

A school member selects a register date once, then opens assigned classes. Learners' existing marks for that date are read back from Supabase and shown in each form. Unmarked pupils have no preselected "Present" value: staff must deliberately choose a status. Submitting an entry validates active user/class/learner and any teacher assignment on the server, uses existing insert-or-update logic and redirects back to the selected day with a confirmation. The submit button shows its actual pending state. The page distinguishes enrolled, recorded and unrecorded totals; it does not count duplicate records or wrong-class marks as enrolment completion.

Records are retrieved only from the authenticated school through Supabase RLS. Classroom view and form checks are **not** a substitute for database RLS. The page explicitly warns when queries fail or pagination safety caps are hit; it does not claim the first 1,000 records represent the whole school.

Unit tests cover Harare midnight rollover, invalid calendar dates, and cross-class or duplicate marks. Web CI must pass on the committed revision before QA.

**Not included:** offline attendance sync, bulk register editing, browser deployment, owner-signed APK, or production acceptance. The release gate and owner dependencies remain unchanged.
