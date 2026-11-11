-- Only approved public school events may be seen by learner/guardian accounts.
-- Preserve head/admin reads and teachers' own drafts; deny class-custom audiences.
drop policy if exists calendar_read on public.calendar_events;
create policy calendar_read on public.calendar_events for select to authenticated
using (
 school_id = (select app_private.current_school_id())
 and deleted_at is null
 and (
  (select app_private.is_school_admin())
  or ((select app_private.current_role()) = 'teacher' and created_by = (select auth.uid()))
  or (
   is_published = true and approval_status = 'approved'
   and (
    visibility = 'all'
    or (visibility = 'staff' and (select app_private.current_role()) in ('teacher','bursar','clerk'))
    or (visibility = 'students' and (select app_private.current_role()) = 'student')
    or (visibility = 'parents' and (select app_private.current_role()) = 'parent')
   )
   and (
    coalesce(cardinality(audience_roles),0)=0
    or (select app_private.current_role())::text = any(audience_roles)
   )
   and coalesce(cardinality(audience_class_ids),0)=0
  )
 )
);
