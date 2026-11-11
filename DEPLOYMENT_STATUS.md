# ZimSchool LMS — Deployment status

**Current as of 11 November 2026: DEVELOPMENT BUILD VERIFIED; PRODUCTION LAUNCH BLOCKED.**

This status supersedes the historic 8 November summary retained below. The review branch is `integration/zimschool-web-android-v05`, draft [PR #10](https://github.com/georgebundorepositories-spec/Learning-Management-System/pull/10). The repository default branch is `main`; neither merging nor releasing is approved by this status report.

## Actual confirmed evidence

- **Current web, database and Android development code:** v0.17.0 (Phase 15), all four workflow checks passed at commit `dba2684b`. [Android](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/43014992470), [database](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/43014992462), [web](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/43014992653), and [API auth](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/43014992493).
- **Host:** Supabase `mezbtcdkmvtkjxdmmhbw`, 29 `public` tables with RLS on all 29; last hosted check showed 1 active administrator, zero active teachers/learners/parents.
- **Functions:** calendar `sync-push`/ `sync-pull` v1 live, `invite-school-member` hosted v1. Invitation source in development has additional verified-origin security but is NOT deployed because no HTTPS site or Auth allow-list is confirmed.
- **Preview app:** [Android v0.17 debug APK](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/43014992470/artifacts/13852491741), not a stable signing identity.
- **Security:** Supabase advisor still warns **leaked-password protection disabled**. Other verified hosted application tables retain RLS and no unsafe client TRUNCATE/TRIGGER/REFERENCES/MAINTAIN grants.

## Final release gate (Phase 16)

The automated [final release gate](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/workflows/final-release-gate.yml) checks public deployment, deployment/signing configuration and independent release evidence, and **must fail** if any requirement is missing. It does not send mail, activate users, merge, deploy or sign by itself. The [release runbook](docs/final-phase-release-runbook.md) explains every remaining action and the distinction between a limited pilot and full commercial rollout.

**Unverified external dependencies:** actual Vercel site; authenticated invite email, redirects and real role sign-in; guardian consent and school data policy; 72-hour two-device and 2G testing; restored backup; permanent Android signing; messaging/USSD/payment providers and reconciliation; owner sign-off. Payments are not working, invoice views are not receipts, and calendar-only offline sync is not universal offline data synchronization.

---

## Historic 8 November engineering log — superseded for release status

# ZimSchool LMS — deployment status

Last verified: 8 November 2026.

## Phase 3–8 development update — 8 November 2026 (draft PR #9)

**This branch is not a production release.** The latest hosted LMS Omni database contains **23 public tables, all with RLS enabled**. Canonical copies of the previously applied hosted Phase 3–8 migrations now live in GitHub using the exact hosted migration versions.

Development branch features include role-specific school dashboards, attendance, subject resource links, assessments/marks, published calendar/sports viewing, private fee invoices (no payments), in-app notices, school reports, and a read-only administration/audit overview. Responsive layouts and dashboard loading/error/denial states are included.

The hosted transaction-only finance/notice RLS canary verified school isolation and a learner's own fee invoice. All test rows were rolled back.

Supabase Performance Advisor reported 10 unindexed foreign keys; the deployed `phase8_covering_foreign_key_indexes` migration addressed these and the follow-up scan no longer lists that finding. Other informational unused-index observations and four multiple-permissive-policy performance findings remain. Supabase Security Advisor reports **leaked-password protection disabled**; enable it before hosting real user accounts.

**Outstanding:** Phase 1 offline 72-hour device convergence; real role-by-role hosted acceptance; Android release and 2G tests; payment provider verification, USSD and SMS integrations; complete learning/assessment offline sync; backup/restore drill; real Vercel URL verification; accessibility/localisation sign-off. Do not collect personal pupil data or process live school fees yet.

See [Phase 8 release gates](docs/phase8-release-gates.md) and [draft PR #9](https://github.com/georgebundorepositories-spec/Learning-Management-System/pull/9).

## Supabase / LMS Omni — live backend

Project: `mezbtcdkmvtkjxdmmhbw` (`eu-west-2`).

- Phase 0 identity schema and Phase 1 **calendar sync backend** are deployed.
- Database migrations `p1_calendar_sync_slice`, `p1_sync_fk_indexes`, `p1_demo_school_flag` and `p1_hide_other_teacher_conflict_rows` are applied.
- Edge Functions `sync-push` and `sync-pull`: ACTIVE v1, JWT verification enabled.
- All 17 public application tables have RLS. The latest Supabase security-advisor scan has **one Auth configuration warning**: breached-password protection is disabled; enable it before enrolling real accounts. No RLS adviser findings were reported.
- Anonymous access smoke: both functions return HTTP 401 without JWT.
- **Fictional school data only**: 2 schools with `is_demo=true`, 4 academic years, 12 terms, 8 classes, 13 subjects, 6 feature flags, 12 staff profiles, 20 learners, 8 guardians, 20 guardian relationships, 8 teaching assignments and 16 calendar events. Reproducible fixtures are in `scripts/seed-fictional-schools.sql` and `scripts/seed-fictional-people-and-events.sql` on the Phase 1 branch.
- All **40** fictional `auth.users` records are passwordless, banned until 2099, without identity-provider records; related `public.users` records are inactive. There are **no usable demo credentials** and no real personal data.
- The hosted, transaction-only RLS isolation canary passed using temporarily activated fictional profiles; changes were rolled back, preserving 0 active demo profiles and 0 logged test operations.

## GitHub and web preview

- The **read-only fictional school preview** has passed TypeScript, test, Next.js build and actual HTTP route checks.
- Public-preview PR #2 was squash-merged into `main` at commit `2667a2b8e031e8bf7d12fa3eecb653cc54a8c5d0`.
- Main homepage redirects to `/demo`; `/login` and `/dashboard` retain real school Auth restrictions.
- `apps/web/vercel.json` includes only a public Supabase URL and publishable key. Vercel Root Directory must be **`apps/web`**.
- A GitHub merge does not prove the website is live. **Vercel project import and its public deployment URL have not been verified** from the connected GitHub records. Do not invent a URL.

## Phase 1 code — draft, unmerged

<https://github.com/georgebundorepositories-spec/Learning-Management-System/pull/1>

Web IndexedDB/calendar queue, PWA app-shell cache, Android Room/transport, and source for the active sync API remain on `feature/phase1-offline-sync`. A real GoTrue JWT → local Edge HTTP → RPC integration suite passed (50+50 events, retry, conflict, tombstone, cross-school isolation): [run #42452783861](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42452783861). Complete **hosted** authenticated sync, secure credential refresh and physical two-device 72-hour acceptance are not yet verified.

## Not production ready

- Live hosted user invitations, phone provider and real sign-in acceptance outstanding.
- No operational Android release; debug APKs are available only in workflow artifacts.
- Full marks/attendance/fixtures offline convergence and 2G gate not complete.
- Do not collect actual student or guardian personal information until release gates pass.

[Phase 0 status](docs/phase0-status.md) · [Public demo release](docs/public-demo-release.md) · [Phase 1 PR](https://github.com/georgebundorepositories-spec/Learning-Management-System/pull/1)

