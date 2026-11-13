# Phase 15 — Android classroom assessment recording

Android v0.17 adds staff assessment creation and subject-scoped marks entry. A teacher may select only the classes and subjects returned by their JWT-scoped directory assignment; a homeroom teacher may prepare all subjects for that class. Active school administrators/head teachers can see all school pairs. All writes are independently authorised on the server by existing `app_private.can_manage_assessment`-backed RLS.

The Android client creates `assessments` and records or corrects `assessment_results` online through PostgREST with the signed-in JWT. It never embeds a service-role key, and does not cache grades, guardian details, class rosters or exam text to plaintext disk. Marks are validated with exact decimal arithmetic and rejected if higher than the live assessment maximum; the response must confirm one changed row. Updates modify only score, feedback, marker and timestamp. Protected IDs remain immutable.

Staff see up to 100 most recent accessible assessments and 100 eligible active learners per class in one mobile page. For larger schools, use the web administrator portal. Students and linked parents continue to read their authorised assessment results **and teacher feedback** only, with no access to staff editing tools.

Local CI tests cover role/subject filtering, invalid dates and precise score constraints. Local JWT integration tests independently exercise subject separation, overmaximum rejection, learner/parent privacy, grade correction, and cross-school denial with synthetic records that are removed. No production accounts or marks have been created.

Production release still requires verified public web deployment, invite URL and email delivery, signed APK, physical school-device tests, and Supabase Auth leaked-password protection.
