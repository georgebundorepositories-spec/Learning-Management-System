# ZimSchool LMS — Attendance Register v0.4 Preview

## Source and scope

This slice follows the supplied **MASTER PROMPT v2.0**, Sections C.5
(`public.attendance` schema), G.2.5 (class picker, P/A/L/E, bulk present),
D.2 (role/RLS), and E.2 (attendance last-write-wins strategy).
The complete required offline-sync contract is **not yet implemented** for
attendance and is explicitly blocked by the Phase 1 acceptance gate.

## Implemented backend

- Hosted LMS Omni migration `p3_attendance_register`, schema version
  `20261108191305`, applied after isolated migration/pgTAP tests passed.
- `public.attendance` stores the school, class, enrolled learner, date,
  enum status, marking staff ID, optional short notes, audit timestamps
  and server-managed sync version.
- Unique `(class_id, student_user_id, date)` prevents duplicate daily entries;
  the POST `on_conflict` operation can update a prior mark in one transaction.
- RLS: heads/admins can mark their own school's classes; teachers only their
  assigned classes; learners read only their own attendance; parents/guardians
  read only a currently linked learner; foreign schools never see each other.
- Private SECURITY DEFINER functions validate active staff membership and
  return the two allowed roster fields (`student_user_id`, `display_name`).
  The public `get_attendance_roster` wrapper is SECURITY INVOKER.
- A private validation trigger verifies the learner is enrolled in the class
  and school, and prevents changing the school/class/learner/day identity.
- No public `anon` access, no authenticated DELETE rights.
- The hosted canary checked the verified demo school ECD A roster (3 fictional
  learners), cross-school invisibility, idempotent upsert and versioning
  inside a transaction with ROLLBACK. **0 attendance entries persisted**.
  No real learner records or passwords were introduced.
- Supabase security adviser showed only an existing Auth setting warning:
  leaked-password protection disabled. RLS is enabled for the new table.

## Implemented Android preview

- Role-gated `Attendance` tab in admin, head and teacher workspaces.
- Class selector restricted to school classes, and for teachers further
  restricted to classes with recorded teaching assignments.
- Date selector in `Africa/Harare`, roster rows with labelled P/A/L/E chips,
  mark-all-present action, selected/total counters and server-confirmed save.
- Uses a renewable Supabase user access token (never a service-role credential)
  and an explicitly allowlisted roster RPC; teacher/client cannot request a
  full user/contact directory.
- Only successful server acknowledgement shows `Saved`. A rejected or partial
  response does not silently clear the form.
- Roster names and attendance read results are held in memory. **No plaintext
  personally identifiable records are cached for offline attendance use**.

## Release boundary — important

The Android v0.4 attendance editor is **online-only** for mark submission.
If disconnected, no data is claimed to be saved; users must keep the form open
and retry. This is a deliberate limitation, not the specification's completed
offline flow. The full Phase 1 attendance sync queue, conflict policy,
encrypted local records and 72-hour physical-device convergence are pending.

Also unfinished: gradebook, marks/manual conflict reconciliation, homework
and assessment workflow, teacher resource upload, messaging, payments,
sports fixtures, enrolment/invitations and report cards.
Do not advertise these as working merely because some navigation exists.

## Tests

- Migration and 19 local pgTAP checks:
  [Supabase CI #42513290069](https://github.com/georgebundorepositories-spec/Learning-Management-System/actions/runs/42513290069).
- Cross-school/teacher/student/guardian read restrictions, forged
  inserts, malformed enrolment, duplicate saves and inactive staff checked.
- Android repository unit tests cover JWT headers, roster response,
  date scoping, bulk POST idempotent key and strict confirmation.
- The current Android workflow is available through the
  `feature/attendance-register-v1` branch Actions list. An APK is
  considered installable *only after* unit, build and emulator checks pass.

## Next gated integration

Extend the Phase 1 sync protocol with attendance operations using the
specification's **last-write-wins per (class, learner, date)** policy,
cryptographically protected local queue storage, protected conflict audit,
and 72-hour two-physical-device acceptance. Then implement grades/marks with
**manual** concurrent-edit conflict resolution, not unattended LWW.
