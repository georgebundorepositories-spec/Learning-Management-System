# ZimSchool LMS — deployment status

Last verified: 8 November 2026.

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
