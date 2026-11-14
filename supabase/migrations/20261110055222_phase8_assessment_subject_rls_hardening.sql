
-- Restrict academic marks to the class+subject actually taught, not just class membership.
create or replace function app_private.can_manage_assessment(p_class_id uuid,p_subject_id uuid)
returns boolean language sql stable security definer set search_path = ''
as $$
select exists (
  select 1
  from public.users actor
  join public.classes c on c.id=p_class_id and c.school_id=actor.school_id
  join public.subjects s on s.id=p_subject_id and s.school_id=c.school_id
  where actor.id=(select auth.uid())
    and actor.is_active and actor.deleted_at is null
    and c.deleted_at is null and s.deleted_at is null
    and (
      actor.role in ('admin','head')
      or (actor.role='teacher' and (
        c.teacher_id=actor.id
        or exists(
          select 1 from public.class_subject_teacher cst
          where cst.class_id=c.id and cst.subject_id=s.id
            and cst.teacher_id=actor.id and cst.deleted_at is null
        )
      ))
    )
) $$;
revoke all on function app_private.can_manage_assessment(uuid,uuid) from public;
grant execute on function app_private.can_manage_assessment(uuid,uuid) to authenticated;

drop policy if exists assessments_staff_read on public.assessments;
create policy assessments_staff_read on public.assessments for select to authenticated using (
 deleted_at is null and school_id=(select app_private.current_school_id())
 and app_private.can_manage_assessment(class_id,subject_id)
);
drop policy if exists assessments_student_read on public.assessments;
create policy assessments_student_read on public.assessments for select to authenticated using (
 deleted_at is null and school_id=(select app_private.current_school_id())
 and exists(select 1 from public.users actor where actor.id=(select auth.uid())
  and actor.role in ('student','parent') and actor.is_active and actor.deleted_at is null)
 and exists(select 1 from public.student_profiles sp
  where sp.class_id=assessments.class_id and sp.deleted_at is null
  and app_private.can_view_student(sp.user_id))
);
drop policy if exists assessments_staff_insert on public.assessments;
create policy assessments_staff_insert on public.assessments for insert to authenticated with check (
 school_id=(select app_private.current_school_id()) and created_by=(select auth.uid())
 and deleted_at is null and app_private.can_manage_assessment(class_id,subject_id)
);
drop policy if exists assessment_results_staff_read on public.assessment_results;
create policy assessment_results_staff_read on public.assessment_results for select to authenticated using (
 exists(select 1 from public.assessments a
  where a.id=assessment_id and a.deleted_at is null
  and a.school_id=(select app_private.current_school_id())
  and app_private.can_manage_assessment(a.class_id,a.subject_id))
);
drop policy if exists assessment_results_private_read on public.assessment_results;
create policy assessment_results_private_read on public.assessment_results for select to authenticated using (
 exists(select 1 from public.users actor where actor.id=(select auth.uid())
  and actor.role in ('student','parent') and actor.is_active and actor.deleted_at is null)
 and app_private.can_view_student(student_user_id)
 and exists(select 1 from public.assessments a where a.id=assessment_id
  and a.deleted_at is null and a.school_id=(select app_private.current_school_id()))
);
drop policy if exists assessment_results_staff_insert on public.assessment_results;
create policy assessment_results_staff_insert on public.assessment_results for insert to authenticated with check (
 marked_by=(select auth.uid())
 and exists(select 1 from public.assessments a
  join public.student_profiles sp on sp.class_id=a.class_id
    and sp.user_id=student_user_id and sp.deleted_at is null
  join public.users learner on learner.id=sp.user_id
  where a.id=assessment_id and a.deleted_at is null
    and a.school_id=(select app_private.current_school_id())
    and learner.school_id=a.school_id and learner.role='student'
    and learner.is_active and learner.deleted_at is null
    and app_private.can_manage_assessment(a.class_id,a.subject_id)
    and score<=a.max_score)
);
drop policy if exists assessment_results_staff_update on public.assessment_results;
create policy assessment_results_staff_update on public.assessment_results for update to authenticated using (
 exists(select 1 from public.assessments a where a.id=assessment_id
  and a.deleted_at is null and a.school_id=(select app_private.current_school_id())
  and app_private.can_manage_assessment(a.class_id,a.subject_id))
) with check (
 marked_by=(select auth.uid())
 and exists(select 1 from public.assessments a
  join public.student_profiles sp on sp.class_id=a.class_id
   and sp.user_id=student_user_id and sp.deleted_at is null
  join public.users learner on learner.id=sp.user_id
  where a.id=assessment_id and a.deleted_at is null
   and a.school_id=(select app_private.current_school_id())
   and learner.school_id=a.school_id and learner.role='student'
   and learner.is_active and learner.deleted_at is null
   and app_private.can_manage_assessment(a.class_id,a.subject_id)
   and score<=a.max_score)
);
-- A score edit cannot change the student or the assessment it belongs to.
revoke update on public.assessment_results from authenticated;
grant update(score,feedback,marked_by,updated_at) on public.assessment_results to authenticated;
