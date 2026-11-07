# ZimSchool LMS — Phase 0 operations runbook

## Infrastructure
- Supabase organisation: **LMS Omni** (`ruyfcuepicadkzyqmbkm`)
- Supabase project: **ZimSchool LMS** (`mezbtcdkmvtkjxdmmhbw`)
- Region: `eu-west-2` (London)
- Repository: `georgebundorepositories-spec/Learning-Management-System`, branch `main`
- Credentials must stay in environment variables, GitHub Actions secrets or a secrets manager.

## Prerequisites
Node.js 22, npm 10, Android SDK 35, Java 17, Gradle 8.9, Supabase CLI and Docker for local DB tests.

## Local web app
1. From the repo root, run `npm ci`.
2. Create `apps/web/.env.local` (ignored) with `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`.
3. Run `npm run dev --workspace=@zimschool/web`.
4. Sign in using an administrator-provisioned account. Public registration is deliberately disabled.

## Local database
Run `supabase start`, then `supabase db reset` and `supabase test db`. Migration files live under `supabase/migrations/`. Do not run `db reset` against the production project; it destroys data.

## One-time first administrator
For a new *empty* school, configure secure, ignored environment variables:
- `NEXT_PUBLIC_SUPABASE_URL` (or `SUPABASE_URL`)
- `SUPABASE_SECRET_KEY` (server-only; `sb_secret_...` or legacy service-role JWT)
- `ZIMLMS_SCHOOL_NAME`, `ZIMLMS_ADMIN_FULL_NAME`
- `ZIMLMS_ADMIN_PHONE` in Zimbabwe E.164 format
- `ZIMLMS_ADMIN_PASSWORD` (minimum 12 characters)
- Optional `ZIMLMS_EMIS_NUMBER`

Run `node scripts/create-admin.mjs`. This privileged command must run only in a trusted administrative environment and never in a browser, APK or public CI job. It creates one school and a phone-authenticated administrator.

Note: The administration and bulk-invite UI are not yet implemented. Administrator provisioning is a gated operational action.

## Android Phase 0
Set `ZIMLMS_SUPABASE_URL` and `ZIMLMS_SUPABASE_PUBLISHABLE_KEY` locally.
From `apps/android/`, run `gradle :app:testDebugUnitTest :app:assembleDebug`.
The Phase 0 APK can sign in online and enrol one encrypted offline PIN on the device. Full offline school CRUD and sync require Phase 1.

## Quality gate
- `npm run lint --workspace=@zimschool/web`
- `npm run test --workspace=@zimschool/web`
- `npm run build --workspace=@zimschool/web`
- `supabase test db`
- Android debug APK and unit tests in GitHub Actions
- **Required before Phase 1:** demonstrate end-to-end web and Android sign-in with a temporary, authorised test school; verify no cross-school leakage with positive and negative tests.

## Incident policy
Never disable RLS to work around an access failure, never publish privileged API keys, and never change school IDs from untrusted requests. Backup and retention procedures must be documented and validated before real student data is onboarded.
