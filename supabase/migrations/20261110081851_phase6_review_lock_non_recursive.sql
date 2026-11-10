
-- Internal, authenticated-only lookup avoids cyclic RLS between answers and reviews.
create or replace function app_private.is_own_submission_reviewed(p_submission_id uuid)
returns boolean language sql stable security definer set search_path=''
as $$
  select exists(
    select 1 from public.assignment_reviews r
    join public.assignment_submissions s on s.id=r.submission_id
    join public.users actor on actor.id=(select auth.uid())
    where r.submission_id=p_submission_id
      and s.student_user_id=actor.id
      and s.school_id=actor.school_id
      and actor.is_active and actor.deleted_at is null
  )
$$;
revoke all on function app_private.is_own_submission_reviewed(uuid) from public;
grant execute on function app_private.is_own_submission_reviewed(uuid) to authenticated;
drop policy if exists assignment_submissions_student_update on public.assignment_submissions;
create policy assignment_submissions_student_update on public.assignment_submissions
for update to authenticated using(
 school_id=(select app_private.current_school_id())
 and student_user_id=(select auth.uid())
 and not app_private.is_own_submission_reviewed(id)
) with check(
 school_id=(select app_private.current_school_id())
 and student_user_id=(select auth.uid())
 and not app_private.is_own_submission_reviewed(id)
 and exists(
   select 1 from public.school_assignments a
   join public.student_profiles sp on sp.class_id=a.class_id
   where a.id=assignment_id and a.school_id=assignment_submissions.school_id
    and a.deleted_at is null and sp.user_id=student_user_id and sp.deleted_at is null
 )
);
