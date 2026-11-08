# Phase 1 — Calendar sync backend DEPLOYED; client release DRAFT

Authoritative basis: `MASTER PROMPT v2.0 — ZimSchool LMS.txt`, Parts E, H.3, P (Phase 1), Q.2.

## Deployment safety

The calendar schema and allowlisted Edge sync APIs are **already deployed** to LMS Omni, including a security hotfix. The web and Android offline clients remain in draft PR #1 and must not be merged or released as an operational school LMS until hosted login and full Phase 1 acceptance are complete.

## Implemented code

- `supabase/migrations/20261107200601_p1_calendar_sync_slice.sql`: calendar events, device registry, operation log, audit log, feature flags, append-only change log; all public tables have RLS. Calendar writes use an allowlisted private, authorisation-checked function wrapped in an invoker RPC. The actor's school comes from the verified user profile, not the client.
- `supabase/functions/sync-push` and `sync-pull`: user-JWT-authenticated Edge endpoints with a fixed `calendar_events` allowlist. Retryable server errors do not acknowledge the queue.
- `apps/web/lib/sync`: user/school-scoped IndexedDB queue, optimistic projections, transactional server-row acknowledgement and cursor commits, and an Edge transport.
- `apps/android/app/src/main/java/zw/zimlms/core/sync`: Room queue, calendar snapshot schema, transactional sync engine, and JWT-based transport. Android now contains an encrypted Keystore session vault, serialized GoTrue token refresh, a staff calendar form and network-constrained WorkManager scheduling; new emulator tests are being verified. See `docs/phase1-android-session.md`.
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
8. **WorkManager scheduled but not field-accepted.** The Android client now queues a connected-only periodic worker at the OS minimum 15-minute interval and uses a Keystore-encrypted refresh-token store. Android can defer execution, and reliability on a real device and hosted identity remains unverified.

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


## Verified on 8 November 2026 — staged integration and fictional fixtures

- [PASS] A real GoTrue-issued JWT travelled through the **local** Edge `sync-push`/`sync-pull` HTTP endpoints, the authenticated SQL RPC and school-scoped RLS. The integration ran twice on logical devices with 50 records each, all client timestamps older than 72 hours. It confirmed idempotent retry, same-school teacher privacy, cross-school refusal, optimistic version conflict handling and tombstone retention:
  [Successful CI run #42452783861](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42452783861).
- [PASS] A same-school insert-ID collision had revealed another teacher's private event in a SECURITY DEFINER function. The deployed function now suppresses the protected row and returns a generic rejected response. The regression is in `supabase/tests/rls/p1_calendar_sync.sql`.
- [PASS] Hosted SQL transaction-only isolation canary checked role-scoped visibility against fictional event fixtures, **rolled back** all temporary activations and writes; 0 demo profiles remain active and 0 test op logs persisted.
- [PASS] Hosted demo data: two `is_demo=true` fictional schools, 4 academic years, 12 terms, 8 classes, 13 subjects, 12 staff, 20 learners, 8 guardians, 20 links, 8 teaching assignments and 16 calendar events. The 40 associated synthetic Auth rows are deliberately banned until 2099, unconfirmed, passwordless and have zero Auth identities; all associated school profiles are inactive. **They cannot be used to sign in.**
- [WARN] The hosted Supabase security advisor now reports `auth_leaked_password_protection`: breached-password checking is disabled. It requires a change in Supabase Auth settings before production enrolment. This is separate from school-scoped RLS and does not expose test passwords (none were created).
- [UNVERIFIED] Vercel's project-level deployment URL has not been observed through GitHub checks. A connected GitHub account alone does not prove the Vercel repository project is imported.

No hosted real-email login, live approved account, two physical devices offline for 72 hours, 2G budget, full marks/attendance sync or background Android WorkManager acceptance is complete.

## Release-size check — 8 November 2026

- [PASS] Release-variant R8/resource-shrunk **unsigned** APK built with `gradle :app:assembleRelease`, measured **1,307,201 bytes** (approximately 1.31 MB), below the 25 MiB CI budget:
  [GitHub Actions #42456216646](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42456216646).
- [PASS] Extended Keystore/session and sync safety regressions passed on API 26 and API 29:
  [GitHub Actions #42455881036](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42455881036).
- [PENDING] An **installable, signed** production release and physical Android 8 performance verification. An unsigned artifact is not a signed release.
