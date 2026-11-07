# Phase 0 — status and deviations

The authoritative requirements are the user-supplied MASTER PROMPT v2.0, Parts A–S. This file distinguishes implemented foundations from accepted outcomes.

## Delivered
- Monorepo with web, Android, shared database types and Supabase directories.
- Supabase project in LMS Omni, with shared primitives and the identity/school schema.
- 11 public identity/school tables with RLS and school-isolation safeguards.
- The generated TypeScript schema committed from the live Supabase project.
- Web SSR authentication screen and signed-in dashboard; invite-only registration.
- First Android online-auth and encrypted offline-PIN implementation.
- GitHub CI for the web, Android and pgTAP database validation, plus npm lockfile.
- Local-only first admin bootstrap script.

## Explicit deviations
1. **Next.js 16.4.5 replaces prescribed Next.js 14.** The older 14 branch has unresolved, high-impact advisories as of November 2026. The maintained active LTS release is the safer baseline for a new application. See https://nextjs.org/blog/security-update-2026-10-14.
2. **No cloud-exposed offline PIN hash.** A salted PBKDF2 verifier is encrypted in Android Keystore-backed local storage; public user profile does not expose a sensitive offline verifier.
3. **RLS helper functions are in unexposed `app_private` schema.** They use controlled `security definer` functions with explicit grants to avoid recursive policy evaluation.
4. **Shared tables have school-scope integrity checks.** Cross-school teaching/guardian relationships are rejected even for privileged callers.
5. **No production fees, messaging or school records were created.** These remain later phases.

Hosted Supabase Phone Auth must be enabled through the Auth Providers settings before phone/password sign-in works; local development has Phone enabled but public signup disabled.

## Gates still required
The Phase 0 gate is not merely compilation. Real signed-in web + Android flows and role isolation against realistic test accounts must be verified. Do not start Phase 1 until these are green.


## Database migration history reconciliation (2026-11-07)

Supabase MCP assigned its own migration timestamps for four successful live deployments.
The remote tracking entries have been atomically reconciled to the exact four
`supabase/migrations/20261107*.sql` filenames in GitHub:

- `20261107171600_p0_shared_primitives.sql`
- `20261107171700_p0_identity_and_rls.sql`
- `20261107171800_p0_tenant_relationship_hardening.sql`
- `20261107171900_p0_cron_and_net_extensions.sql`

The SQL changes had already been executed; this was migration metadata repair only.
Avoid future MCP migration applications that invent unmatched timestamps. Prefer
`supabase db push --remote` with versioned files (after authenticated CLI setup),
then verify `supabase migration list --remote`.

Security auditor review after the repair: 11/11 public tables still have RLS enabled;
Supabase security advisors returned zero findings. This does not replace end-to-end
cross-school or role tests.

## Additional verified Phase 0 work — 7 November 2026

- [PASS] PostgreSQL migrations, pgTAP role/tenant policy matrix and local Supabase Auth API smoke:
  [GitHub Actions #42403559420](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42403559420).
- [PASS] Android emulator offline PIN protection and lockout, after resolving CI /dev/kvm permissions:
  [GitHub Actions #42405183531](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42405183531).
- [PASS] Android online request/protected-profile unit tests (with in-process HTTP interceptors);
  real hosted Auth connectivity still needs a separate end-to-end demonstration:
  [GitHub Actions #42405551506](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42405551506).
- [PASS] PIN enrolment's slow PBKDF2 operation is now executed off the UI thread.
- [PASS] Expanded Android API 26 + 29 emulator compatibility and preliminary debug-size jobs:
  [GitHub Actions #42405832856](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42405832856).
- [PENDING] Hosted Supabase phone auth provider, real administrator provisioning and end-to-end UI sign-in against the hosted project.

**Phase 0 gate remains OPEN** until the remaining authentication checks and expanded device tests pass.
Do not claim that passing mocked Android HTTP unit tests proves a live hosted server login.
Do not start Phase 1 until the gate is verified.

## Local phone Auth acceptance and hosted provider boundary

- [PASS] Local phone/password Supabase Auth passed with an admin-provisioned synthetic phone identity, followed by an RLS-scoped profile read. The CI-only SMS-provider configuration is injected into the ephemeral GitHub runner and never deployed to LMS Omni:
  [GitHub Actions #42406420243](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42406420243).
- [PASS] GitHub Actions tests on Android API 26 (Android 8) and API 29 (Android 10); the current debug APK also passed a preliminary 25 MiB size budget:
  [GitHub Actions #42405832856](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42405832856).
- [NOT VERIFIED] Hosted Supabase Phone provider and legitimate SMS delivery, real administrator provisioning, and full UI end-to-end sign-in with an authorised test school.
- [NOT IMPLEMENTED] Persistent session lifecycle, refresh tokens and 72-hour multi-device offline sync, which belong to the subsequent sync gate.

The local dummy SMS configuration is intentionally *not* part of checked-in `supabase/config.toml`; never enable dummy SMS credentials on the hosted project. Real SMS provisioning should use valid provider credentials through Supabase's hosted Auth settings.
