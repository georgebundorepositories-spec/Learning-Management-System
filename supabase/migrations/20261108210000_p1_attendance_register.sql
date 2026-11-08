-- Phase 1 attendance register, source-controlled to match the hosted schema.
-- Non-destructive: the hosted project may already have attendance provisioned.
-- Do not drop or silently rewrite existing school attendance data.
do $enum$
begin
  if not exists (select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
                 where n.nspname='public' and t.typname='attendance_status') then
    create type public.attendance_status as enum ('present','absent','late','excused');
  end if;
end $enum$;

create table if not exists public.attendance (
  id uuid primary key default gen_random_uuid(),
  school_id uuid not null references public.schools(id) on delete cascade,
  class_id uuid not null references public.classes(id) on delete cascade,
  student_user_id uuid not null references public.users(id) on delete cascade,
  date date not null,
  status public.attendance_status not null,
  marked_by uuid not null references public.users(id),
  notes text check (notes is null or char_length(notes)<=400),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1,
  constraint attendance_unique_class_student_day unique(class_id,student_user_id,date)
);
create index if not exists attendance_school_date_class_idx
 on public.attendance(school_id,date,class_id) where deleted_at is null;
create index if not exists attendance_student_date_idx
 on public.attendance(student_user_id,date desc) where deleted_at is null;

create or replace function app_private.can_manage_attendance(p_class_id uuid)
returns boolean language sql stable security definer set search_path='' as $function$
  select exists (
    select 1 from public.classes c
    join public.users actor on actor.id=(select auth.uid())
    where c.id=p_class_id and c.deleted_at is null
      and actor.school_id=c.school_id
      and actor.is_active and actor.deleted_at is null
      and (actor.role in ('admin','head')
        or (actor.role='teacher' and (
          c.teacher_id=actor.id or exists (
            select 1 from public.class_subject_teacher cst
            where cst.class_id=c.id and cst.teacher_id=actor.id
              and cst.deleted_at is null
          )
        ))
      )
  )
$function$;
revoke all on function app_private.can_manage_attendance(uuid) from public,anon;
grant execute on function app_private.can_manage_attendance(uuid) to authenticated;

create or replace function app_private.validate_attendance_record()
returns trigger language plpgsql security definer set search_path='' as $function$
begin
  if tg_op='UPDATE' and (
    new.school_id is distinct from old.school_id
    or new.class_id is distinct from old.class_id
    or new.student_user_id is distinct from old.student_user_id
    or new.date is distinct from old.date
    or new.deleted_at is distinct from old.deleted_at
  ) then
    raise exception 'Attendance identity and deletion cannot be changed';
  end if;
  if not exists (
    select 1 from public.student_profiles sp
    join public.users learner on learner.id=sp.user_id
    join public.classes c on c.id=sp.class_id
    where sp.user_id=new.student_user_id
      and sp.deleted_at is null and learner.deleted_at is null
      and learner.role='student' and learner.school_id=new.school_id
      and c.id=new.class_id and c.school_id=new.school_id
      and c.deleted_at is null
  ) then
    raise exception 'Learner is not enrolled in the selected school and class';
  end if;
  if not exists (
    select 1 from public.users actor
    where actor.id=new.marked_by and actor.school_id=new.school_id
      and actor.is_active and actor.deleted_at is null
      and actor.role in ('admin','head','teacher')
  ) then
    raise exception 'Attendance marker is not authorised school staff';
  end if;
  if tg_op='INSERT' then new.sync_version:=1; end if;
  return new;
end
$function$;
revoke all on function app_private.validate_attendance_record() from public,anon,authenticated;

drop trigger if exists attendance_validate_before_write on public.attendance;
create trigger attendance_validate_before_write
 before insert or update on public.attendance
 for each row execute function app_private.validate_attendance_record();
drop trigger if exists attendance_version_before_update on public.attendance;
create trigger attendance_version_before_update
 before update on public.attendance
 for each row execute function public.set_updated_at_and_version();

alter table public.attendance enable row level security;
drop policy if exists attendance_insert_staff on public.attendance;
create policy attendance_insert_staff on public.attendance
 for insert to authenticated
 with check (
  school_id=(select app_private.current_school_id())
  and marked_by=(select auth.uid())
  and app_private.can_manage_attendance(class_id)
 );
drop policy if exists attendance_select_auth on public.attendance;
create policy attendance_select_auth on public.attendance
 for select to authenticated
 using (
  deleted_at is null
  and school_id=(select app_private.current_school_id())
  and (app_private.can_manage_attendance(class_id)
    or student_user_id=(select auth.uid())
    or app_private.is_guardian_of(student_user_id))
 );
drop policy if exists attendance_update_staff on public.attendance;
create policy attendance_update_staff on public.attendance
 for update to authenticated
 using (
  deleted_at is null
  and school_id=(select app_private.current_school_id())
  and app_private.can_manage_attendance(class_id)
 )
 with check (
  deleted_at is null
  and school_id=(select app_private.current_school_id())
  and marked_by=(select auth.uid())
  and app_private.can_manage_attendance(class_id)
 );
revoke all on public.attendance from anon;
grant select,insert,update on public.attendance to authenticated;
