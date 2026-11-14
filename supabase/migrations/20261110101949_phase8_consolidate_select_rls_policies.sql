-- Merge equivalent OR-combined SELECT policies into one per table.
-- Avoids multiple permissive policies without widening authorised records.

drop policy if exists users_read on public.users;
drop policy if exists users_read_assigned_learners on public.users;
create policy users_read on public.users for select to authenticated using (
  (
    id=(select auth.uid()) and is_active and deleted_at is null
  ) or (
    (select app_private.is_school_admin())
    and school_id=(select app_private.current_school_id())
  ) or (
    role='student' and app_private.can_view_student(id)
  )
);

drop policy if exists student_profiles_read on public.student_profiles;
drop policy if exists student_profiles_teacher_read on public.student_profiles;
create policy student_profiles_read on public.student_profiles for select to authenticated using (
 deleted_at is null and (
   (
     (select app_private.current_school_id()) is not null
     and (
       user_id=(select auth.uid())
       or (select app_private.is_guardian_of(student_profiles.user_id))
       or (
         (select app_private.is_school_admin()) and user_id in (
           select u.id from public.users u
           where u.school_id=(select app_private.current_school_id())
         )
       )
     )
   ) or app_private.can_view_student(user_id)
 )
);

drop policy if exists assessments_staff_read on public.assessments;
drop policy if exists assessments_student_read on public.assessments;
create policy assessments_staff_read on public.assessments for select to authenticated using (
 deleted_at is null and school_id=(select app_private.current_school_id())
 and (
   app_private.can_manage_assessment(class_id,subject_id)
   or (
     exists(
       select 1 from public.users actor
       where actor.id=(select auth.uid()) and actor.role in ('student','parent')
         and actor.is_active and actor.deleted_at is null
     )
     and exists(
       select 1 from public.student_profiles sp
       where sp.class_id=assessments.class_id and sp.deleted_at is null
         and app_private.can_view_student(sp.user_id)
     )
   )
 )
);

drop policy if exists assessment_results_staff_read on public.assessment_results;
drop policy if exists assessment_results_private_read on public.assessment_results;
create policy assessment_results_staff_read on public.assessment_results
 for select to authenticated using (
  exists(
    select 1 from public.assessments a
    where a.id=assessment_id and a.deleted_at is null
      and a.school_id=(select app_private.current_school_id())
      and app_private.can_manage_assessment(a.class_id,a.subject_id)
  ) or (
    exists(
      select 1 from public.users actor
      where actor.id=(select auth.uid()) and actor.role in ('student','parent')
        and actor.is_active and actor.deleted_at is null
    )
    and app_private.can_view_student(student_user_id)
    and exists(
      select 1 from public.assessments a
      where a.id=assessment_id and a.deleted_at is null
        and a.school_id=(select app_private.current_school_id())
    )
  )
);
