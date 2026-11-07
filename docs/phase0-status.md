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
