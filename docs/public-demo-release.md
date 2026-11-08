# ZimSchool LMS — Fictional web preview

This release publishes a **read-only fictional demonstration** of the school interface. It does not release the experimental Phase 1 sync engine or expose protected school records.

## Public routes
- `/` redirects to `/demo`
- `/demo` displays fictional school/class/subject/event examples
- `/login` is the real Supabase login screen; it requires an authorised, provisioned account
- `/dashboard` remains authenticated and school-scoped

## Safe dataset
LMS Omni contains 2 schools explicitly flagged `is_demo=true`, 4 academic years, 12 terms, 8 classes, 13 subjects and 6 feature flags. **No real student or staff identity data** were entered. Demo presentation values are static so no unauthenticated read policies are required.

## Vercel integration
If the repository is imported as a Vercel project, set **Root Directory = apps/web**, Framework **Next.js**, Production Branch = `main`. The checked-in `apps/web/vercel.json` contains only the public project URL and the public Supabase publishable key. Never add secret/service-role keys.

Signing into Vercel with GitHub alone does **not** import a repository. A verified deployment URL and success status are necessary to claim the website is live.

## Release boundaries
Phase 1 offline calendar sync remains in the unmerged `feature/phase1-offline-sync` branch (draft PR #1). Its unfinished requirements include hosted Auth, security session refresh, 72-hour two-device acceptance and sync of marks, attendance and fixtures. Do not use the public preview with real pupil data.
