# ZimSchool LMS

A school-management and learning system for Zimbabwean schools. **The development preview is functioning and CI-tested; the full production launch has NOT been approved.** The project includes a Next.js web portal, Kotlin/Compose Android app and a Supabase PostgreSQL backend. Public registration is deliberately disabled.

## Current verified status — 11 November 2026

- **Android v0.17.0:** Debug APK [artifact](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/43014992470/artifacts/13852491741), installable alongside older previews. It is **not** a persistently owner-signed production release.
- **Web:** Secure role-specific server pages, a read-only fictional `/demo`, `/login`, `/dashboard` and email invitation acceptance code. A public Vercel deployment URL is **not yet verified**.
- **Database:** Hosted Supabase project [ZimSchool LMS](https://supabase.com/dashboard/project/mezbtcdkmvtkjxdmmhbw), 29 public application tables, all with row-level security. At last check only **one active administrator**; no real active teachers, learners or parents have been provisioned.
- **Full-feature status:** Payments, USSD, SMS/push, all-entity offline sync, restore drills, real-device 72-hour convergence and live school onboarding are not production-verified. Calendar-only sync endpoints are deployed; private grades, attendance, invoices, family records and plans currently require an online authorisation check.
- **Supabase security advisor:** Leaked-password protection is disabled; enable in Auth settings before real user onboarding.
- **Final release gate:** [GitHub Actions](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/workflows/final-release-gate.yml) fails closed when necessary public hosting, permanent Android signing, external provider and school privacy/testing evidence are absent. A red release gate is not the same as a failing application build.

## Verified code CI

The latest Android v0.17 feature revision passed all four workflows:
- [Android build, unit tests, API 26/29 emulators and package audit](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/43014992470).
- [Supabase local migrations, pgTAP, and authenticated seven-account school permissions](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/43014992462).
- [Next.js build, tests, and fictional web-demo HTTP smoke](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/43014992653).
- [Hosted sync endpoint unauthenticated rejection](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/43014992493). This does NOT verify a hosted website.

Development code is maintained on `integration/zimschool-web-android-v05` in [draft PR #10](https://github.com/georgebundorepositories-spec/Learning-Management-System/pull/10), awaiting a release review. Do not confuse a draft PR, a compiled preview APK or a Supabase API test with a production launch.

## Capabilities implemented in the development branch

**Web:** School-specific dashboards and identities, staff/teacher/learner/guardian roles, attendance, class and subject assignments, calendar, coursework, assessment scores, lesson preparation, curriculum lessons and progress, notices, school resources, invoice viewing, administrator invitations and guardian linking, and fictional demonstration pages.

**Android:** Verified online login, encrypted device PIN, 75 bundled curriculum lessons and local progress, calendar-only offline synchronization, staff attendance and lesson planning, school member and class assignments, classroom assessments/marks and teacher feedback, private learner/parent results, notices, coursework and resource/invoice viewing. Access to current private school data is online and governed by Supabase RLS.

**Backend:** Supabase multi-tenant tables with RLS, unexposed private security helpers, authenticated Edge calendar sync functions and user invitation code. Never embed service-role keys in a client application.

## Running the code locally

Web (Node.js 22, npm 10):
```bash
npm ci
# Configure ignored apps/web/.env.local with NEXT_PUBLIC_SUPABASE_URL
# and NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY
npm run dev --workspace=@zimschool/web
```

Database (Supabase CLI and Docker):
```bash
supabase start
supabase db reset
supabase test db
```
**Never run `supabase db reset` on the hosted production project.**

Android (Java 17, Android SDK 35, Gradle 8.9):
```bash
cd apps/android
gradle :app:testDebugUnitTest :app:assembleDebug
```
Set `ZIMLMS_SUPABASE_URL` and `ZIMLMS_SUPABASE_PUBLISHABLE_KEY` locally. Keep all privileged API keys and Android keystore passwords outside Git.

## Release and safety

See the [final-phase release runbook](docs/final-phase-release-runbook.md), [current deployment status](DEPLOYMENT_STATUS.md), [operations](docs/ops-runbook.md), [security](docs/security.md) and [release acceptance gates](docs/phase8-release-gates.md).

**No real children's personal records, live funds, bulk messages or automated production publishing before documented school-authorised consent, live testing and owner approval.**
