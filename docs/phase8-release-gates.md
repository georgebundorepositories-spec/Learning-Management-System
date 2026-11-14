# ZimSchool LMS — Phase 8 release gates (8 November 2026)

> **Release state: NOT PRODUCTION-READY.** This document distinguishes implemented foundations from acceptance proof. Do not onboard real pupils, collect live fees, or enable bulk messages until the gates are complete.

## Phase roadmap verified against the repository README

| Phase | Current development evidence | Completion gate |
|---|---|---|
| 0 — Identity | Phone/email web authentication, protected dashboard, Android login foundation | Real hosted phone/email invite flows, inactive-account rejection on Android and web |
| 1 — Offline | Phase 1 queue/sync work is isolated in draft PR #1 | 72-hour two-device offline convergence, stale-auth denial, tombstone and retry safety, slow/2G network tests |
| 2 — Calendar/sports | School-scoped approved calendar and sports category viewer | Event creation/approval, fixtures/results, real permissions and offline updates |
| 3 — Portals | Teacher attendance, assessment marks, learner/parent views, learning URL catalogue | Complete write/read permissions and negative RLS tests; assignments, submissions and file uploads |
| 4 — Payments/USSD/receipts | School invoice issuance and private invoice views; no gateway or ledger | Verified provider webhooks, immutable double-entry ledger, receipts only after confirmed settlement, duplicate-safe retries, refunds/reversal and USSD provider tests |
| 5 — Messaging | Role-targeted in-app notices; no SMS or push transmission | Provider integration, opt-in/opt-out policies, queue retries, consent and delivery auditing |
| 6 — Content/reports | Subject resources, school metrics and 1,000-row attendance snapshot | File storage scanning, course/assignment flows, accurate complete-term reports and downloadable records |
| 7 — Administration | Admin-only read-only settings, feature flags, audit list; responsive CSS | Secure invitation, deactivation, role provisioning, localisation and accessibility audit |
| 8 — Hardening | Web CI, database CI, migrated RLS, duplicate-safe invoices/notices, rollback-only tenant canary | All mandatory release gates below |

## Evidence checked during this development pass

- Web build CI: TypeScript, unit tests, production build and live Next.js demo route passed on workflow #141 after Phase 4–7 portal additions.
- Local Supabase migration reset, pgTAP, Auth/tenant isolation and phone-auth smoke passed on workflow #98 for the Phase 4–5 migrations.
- Hosted transaction-only canary: same-school administrator/learner access and cross-school/unrelated-learner denial for fee invoices; cross-school notice denial. Canary test rows were rolled back, with 0 persisted invoice or notice canaries.
- A subsequent SQL regression test and request-key changes have been committed; the latest workflow result must be checked before release. Prior CI success does not guarantee later commits.
- Supabase security adviser still reports **leaked-password protection disabled**. Enable it in hosted Auth settings and rerun the security adviser. Reference: https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection

## Mandatory release evidence before merging as production-ready

1. Run `npm ci`, `npm run lint --workspace=@zimschool/web`, `npm run test --workspace=@zimschool/web`, and `npm run build --workspace=@zimschool/web`.
2. Run `supabase start`, `supabase db reset`, `supabase test db`; preserve CI log URLs. Database resets are **local only**.
3. Verify hosted authenticated workflows for admin, head, teacher, learner, parent and bursar with *authorized*, nonproduction test accounts. Test cross-school and cross-role denials, including parent-only records.
4. Test invoice request key replay with the same and mismatched payload. Never create a payment receipt without a server-verified, signed provider settlement and reconciliation.
5. Test in-app notice audience filtering, repeated submits and 2G reconnect.
6. Verify role-specific Android UI on actual Android 8+ devices and a web mobile viewport. Do not call the debug APK a production release.
7. Verify offline operation for 72 hours on two devices, then out-of-order sync, conflicts, retries, tombstones and auth expiry.
8. Complete accessibility audit (WCAG 2.2 AA target), Shona/Ndebele/English copy review, backup/restore drill, retention policy, incident runbook, audit exports, 2G performance gate, rate limiting and monitoring.
9. Configure Vercel, hosted environment variables, Auth password protection, legitimate provider accounts and separate staging environment. Record actual deployed URL and test it from an unauthenticated browser before announcing release.
10. Obtain sign-off for production use involving children's records and live financial transactions.

## Safety constraints

- Supabase publishable keys may be used by clients; never place service-role/secret credentials in GitHub source, APKs or `NEXT_PUBLIC_` variables.
- Parent and learner finance visibility is enforced in PostgreSQL, not just by hiding UI links.
- Duplicate-safe issue/notice request keys are per-school unique. They do not substitute for payment-provider idempotency.
- The school dashboard's attendance summary is a sample capped at 1,000 entries, not a total attendance rate.
- All school data in the current project must remain fictional or approved test data until the gates above are signed off.
