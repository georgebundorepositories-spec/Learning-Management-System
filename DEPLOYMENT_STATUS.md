# ZimSchool LMS — deployment status

Last verified: 8 November 2026.

## Supabase / LMS Omni — live backend

Project: `mezbtcdkmvtkjxdmmhbw` (`eu-west-2`).

- Phase 0 identity schema and Phase 1 **calendar sync backend** are deployed.
- Database migrations `p1_calendar_sync_slice`, `p1_sync_fk_indexes` and `p1_demo_school_flag` are applied.
- Edge Functions `sync-push` and `sync-pull`: ACTIVE v1, JWT verification enabled.
- All 17 public application tables have RLS; last security-advisor scan: **0 findings**.
- Anonymous access smoke: both functions return HTTP 401 without JWT.
- **Synthetic school data only**: 2 schools with `is_demo=true`, 4 academic years, 12 terms, 8 classes, 13 subjects, 6 feature flags. The reproducible non-personal fixtures are in `scripts/seed-fictional-schools.sql` on the Phase 1 branch.
- No Auth identities, real pupils, staff profiles, phone numbers or private school records were seeded.

## GitHub and web preview

- The **read-only fictional school preview** has passed TypeScript, test, Next.js build and actual HTTP route checks.
- Public-preview PR #2 was squash-merged into `main` at commit `2667a2b8e031e8bf7d12fa3eecb653cc54a8c5d0`.
- Main homepage redirects to `/demo`; `/login` and `/dashboard` retain real school Auth restrictions.
- `apps/web/vercel.json` includes only a public Supabase URL and publishable key. Vercel Root Directory must be **`apps/web`**.
- A GitHub merge does not prove the website is live. **Vercel project import and its public deployment URL have not been verified** from the connected GitHub records. Do not invent a URL.

## Phase 1 code — draft, unmerged

<https://github.com/georgebundorepositories-spec/Learning-Management-System/pull/1>

Web IndexedDB/calendar queue, PWA app-shell cache, Android Room/transport, and source for the active sync API remain on `feature/phase1-offline-sync`. Complete hosted authenticated sync, secure credential refresh and physical two-device 72-hour acceptance are not yet verified.

## Not production ready

- Live hosted user invitations, phone provider and real sign-in acceptance outstanding.
- No operational Android release; debug APKs are available only in workflow artifacts.
- Full marks/attendance/fixtures offline convergence and 2G gate not complete.
- Do not collect actual student or guardian personal information until release gates pass.

[Phase 0 status](docs/phase0-status.md) · [Public demo release](docs/public-demo-release.md) · [Phase 1 PR](https://github.com/georgebundorepositories-spec/Learning-Management-System/pull/1)
