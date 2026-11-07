# ZimSchool LMS

Offline-first learning management system for Zimbabwean schools, based on **MASTER PROMPT v2.0 — ZimSchool LMS**.

## Project targets
- Android (Kotlin/Jetpack Compose; Room + durable sync queue) first, web PWA (Next.js + Dexie) second, USSD/SMS fallback.
- Supabase Postgres/Auth/Storage/Edge Functions with school-scoped row-level security.
- English, Shona and Ndebele localisation.
- Reliable offline-first operations, including conflict handling and idempotent sync.

## Build order
Follow Part P of the specification; **do not implement feature phases before their phase gates pass**.

1. **Phase 0 — Foundations:** repository toolchain, CI scaffold, database migrations, identity/RLS foundation, login prototypes and tests.
2. **Phase 1 — Sync:** persistent offline queues, secure push/pull, reconnect and two-device convergence tests.
3. Phase 2 — Calendar and sports.
4. Phase 3 — Staff, student and parent portals.
5. Phase 4 — Payments.
6. Phase 5 — Messaging.
7. Phase 6 — Content and reports.
8. Phase 7 — Administration, localisation, accessibility and performance.
9. Phase 8 — Security hardening and documentation.

## Development guardrails
- Never commit credentials, service-role keys or real personal information.
- Every new exposed table requires RLS and role-isolation tests.
- Payment callbacks must be authenticated and idempotent; never trust client-reported payment success.
- A client must never select an arbitrary SQL table or school scope for sync operations.
- Never consider a phase complete without passing its acceptance tests.

## Status
**Phase 0 initialized.** No application builds, Supabase migrations or integration tests have passed yet.

## Primary specification
The controlling product requirements are in the user-supplied document `MASTER PROMPT v2.0 — ZimSchool LMS.txt`.
