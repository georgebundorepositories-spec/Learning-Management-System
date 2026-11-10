# Role-based portals and school member invitations
Date: 10 November 2026 — development preview only

## Designations
- Teacher: teaching classes, staff lesson planning, curriculum guides, attendance, learning resources and authorised coursework; no school account invitation or parent directory.
- Learner (student): own class, 75 offline lesson library, practice, assigned coursework and private results/progress. No teacher planning, directory, register editor or school reports.
- Parent: linked children, private results/progress, notices, school fees and shared learning resources. No lesson planning, class attendance editor or learner directory.
- Administrator and head: school oversight. Only **administrator** may invite new teacher, learner and parent identities.

The Next.js dashboard uses `apps/web/lib/portal-access.ts`; direct route permission is checked server-side. Android uses `WorkspaceNavigation.tabs(role)` and the staff-only `TeacherPlanningPage`. Navigation filtering is not a security boundary; school-aware Supabase RLS and authenticated server operations remain mandatory.

## Provisioning two real member accounts
The administrator opens `/dashboard/users` and sends two separate email invitations. Each requires name, valid email, unique Zimbabwe mobile number and teacher/student/parent designation. The Edge Function `invite-school-member` verifies caller JWT and active administrator role in the database, derives the school ID server-side, verifies requested class/child links, creates the Auth invite, then records the school role and profile, plus an audit trail. Passwords and Supabase secret keys are never returned to browsers. The invitee sets their own credentials through the email link.

**No credentials or actual identities are invented.** This change adds the real workflow but cannot choose recipient names, emails or phones for the administrator. Existing 40 inactive fictional Auth users are not enabled.

SMTP and Supabase Auth Site URL need approval before inviting real children or adults. The server-side invite flow will be visible only when the web deployment has been approved and configured.

## Testing / release boundaries
- Unit tests assert role visibility.
- Staff planning pages check role on Android and web.
- The live Edge Function is JWT-protected and independently checks `users.role`, `is_active`, `school_id` and membership.
- Physical device, real invitation email, learner-guardian linkage and end-to-end school access must be verified before production.
