# ZimSchool LMS

Offline-first school management and learning platform for Zimbabwean schools, governed by **MASTER PROMPT v2.0 — ZimSchool LMS**.

## Current build status — Phase 0 foundations plus staged Phase 1 backend and fictional web preview
- Supabase project: [ZimSchool LMS](https://supabase.com/dashboard/project/mezbtcdkmvtkjxdmmhbw) in **LMS Omni** organisation.
- Applied foundation migrations: shared primitives, identity model and tenant relationship hardening.
- 17 public application tables have RLS policies and version triggers. Two fictional `is_demo=true` schools are seeded in LMS Omni, with academic/class/subject records but no personal identities.
- Next.js web app: phone/email login, invite-only registration, protected dashboard, sign-out, three-language UI foundation.
- Kotlin/Compose Android app: online phone/email login and Keystore-encrypted offline PIN; Android 8 and Android 10 emulator tests passed.
- GitHub Actions: [web checks](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/workflows/web.yml), [database/RLS](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/workflows/supabase.yml), [Android debug APK](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/workflows/android.yml).
- Phase 1 calendar synchronisation backend is live in Supabase; further offline engine work lives in [draft PR #1](https://github.com/georgebundorepositories-spec/Learning-Management-System/pull/1), not in `main`.
- The public fictional school preview is in `main`: `/demo` is the homepage. Web CI tested the actual route, but a Vercel deployment URL has not been verified.
- Real hosted user sign-in, 72-hour two-device offline convergence and the complete Phase 1 gate are outstanding.

## Local web quickstart
Prerequisites: Node.js 22 and npm 10.

```bash
npm ci
# Add NEXT_PUBLIC_SUPABASE_URL and NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY to apps/web/.env.local
npm run dev --workspace=@zimschool/web
```

Visit http://localhost:3000/demo for the fictional school preview. For secure login, open http://localhost:3000/login and sign in with an administrator-provisioned account.

## Local database testing
Prerequisites: Supabase CLI and Docker.

```bash
supabase start
supabase db reset
supabase test db
```

No public registration is enabled. Use the [ops runbook](docs/ops-runbook.md) for first-school setup.

## Android development
Set `ZIMLMS_SUPABASE_URL` and `ZIMLMS_SUPABASE_PUBLISHABLE_KEY` in your local environment. From `apps/android` run:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

GitHub Actions also publishes the compiled debug APK under the Android workflow artifacts.

## Build phases
0. Foundations and secure login — **in progress**.
1. Offline sync and convergence — partial backend deployed; remainder in draft development, must pass the 72-hour two-device test.
2. Calendar and sports.
3. Teacher, student and parent portals.
4. Payments, USSD and receipts.
5. Messaging and alerts.
6. Content and reports.
7. Administration, localisation, performance and accessibility.
8. Hardening, acceptance tests and documentation.

See [deployment status](DEPLOYMENT_STATUS.md), [public preview release](docs/public-demo-release.md), [current Phase 0 status](docs/phase0-status.md), [architecture](docs/architecture.md), [security baseline](docs/security.md), and [operations](docs/ops-runbook.md).

## Security
Do not commit secret keys, administrator passwords, student records or personal data. Use the public publishable key only in client apps. Each newly exposed table requires school-scoped RLS and negative access tests. Offline state never proves cloud authorisation.

## Licence
The specification calls for AGPL-3.0 with a separately drafted commercial exception for payments/messaging adapter interfaces. Licence wording and exceptions require review before external distribution.
