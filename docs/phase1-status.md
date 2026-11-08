# Phase 1 — Offline Sync Engine (DRAFT, NOT DEPLOYED)

Authoritative basis: `MASTER PROMPT v2.0 — ZimSchool LMS.txt`, Parts E, H.3, P (Phase 1), Q.2.

## Deployment safety

All Phase 1 changes live on the draft branch `feature/phase1-offline-sync` (PR #1), **not on main**. Do not apply the Phase 1 migration or deploy the Edge Functions to LMS Omni until the Phase 0 hosted login gate is closed and the Phase 1 security review is complete.

## Implemented code

- `supabase/migrations/20261107200601_p1_calendar_sync_slice.sql`: calendar events, device registry, operation log, audit log, feature flags, append-only change log; all public tables have RLS. Calendar writes use an allowlisted private, authorisation-checked function wrapped in an invoker RPC. The actor's school comes from the verified user profile, not the client.
- `supabase/functions/sync-push` and `sync-pull`: user-JWT-authenticated Edge endpoints with a fixed `calendar_events` allowlist. Retryable server errors do not acknowledge the queue.
- `apps/web/lib/sync`: user/school-scoped IndexedDB queue, optimistic projections, transactional server-row acknowledgement and cursor commits, and an Edge transport.
- `apps/android/app/src/main/java/zw/zimlms/core/sync`: Room queue and calendar snapshot schema, transactional sync engine, and JWT-based transport (requires session-broker integration).
- Web unit tests: two logical devices, 50 edits each with client timestamps 73 hours old; idempotency, conflict preservation, cursor monotonicity.
- pgTAP integration tests: tenant forgery denied, replay does not duplicate, version mismatch yields conflict, soft-delete tombstone retained, cross-school pull/write blocked.
- Android instrumentation tests: queue durability across DB re-open, two in-memory caches converging after 50 edits each, conflict retention.

## Defensible specification deviations (pending user review)

1. **Optimistic concurrency over client-time LWW.** Part E's sample compares client timestamps. A device with a wrong clock can overwrite authoritative data. This implementation uses `sync_version` compare-and-swap and retains conflicts until reviewed. Calendar field-level merge is still pending.
2. **Commit-safe cursor.** Part E's sample pulls using only `updated_at`, which risks misses on tied timestamps and concurrent commits. We use a server-generated bigint change sequence, with per-school advisory locking before sequence assignment. Only an atomically saved pull page advances the durable cursor.
3. **Calendar migration partly brought forward.** Part P places most calendar schema in Phase 2, but Q.2's Phase 1 acceptance test requires offline calendar writes. A narrow calendar table exists early; sports, attendance and marks are not yet included.
4. **Browser store uses native IndexedDB, not Dexie.** It avoids a new dependency while the protocol is validated, but the specification calls for Dexie; migrate or formally approve the alternative before Phase 1 release.
5. **Room 2.8.5 rather than 2.6.1.** Uses a maintained 2026 stable version, verified with Android 8 compatibility builds. Local calendar cache is currently unencrypted. Do not store student PII until at-rest encryption and data-retention controls are implemented.
6. **Staff-only calendar sync initially.** Teacher pulls expose authored events only; admin/head pulls cover the school. Student/parent visibility and approval-state transitions remain Phase 2 obligations.
7. **No automated tombstone purge yet.** Hard delete is prohibited. A retention window needs an expiration-aware full-refresh protocol before implementing the specification's 90-day tombstone purge.
8. **WorkManager sync schedule not active.** JWT refresh and secure session persistence are not ready. No background worker should attempt sync with stale/unavailable credentials.

## Acceptance gates not yet satisfied

- [ ] Hosted Supabase phone provider, provisioned admin and real web/Android sign-in verified (Phase 0 gate).
- [ ] Android real-device online JWT persistence/refresh and reliable background WorkManager 15-minute sync.
- [ ] PWA installability and reconnect integration wired to the queue; calendar CRUD UI.
- [ ] Calendar field merge and approval-state handling complete.
- [ ] All required entities (calendar, marks, attendance, fixtures) supported with per-role RLS.
- [ ] Q.2: two physical devices offline for 72 hours with 50 mixed changes each; reconnect, replay and convergence without duplicate/lost operations.
- [ ] C4: complete term sync under 5 MB at simulated 2G throughput.
- [ ] C2/C3: release APK under 25 MB and cold-start under 3 seconds on 2 GB Android 8 hardware.
- [ ] No student PII exposed to other schools, unauthorized users, or unencrypted local caches.

The CI tests are an initial engineering verification, **not** the final Q.2 acceptance test.
