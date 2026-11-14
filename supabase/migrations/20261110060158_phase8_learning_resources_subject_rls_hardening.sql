
-- Only a subject's assigned teaching staff and school administrators may publish school resources.
create or replace function app_private.can_publish_subject(p_subject_id uuid)
returns boolean language sql stable security definer set search_path=''
as $$
select exists (
 select 1 from public.users actor
 join public.subjects s on s.id=p_subject_id and s.school_id=actor.school_id
 where actor.id=(select auth.uid()) and actor.is_active and actor.deleted_at is null
 and s.deleted_at is null
 and (
  actor.role in ('admin','head')
  or (actor.role='teacher' and (
    exists (
      select 1 from public.class_subject_teacher cst
      join public.classes c on c.id=cst.class_id
      where cst.teacher_id=actor.id and cst.subject_id=s.id
       and cst.deleted_at is null and c.school_id=s.school_id and c.deleted_at is null
    ) or exists (
      select 1 from public.classes c
      where c.teacher_id=actor.id and c.school_id=s.school_id and c.deleted_at is null
    )
  ))
 )
) $$;
revoke all on function app_private.can_publish_subject(uuid) from public;
grant execute on function app_private.can_publish_subject(uuid) to authenticated;

drop policy if exists learning_resources_insert on public.learning_resources;
create policy learning_resources_insert on public.learning_resources
for insert to authenticated with check (
 school_id=(select app_private.current_school_id())
 and created_by=(select auth.uid()) and deleted_at is null
 and app_private.can_publish_subject(subject_id)
);
drop policy if exists learning_resources_update on public.learning_resources;
create policy learning_resources_update on public.learning_resources
for update to authenticated using (
 school_id=(select app_private.current_school_id()) and deleted_at is null
 and (created_by=(select auth.uid()) or (select app_private.is_school_admin()))
) with check (
 school_id=(select app_private.current_school_id())
 and (created_by=(select auth.uid()) or (select app_private.is_school_admin()))
 and app_private.can_publish_subject(subject_id)
);
revoke update on public.learning_resources from authenticated;
grant update(title,description,resource_url,subject_id,updated_at,deleted_at)
 on public.learning_resources to authenticated;
