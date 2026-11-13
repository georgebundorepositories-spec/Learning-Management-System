# Phase 21 — Assign LMS user roles in Supabase

## One source of truth

The LMS designation is the enum field public.users.role, NOT auth.users.role and NOT user-editable metadata. The existing enum supports admin (school admin), head, teacher, student, parent and bursar. **There is not yet a platform-wide master administrator designation**.

The project owner can open the [Supabase Table Editor](https://supabase.com/dashboard/project/mezbtcdkmvtkjxdmmhbw/editor), navigate to public.users, locate the account and choose the role in that row. An Auth account must already exist with the matching public.users.id (the UUID in auth.users); editing Authentication > Users > role is wrong because that is Supabase's Postgres auth role.

The school administrator also has a simple LMS interface at /dashboard/users, under "Assign a member's role". This interface deliberately offers only teacher, student, parent and bursar and excludes the current administrator and any admin/head targets. Another admin/head may only be provisioned through the trusted Supabase project owner or verified server-side provisioning.

## Safety

The Phase 21 database triggers enforce these rules at the data layer, regardless of browser UI:

- Same-school active school administrator only. Head, teacher, parent, student and bursar cannot elevate or assign roles using the browser API.
- The school administrator cannot assign admin/head, change their own designation, or change an existing admin/head.
- A project owner or controlled service-role server operation can assign an admin/head, but the last active school administrator cannot be removed without another administrator.
- A school administrator changing a teacher's designation must first reassign live teaching duties. Trusted project-owner/service-role provisioning may revoke a teacher's designation immediately without destroying historic class links; restoring the teacher role can reactivate those assignments, so review them before restoration.
- Changing learner or guardian roles archives incompatible profile records and revokes guardian connections rather than silently granting access to children's data. A newly assigned learner has no automatic class. Former guardian links are not restored automatically.
- A role switch creates/reactivates the appropriate school profile (staff, student, parent), and writes an audit_log entry of previous and new role. Changes are transactional; a failing update rolls back its profile and log changes.
- Existing web guards, Android online role verification and Supabase RLS derive authorisation from the current active public.users.role. Private account tools still require online verification and school relationships.
- **No direct master-admin or cross-school privileges are created.**

## Testing and deployment

The canonical migration and rollback-only pgTAP tests are in this PR. Do not deploy the new school-admin role-change form without migrating the hosted database and verifying role changes with real authenticated database sessions. Existing Supabase project-owner Table Editor edits are already possible. Do not treat fictional fixture accounts as real users.

This phase does not resolve Vercel account login, production releases, real teacher/parent onboarding or safeguarding approvals.
