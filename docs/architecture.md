# Architecture

Authoritative source: MASTER PROMPT v2.0, Parts B–E.

The Android client and PWA persist local changes in encrypted/local stores and enqueue an idempotent push. Cloud state resides in Supabase PostgreSQL with school-scoped RLS. Pull cursors and tombstones must preserve consistent convergence and prevent data loss after prolonged disconnection.

## Design review required before Phase 1
- Restrict sync to an explicit entity allowlist, with per-entity authorization. Never trust client-provided school_id.
- Implement stable cursor ordering (timestamp plus unique ID) to avoid missing tied timestamps.
- Use server-side transactional updates for invoices/payments, rather than mutable client balances.
- Ensure conflict resolution is version-based, not dependent solely on untrusted device clocks.

No infrastructure has been deployed by this repository scaffold.
