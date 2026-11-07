# ZimSchool LMS — Phase 1 backend deployment record

Date: 7 November 2026 (Africa/Harare)
Target: **LMS Omni** Supabase organisation `ruyfcuepicadkzyqmbkm`
Project: **ZimSchool LMS** (`mezbtcdkmvtkjxdmmhbw`), `eu-west-2`
Deployment state: **Backend active; full Phase 1 acceptance not complete.**

## Deployed components

- Additive SQL migration `p1_calendar_sync_slice` from source file `supabase/migrations/20261107200601_p1_calendar_sync_slice.sql` on commit `ef0bbae93dd2cd7374e1168b6ef72454676261a7`.
- Additive performance migration `p1_sync_fk_indexes` from `supabase/migrations/20261107200817_p1_sync_fk_indexes.sql`: four missing foreign-key indexes.
- Supabase Edge Function `sync-push`, version 1, `verify_jwt=true`:
  `https://mezbtcdkmvtkjxdmmhbw.supabase.co/functions/v1/sync-push`
- Supabase Edge Function `sync-pull`, version 1, `verify_jwt=true`:
  `https://mezbtcdkmvtkjxdmmhbw.supabase.co/functions/v1/sync-pull`
- Typed client schema regenerated from deployed Supabase tables and RPCs on `feature/phase1-offline-sync`.

## Verification evidence

- Source branch CI at commit `ef0bbae`: database migration + pgTAP, web type checks/test/build, Android debug build and API 26/API 29 device tests passed.
- Supabase project reports `ACTIVE_HEALTHY`; all **17 public tables** have RLS enabled and at least one policy.
- Supabase security advisor: **zero findings** after deployment.
- Supabase performance advisor: **zero unindexed foreign keys** after adding four indexes. Unused-index notices are expected on an empty project.
- External no-token smoke run: both endpoints returned **HTTP 401**:
  https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42409735192
- No school records existed at deployment (all 17 application tables empty).

## Not established

- No successful **authenticated hosted** push/pull has been observed: no hosted school/admin test identity has been provisioned.
- No Next.js deployment URL: web hosting provider is not connected/configured in this project.
- No production Android release or user-facing Phase 1 offline-sync UI. A development APK exists in GitHub Actions only.
- No persistent device session refresh, WorkManager 15-minute background job, complete offline marks/attendance/fixtures, field-level conflict reconciliation, 2G or physical two-device 72-hour acceptance.
- No real SMS provider has been configured. Neither successful OTP delivery nor hosted phone login has been verified.

**Release gate:** Phase 1 is a *staged backend deployment*, not a finished product. The draft PR must remain open until Phase 0 hosted login, Phase 1 security, and complete offline acceptance gates have passed. Keep `main` unchanged.

## Follow-up actions

1. Complete hosted Supabase Auth provider configuration, provision a synthetic school/admin test user using operator-only tools, and test authenticated API requests plus cleanup.
2. Connect a Next.js hosting account (Vercel is appropriate), deploy a reviewed client and configure its publishable Supabase URL/key; never expose secret keys.
3. Finish session persistence, web PWA and Android UI integration, 15-minute background sync, all required entities, 2G limits and real 72-hour cross-device convergence.
4. Merge PR only after the release gates pass.

Migration version verification: Supabase `list_migrations` returned `20261107200601 p1_calendar_sync_slice` and `20261107200817 p1_sync_fk_indexes`. The repository file prefixes now match these deployed versions to prevent repeat application by the Supabase CLI.
