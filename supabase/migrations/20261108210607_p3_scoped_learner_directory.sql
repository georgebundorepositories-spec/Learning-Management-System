create or replace function app_private.can_view_student(p_student_id uuid)
returns boolean language sql stable security definer set search_path='' as $$
 select exists (
 select 1 from public.users actor
 join public.users learner on learner.id=p_student_id
 left join public.student_profiles sp on sp.user_id=learner.id and sp.deleted_at is null
 where actor.id=(select auth.uid()) and actor.is_active and actor.deleted_at is null
 and learner.school_id=actor.school_id and learner.is_active and learner.deleted_at is null and learner.role='student'
 and (
 actor.id=learner.id
 or (actor.role='parent' and app_private.is_guardian_of(learner.id))
 or (actor.role in ('admin','head'))
 or (actor.role='teacher' and sp.class_id is not null and app_private.can_manage_attendance(sp.class_id))
 )
 ) $$;
revoke all on function app_private.can_view_student(uuid) from public;
grant execute on function app_private.can_view_student(uuid) to authenticated;
create policy users_read_assigned_learners on public.users for select to authenticated
using (role='student' and app_private.can_view_student(id));
create policy student_profiles_teacher_read on public.student_profiles for select to authenticated
using (deleted_at is null and app_private.can_view_student(user_id));