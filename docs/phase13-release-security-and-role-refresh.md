# Phase 13 — Production security and live-role checks

Status: **development-only**, draft pull request #10; never merge without a real-device and school-owner release review.

## PostgreSQL privilege correction (deployed)

- Audit of the hosted project found 22 existing application tables with `TRUNCATE`, `REFERENCES` and `TRIGGER` grants to both `anon` and `authenticated`. On PostgreSQL 17 the same roles also held `MAINTAIN` grants. Whole-table `TRUNCATE` is NOT constrained by row-level security.
- Migration `20261111080000_phase13_deny_unscoped_table_privileges.sql` revokes `TRUNCATE`, `REFERENCES`, `TRIGGER`, and PostgreSQL 17+ `MAINTAIN` for both client roles on all current `public` tables. It also changes `postgres`'s default privileges for newly created `public` tables. Normal RLS-scoped `SELECT`, `INSERT`, `UPDATE`, `DELETE` grants are preserved.
- Verified by a hosted SQL check after the migration: **0 public tables** exposing those whole-table privileges to client roles; **0 public tables** without RLS; existing authorised reads from classes and teacher lesson plans retain their grants; **0 unsafe `postgres`-owned default table grants**.
- `supabase/tests/phase13_deny_unscoped_table_privileges.test.sql` protects this behaviour in CI.
- Scope limitation: Supabase's separate `supabase_admin` default privilege template also includes broad grants, but the project `postgres` role is not a member of `supabase_admin` and cannot amend that owner's defaults. All 29 current application tables are owned by `postgres`, and none is currently exposed. Review future tables created by a different database owner with Supabase platform administrators.

## Authenticated school membership workflow tests

- `scripts/test-academic-auth-e2e.mjs` now uses **seven** short-lived local JWT identities (including one school administrator). It verifies an authorised school administrator can assign a class teacher, move an enrolled learner and restore the class, link a guardian, and revoke the link. It also tests rejection of foreign-school assignments, learner writes to staff-only resources and inactive relationship visibility.
- This test refuses hosted endpoints, never emails or logs real credentials, and deletes synthetic records and identities in a `finally` cleanup.

## Android role freshness (v0.15 preview)

- `SchoolRoleVerifier` looks up current `public.users` role, activation, deletion and school directly through an existing encrypted Supabase session/JWT. It does not trust cached PIN claims or mutable JWT user metadata for live access.
- Online sign-in continues to open the role portal. A **PIN-only offline unlock** now exposes the bundled curriculum while delaying school-specific private workspaces until successful online verification. Verification reruns whenever the app resumes and can be retried manually. A changed role or inactive membership requires a fresh online sign-in.
- Failed online role verification **fails closed for private school modules** but does not delete local lesson progress. Available offline curriculum remains readable.
- The new debug package suffix is `zw.zimlms.preview.rolecheck15`, deliberately separate from v0.12/v0.13/v0.14 so earlier encrypted progress is not silently overwritten or imported. The Android build, JUnit policy tests and API26/API29 emulator tests must pass on the exact final commit before distributing a build.

## Truthful Vercel deployment status

- The preview workflow no longer reports a green "deployed" job when `VERCEL_TOKEN`, `VERCEL_ORG_ID`, or `VERCEL_PROJECT_ID` are missing. The job now fails visibly with a configuration error. An authenticated Vercel deployment must successfully serve `/demo`, `/login` and `/accept-invite` before it is counted as verified.
- Credentials must be configured in GitHub repository Actions secrets, never committed. In Vercel, configure the Next.js root directory to `apps/web` and the browser-only Supabase URL/publishable key.
- Configure the corresponding trusted `ZIMSCHOOL_WEB_ORIGIN` HTTPS origin as a Supabase Edge Function secret and allow `<origin>/accept-invite` in Auth redirects after the site is real. Only then is it safe to deploy the updated `invite-school-member` function. Do not deploy that revision with an unverified origin.
- Real teacher, learner and parent invitations, acceptance email delivery, self-chosen password sign-in, guardian consent for children's data, production Vercel hosting and owner-signed APK remain unverified.

## Remaining account and release gates

- The hosted school database currently contains one active administrator, **zero active teachers**, **zero active learners** and **zero active parents**. Do not represent synthetic Auth test fixtures as live users.
- Inviting two real people still needs each recipient's name, email, unique Zimbabwe mobile number, role and appropriate school consent.
- Supabase security advisors still report **Leaked Password Protection Disabled**; enable in Supabase Auth and repeat the security review before production.
- A passing GitHub web build, Supabase local database check, API smoke or Android emulator suite does not prove real SMS/email, a public Vercel deployment, or a physically installed owner-signed Android release.
