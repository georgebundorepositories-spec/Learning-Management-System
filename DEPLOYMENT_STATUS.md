# ZimSchool LMS — deployment status

Last verified: 7 November 2026.

## LMS Omni (Supabase)

Project: `mezbtcdkmvtkjxdmmhbw` (London, `eu-west-2`).

**Phase 1 calendar sync backend is deployed.** The following components are live:
- Database migrations `p1_calendar_sync_slice` (version `20261107200601`) and `p1_sync_fk_indexes` (version `20261107200817`).
- Edge Functions `sync-push` and `sync-pull`, both version 1, with JWT verification enabled.
- All 17 public tables have row-level security enabled; Supabase security advisor reported 0 findings.
- External HTTP test confirmed both endpoints reject missing JWTs with status 401.

## Not live yet

- No public Next.js web deployment; no Vercel hosting connection or domain has been set up.
- No released Android APK or Play Store installation; development APK builds exist in CI.
- Hosted school/admin credentials, SMS provider, real phone login and end-to-end authenticated hosted sync are not verified.
- Phase 1 full 72-hour offline, mixed-entity and 2G gates are not complete.

**The deployable Phase 1 backend code is maintained in the unmerged draft PR #1, not in `main` yet:**
https://github.com/georgebundorepositories-spec/Learning-Management-System/pull/1

Full deployment record:
https://github.com/georgebundorepositories-spec/Learning-Management-System/blob/feature/phase1-offline-sync/docs/deployments/2026-11-07-phase1-backend.md

Do not present this state as a production-ready school LMS or bypass release acceptance gates.
