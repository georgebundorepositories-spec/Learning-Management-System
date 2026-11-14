-- Phase 11: permission revocation must take effect from the current DB role, not stale JWT metadata.
-- Existing role-scoped screens alone cannot protect direct Data API requests.
create or replace function app_private.is_guardian_of(student uuid)
returns boolean language sql stable security definer set search_path to ''
as $function$
 select exists (
  select 1 from public.student_guardian_link l
  join public.users s on s.id=l.student_user_id
  join public.users g on g.id=l.guardian_user_id
  where l.student_user_id=student and l.guardian_user_id=(select auth.uid())
    and s.school_id=g.school_id
    and s.role='student' and g.role='parent'
    and s.is_active and g.is_active
    and s.deleted_at is null and g.deleted_at is null and l.deleted_at is null
 )
$function$;

drop policy if exists teacher_lesson_plans_staff_read on public.teacher_lesson_plans;
create policy teacher_lesson_plans_staff_read on public.teacher_lesson_plans
 for select to authenticated using (
  deleted_at is null and school_id=(select app_private.current_school_id())
  and (
    ((select auth.uid())=teacher_id and exists(
      select 1 from public.users actor
      where actor.id=(select auth.uid()) and actor.role='teacher'
        and actor.is_active and actor.deleted_at is null
        and actor.school_id=teacher_lesson_plans.school_id
    ))
    or (select app_private.is_school_admin())
  )
 );
