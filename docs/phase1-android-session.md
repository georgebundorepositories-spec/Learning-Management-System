# Phase 1 Android — encrypted sessions and background sync

**Status: developer branch; CI build and emulator tests required. Do not merge until accepted.**

## Implementation

- `AuthRepository.loginWithSession` validates the school's RLS-scoped staff profile before returning a short-lived Supabase access token and GoTrue refresh token. Raw passwords are not persisted.
- `SessionVault` encrypts the profile, refresh token, access token, expiration and random per-user device UUID in AES-256-GCM using a non-exportable Android Keystore key. The Android app sets `allowBackup=false` to prevent moving encrypted sessions to another installation.
- `SessionBroker.accessToken` serializes refresh-token rotations with one per-process coroutine mutex and rotates the stored refresh token atomically. A rejected/invalid refresh fails closed to online sign-in. Temporary network failures preserve both the session and the queued edits.
- `CalendarSyncWorker` uses Room and an authenticated user-JWT `SyncHttpApi`, never a service-role secret. WorkManager only schedules it when connected; periodic interval is **at least 15 minutes**, but Android may defer due to Doze/OS optimization. The one-time worker provides a reconnect request.
- `SchoolCalendarPanel` queues staff calendar inserts into Room *before* scheduling network traffic. Staff can inspect the pending/failed queue and request an explicit sync. Calendar cache remains per authenticated user/school.
- `MainActivity`: tokens are saved **only after** successful offline-PIN enrollment following a valid online login. Offline PIN unlock can show local data with no network; if a matching stored server session exists, it reactivates the WorkManager schedule. Sign-out cancels jobs, serializes with refresh-token rotation, attempts a five-second local-scope GoTrue server revocation when online, and always clears both Keystore-protected session and offline PIN.
- `SessionVaultInstrumentedTest`: emulator tests for encrypted storage, altered ciphertext, logout, cross-school token refusal and concurrent refresh requests.

## Security boundaries

The PIN is a *local access gate*, not proof of current server authorization. Remote requests require a valid GoTrue JWT, school memberships and server-side RLS. We do **not** store student/guardian personal data in the current unencrypted Room calendar. Cached calendar content may still be sensitive in real deployments, so at-rest Room encryption and a cache-retention policy are required before storing confidential school records.

Sign-out attempts `POST /auth/v1/logout?scope=local` to revoke **this device's** refresh session when online, without disrupting other signed-in devices. If offline, the remote revocation cannot be completed; local tokens and PIN are still cleared. **Access JWTs remain valid until their expiry even if a session is revoked**, as specified by Supabase Auth. Before general release, verify hosted logout end-to-end, shorten JWT lifetime as appropriate, and integrate server-side device registration/revocation checks.

When changing user accounts on shared tablets, explicit sign-out is mandatory. A single device session is active at once; the Room database name is scoped to the prior user and school.

## Acceptance criteria still open

- Host an approved test account using the real Supabase Auth Admin API (do not fabricate confirmed login records by direct SQL).
- Verify phone/email hosted sign-in, refresh-token rotation on a real Android 8 tablet, account deactivation, logout, and session expiration.
- Measure real WorkManager execution/retry under unstable 2G connections and Android Doze conditions.
- Fully encrypt protected locally cached information; add per-user account purge/retention policies and keep user-generated queued work until safely reconciled.
- Full mixed-entity 72-hour / two-physical-device Q.2 acceptance test remains mandatory.

## Additional regression coverage — 8 November 2026

- `SyncEngine` now serializes manual and WorkManager calls with a process-wide mutex.
- A pull transaction rechecks its most recent persisted `calendar_cursor`; a late response cannot roll the cursor or the cached event state backwards.
- Emulator tests reproduce stale cursors during HTTP calls, simultaneous worker/foreground push attempts, and an interrupted network that must preserve Room queued edits.
- Emulator tests verify successful `scope=local` session-revocation request and best-effort offline sign-out that clears local tokens.
- Initial expanded Android CI run [#42455502984](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42455502984) failed in the new remote-logout instrumentation test on API 26 and API 29; the APK build succeeded. Its HTTP interceptor assertions obscured the exact request mismatch. A corrected, independently assertable mock response/test has been committed; **run [#42455881036](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42455881036) is the verification gate**. Do not treat this work as passing until both emulators are green.
