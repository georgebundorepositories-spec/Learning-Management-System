# ZimSchool LMS — Phase 1 offline acceptance protocol

Version: 2026-11-08
Status: **NOT YET EXECUTED**. This is a test protocol, not evidence of successful 72-hour operation.

## Environment and prerequisites

- Two physical Android devices, preferably Android 8 (2 GB RAM) and Android 10+, with the same reviewed release candidate installed.
- A staging/demo school explicitly tagged `is_demo=true`.
- Two active synthetic teacher identities provisioned through **Supabase Auth Admin API**, not by inserting password hashes directly into `auth.users`.
- Real test logins and verified token-refresh functionality on **hosted** LMS Omni. The existing 40 fictional identities are deliberately banned/passwordless and cannot be used.
- Verify school tenant isolation and the logged-in account names on both devices.
- Use demonstrably fictional event/learner data; do not record any real child or family information.
- Use independent recorders for start/end times, device model, OS, storage, available RAM, app version/commit, and test-school ID.
- Do not label the current 100-calendar-event local CI test as the required mixed-entity acceptance.

## Three-day disconnected scenario (72 continuous hours)

1. Connect both devices, authenticate once, download the baseline school dataset and capture the initial server cursor and row counts.
2. Switch on airplane mode **including disabling Wi-Fi**, and record a test start timestamp in Africa/Harare time. Keep both devices disconnected for at least 72 hours.
3. Device A queues 50 mixed edits and Device B independently queues 50. Required types: calendar events, marks, attendance and sports fixtures (as specified in Master Prompt Part Q.2). Include inserts, updates and deletes, repeated edits to the same item, at least 3 deliberate same-field conflicts, and a simulated app force-stop/relaunch.
4. Throughout the offline period, check that all 100 operations remain durable after normal process termination and app restart; verify that successful offline PIN unlock does not expose another account's data.
5. After 72 hours, reconnect Device B first on a constrained 2G profile; capture timestamps, HTTP bytes sent and received, push acknowledgement outcomes and pull cursor.
6. Reconnect Device A. Verify no operation IDs were duplicated and no conflicting edit was silently discarded.
7. Trigger a second sync on both devices and compare canonical server IDs, approved values, sync versions and tombstone states. The final server cursor must be monotonic. An unchanged second sync must be idempotent.
8. Repeat an interrupted connection at the point a server ACK is lost; the client should retry the identical `op_id` and the server must not create a duplicate row.

## What must be measured

| Acceptance metric | Expected outcome | Evidence required |
|---|---|---|
| True continuous disconnection | At least 72 hours | Device/network log start and end |
| Two physical devices | 50 operations each | Per-device encrypted queue/export counters |
| Mixed entity coverage | Calendar, marks, attendance, fixtures | Server/RLS/entity-specific diff |
| Data loss | Zero acknowledged edits missing | Before/after operation-ID audit |
| Duplicate operations | Zero duplicate `op_id` effects | Server op log and change log counts |
| Tenant isolation | No cross-school reads/writes | Negative authenticated tenant probes |
| Conflict handling | Conflicting edits visible for resolution | Original local op + server version |
| Pull cursor | Monotonically increasing | Per-transaction cursor log |
| Interrupted upload | Queue survives lost ACK | Rerun with identical operation ID |
| 2G term sync size | Under 5 MB | Network byte capture, payload and gzip statistics |
| Android release size | Under 25 MB | Signed release APK size |
| Android 8 / 2 GB cold start | Under 3 seconds | Repeated measured cold-start trials |

**Pass criteria:** all mandatory rows pass with reproducible logs and no fabricated timestamps. Each failure must create an issue with test steps, device/OS, client commit and server version; a rerun is required after fixes.

## Current implementation gap

At 2026-11-08, only **calendar-event** push/pull exists end to end in Phase 1. Marks, attendance and fixtures do **not** yet have reviewed offline write RPCs, conflict policies or matching client UIs; therefore the mixed 100-operation acceptance cannot currently pass. Room stores calendar JSON without at-rest database encryption; avoid confidential events/PII. Neither hosted test-user login nor the real 72-hour physical-device run is verified.

Existing logical-only integration result (not physical acceptance):
https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42452783861
