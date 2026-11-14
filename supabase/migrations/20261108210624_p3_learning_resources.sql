create table if not exists public.learning_resources (
 id uuid primary key default gen_random_uuid(),
 school_id uuid not null references public.schools(id),
 subject_id uuid not null references public.subjects(id),
 title text not null check (char_length(title) between 3 and 180),
 description text not null default '',
 resource_url text check (resource_url is null or (char_length(resource_url)<=2048 and resource_url ~ '^https://')),
 created_by uuid not null references public.users(id),
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now(),
 deleted_at timestamptz
);
create index if not exists learning_resources_school_subject_idx on public.learning_resources(school_id,subject_id) where deleted_at is null;
alter table public.learning_resources enable row level security;
create policy learning_resources_read on public.learning_resources for select to authenticated using
(deleted_at is null and school_id=(select app_private.current_school_id()));
create policy learning_resources_insert on public.learning_resources for insert to authenticated with check
(school_id=(select app_private.current_school_id()) and created_by=(select auth.uid())
 and exists(select 1 from public.subjects s where s.id=subject_id and s.school_id=school_id)
 and exists(select 1 from public.users u where u.id=(select auth.uid()) and u.is_active and u.role in ('admin','head','teacher')));
create policy learning_resources_update on public.learning_resources for update to authenticated using
(school_id=(select app_private.current_school_id()) and
 (created_by=(select auth.uid()) or (select app_private.is_school_admin())))
with check (school_id=(select app_private.current_school_id()) and
 (created_by=(select auth.uid()) or (select app_private.is_school_admin()))
 and exists(select 1 from public.subjects s where s.id=subject_id and s.school_id=school_id));
grant select,insert,update on public.learning_resources to authenticated;