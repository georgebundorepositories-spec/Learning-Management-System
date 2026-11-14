-- Guard coursework list access by class AND subject for teachers.
-- Learners and linked guardians remain authorised to view their own class assignments.
drop policy if exists assignments_read_authorised on public.school_assignments;
create policy assignments_read_authorised on public.school_assignments
 for select to authenticated using (
  deleted_at is null
  and school_id=(select app_private.current_school_id())
  and (
    app_private.can_manage_assessment(class_id,subject_id)
    or (
      exists (
        select 1 from public.users actor
        where actor.id=(select auth.uid())
          and actor.school_id=school_assignments.school_id
          and actor.role in ('student','parent')
          and actor.is_active and actor.deleted_at is null
      )
      and exists(
        select 1 from public.student_profiles sp
        join public.users learner on learner.id=sp.user_id
        where sp.class_id=school_assignments.class_id
          and sp.deleted_at is null
          and learner.school_id=school_assignments.school_id
          and learner.role='student'
          and learner.is_active and learner.deleted_at is null
          and app_private.can_view_student(sp.user_id)
      )
    )
  )
);