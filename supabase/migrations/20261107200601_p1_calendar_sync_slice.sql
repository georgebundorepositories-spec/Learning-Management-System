-- Phase 1 initial calendar sync vertical slice. NOT DEPLOYED TO HOSTED LMS Omni.
-- This migration is exercised on the disposable CI database before release.
-- Changes to the Part C/E sample: no dynamic table selection, server-derived school
-- identity, compare-and-swap versions (never trust client_ts for conflict ordering),
-- append-only integer change cursors, and server-owned approval fields.

create type public.calendar_category as enum
  ('academic','exam','sports','cultural','admin','holiday','meeting','trip','other');
create type public.visibility_scope as enum
  ('all','staff','students','parents','admin','custom');
create type public.approval_status as enum
  ('draft','pending','approved','rejected');

create table public.calendar_events (
  id uuid primary key default gen_random_uuid(),
  school_id uuid not null references public.schools(id) on delete cascade,
  term_id uuid references public.terms(id),
  title text not null check (char_length(title) between 1 and 200),
  title_i18n jsonb,
  description text,
  category public.calendar_category not null default 'other',
  start_at timestamptz not null,
  end_at timestamptz not null,
  all_day boolean not null default false,
  location text,
  visibility public.visibility_scope not null default 'all',
  audience_roles text[],
  audience_class_ids uuid[],
  created_by uuid not null references public.users(id),
  approved_by uuid references public.users(id),
  approval_status public.approval_status not null default 'draft',
  approved_at timestamptz,
  rejection_reason text,
  recurrence_rule text,
  recurrence_parent_id uuid references public.calendar_events(id),
  attachments jsonb not null default '[]'::jsonb,
  colour text,
  is_published boolean not null default false,
  push_sent boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1,
  constraint event_time_range check (end_at >= start_at)
);
create index idx_cal_events_school_start on public.calendar_events(school_id,start_at)
  where deleted_at is null;
create index idx_cal_events_author on public.calendar_events(created_by,updated_at);
create trigger trg_calendar_events_version before update on public.calendar_events
  for each row execute function public.set_updated_at_and_version();

create table public.device_registry (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users(id) on delete cascade,
  device_id text not null check (char_length(device_id) between 1 and 128),
  platform text, app_version text, fcm_token text,
  last_seen timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1,
  unique(user_id,device_id)
);
create trigger trg_device_registry_version before update on public.device_registry
  for each row execute function public.set_updated_at_and_version();

create table public.sync_op_log (
  id uuid primary key default gen_random_uuid(),
  school_id uuid not null references public.schools(id),
  device_id text not null,
  user_id uuid not null references public.users(id),
  op_id uuid not null unique,
  entity text not null check (entity='calendar_events'),
  entity_id uuid not null,
  op_type text not null check (op_type in ('insert','update','delete')),
  client_ts timestamptz,
  server_ts timestamptz not null default now(),
  result text not null check (result in ('applied','conflict','rejected')),
  conflict_detail jsonb,
  server_row jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1
);
create index idx_sync_log_actor on public.sync_op_log(user_id,server_ts);
create index idx_sync_log_school on public.sync_op_log(school_id,server_ts);
create trigger trg_sync_op_log_version before update on public.sync_op_log
  for each row execute function public.set_updated_at_and_version();

create table public.audit_log (
  id uuid primary key default gen_random_uuid(),
  school_id uuid not null references public.schools(id),
  actor_id uuid references public.users(id),
  action text not null, entity text, entity_id uuid,
  diff jsonb, ip text, user_agent text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1
);
create index idx_audit_school_time on public.audit_log(school_id,created_at desc);
create trigger trg_audit_log_version before update on public.audit_log
  for each row execute function public.set_updated_at_and_version();

create table public.feature_flags (
  id uuid primary key default gen_random_uuid(),
  school_id uuid not null references public.schools(id) on delete cascade,
  key text not null,
  value jsonb not null default 'true'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1,
  unique(school_id,key)
);
create index idx_feature_flags_school on public.feature_flags(school_id);
create trigger trg_feature_flags_version before update on public.feature_flags
  for each row execute function public.set_updated_at_and_version();

-- Change log is append-only. The sequence is a *server* cursor; never use a device
-- timestamp or updated_at alone, which can skip tied values or skewed clocks.
create table public.sync_change_log (
  seq bigint generated always as identity primary key,
  school_id uuid not null references public.schools(id) on delete cascade,
  entity text not null check (entity='calendar_events'),
  entity_id uuid not null,
  row_data jsonb not null,
  recorded_at timestamptz not null default now()
);
create index idx_sync_change_school_seq on public.sync_change_log(school_id,seq);

-- SERIALISE SEQUENCE ALLOCATION PER SCHOOL UNTIL COMMIT. Without this lock,
-- two concurrent transactions could commit sequence 10 before sequence 9, and
-- a pull cursor of 10 would permanently skip record 9.
create function app_private.append_calendar_change()
returns trigger language plpgsql security definer set search_path='' as $$
begin
  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(new.school_id::text, 314159));
  insert into public.sync_change_log(school_id,entity,entity_id,row_data)
  values(new.school_id,'calendar_events',new.id,to_jsonb(new));
  return new;
end;
$$;
revoke all on function app_private.append_calendar_change() from public,anon,authenticated;
create trigger trg_calendar_change after insert or update on public.calendar_events
  for each row execute function app_private.append_calendar_change();

-- Hard deletes destroy tombstones and break offline clients.
create function app_private.prevent_calendar_hard_delete()
returns trigger language plpgsql set search_path='' as $$
begin
  raise exception 'Use soft delete for synced calendar events';
end;
$$;
revoke all on function app_private.prevent_calendar_hard_delete() from public,anon,authenticated;
create trigger trg_prevent_calendar_hard_delete before delete on public.calendar_events
  for each row execute function app_private.prevent_calendar_hard_delete();

-- Calendar term references must stay inside the same school.
create function app_private.check_calendar_tenant()
returns trigger language plpgsql security definer set search_path='' as $$
declare v_school uuid;
begin
  if new.term_id is not null then
    select school_id into v_school from public.terms where id=new.term_id;
    if v_school is distinct from new.school_id then
      raise exception 'Calendar term belongs to another school';
    end if;
  end if;
  return new;
end;
$$;
revoke all on function app_private.check_calendar_tenant() from public,anon,authenticated;
create trigger trg_calendar_tenant before insert or update on public.calendar_events
  for each row execute function app_private.check_calendar_tenant();

alter table public.calendar_events enable row level security;
alter table public.device_registry enable row level security;
alter table public.sync_op_log enable row level security;
alter table public.audit_log enable row level security;
alter table public.feature_flags enable row level security;
alter table public.sync_change_log enable row level security;

-- Phase 1: authors may see their drafts, heads/admins can see school events.
-- Read policies for students/parents require a reviewed visibility model in Phase 2.
create policy calendar_read on public.calendar_events for select to authenticated
using (
  school_id=(select app_private.current_school_id()) and deleted_at is null
  and ((select app_private.is_school_admin()) or
    ((select app_private.current_role())='teacher' and created_by=(select auth.uid())))
);
grant select on public.calendar_events to authenticated;

create policy device_own_read on public.device_registry for select to authenticated
using(user_id=(select auth.uid()) and (select app_private.current_school_id()) is not null);
create policy device_own_insert on public.device_registry for insert to authenticated
with check(user_id=(select auth.uid()) and (select app_private.current_school_id()) is not null);
create policy device_own_update on public.device_registry for update to authenticated
using(user_id=(select auth.uid()) and (select app_private.current_school_id()) is not null)
with check(user_id=(select auth.uid()) and (select app_private.current_school_id()) is not null);
grant select,insert,update on public.device_registry to authenticated;

create policy sync_log_own_read on public.sync_op_log for select to authenticated
using(user_id=(select auth.uid()) and school_id=(select app_private.current_school_id()));
grant select on public.sync_op_log to authenticated;

create policy audit_admin_read on public.audit_log for select to authenticated
using(school_id=(select app_private.current_school_id()) and (select app_private.is_school_admin()));
grant select on public.audit_log to authenticated;

create policy flags_admin_read on public.feature_flags for select to authenticated
using(school_id=(select app_private.current_school_id()) and (select app_private.is_school_admin()));
grant select on public.feature_flags to authenticated;

create policy sync_change_own_read on public.sync_change_log for select to authenticated
using (
  school_id=(select app_private.current_school_id()) and
  ((select app_private.is_school_admin()) or
   ((select app_private.current_role())='teacher' and
    row_data->>'created_by'=(select auth.uid())::text))
);
grant select on public.sync_change_log to authenticated;

-- Write operation is deliberately in an UNEXPOSED schema.
-- Public wrapper is SECURITY INVOKER. Each operation validates the actor,
-- tenant, role, entity ID and version before the privileged write.
create function app_private.apply_calendar_op(
  p_device_id text, p_op_id uuid, p_entity_id uuid, p_op_type text,
  p_payload jsonb, p_base_version integer, p_client_ts timestamptz
) returns jsonb language plpgsql security definer set search_path='' as $$
declare
  v_user uuid := auth.uid();
  v_school uuid;
  v_role public.user_role;
  v_current public.calendar_events%rowtype;
  v_saved public.calendar_events%rowtype;
  v_op uuid;
  v_prior public.sync_op_log%rowtype;
  v_result text := 'rejected';
  v_detail jsonb := null;
  v_title text;
  v_start timestamptz;
  v_end timestamptz;
  v_category public.calendar_category;
begin
  select school_id,role into v_school,v_role from public.users
   where id=v_user and is_active and deleted_at is null;
  if v_school is null or v_role not in ('teacher','admin','head') then
    return jsonb_build_object('result','rejected','conflict_detail',jsonb_build_object('reason','forbidden'));
  end if;
  if p_device_id is null or char_length(p_device_id) not between 1 and 128
     or p_op_id is null or p_entity_id is null
     or p_op_type not in ('insert','update','delete')
     or p_payload is null or jsonb_typeof(p_payload)<>'object'
     or pg_catalog.octet_length(p_payload::text)>65536 then
    return jsonb_build_object('result','rejected','conflict_detail',jsonb_build_object('reason','invalid_request'));
  end if;

  -- This unique claim serialises concurrent requests with the same op_id.
  insert into public.sync_op_log
    (school_id,device_id,user_id,op_id,entity,entity_id,op_type,client_ts,result,conflict_detail)
  values(v_school,p_device_id,v_user,p_op_id,'calendar_events',p_entity_id,p_op_type,
    p_client_ts,'rejected','{"reason":"in_progress"}'::jsonb)
  on conflict(op_id) do nothing returning op_id into v_op;

  if v_op is null then
    select * into v_prior from public.sync_op_log where op_id=p_op_id;
    if v_prior.user_id is distinct from v_user or v_prior.entity_id is distinct from p_entity_id
       or v_prior.op_type is distinct from p_op_type then
      return jsonb_build_object('result','rejected','conflict_detail',jsonb_build_object('reason','op_id_reused'));
    end if;
    return jsonb_build_object('result',v_prior.result,'server_row',v_prior.server_row,
      'conflict_detail',v_prior.conflict_detail);
  end if;

  select * into v_current from public.calendar_events where id=p_entity_id for update;
  if v_current.id is not null and v_current.school_id is distinct from v_school then
    v_detail := jsonb_build_object('reason','not_found');
  elsif p_op_type='insert' then
    if v_current.id is not null then
      v_result := 'conflict';
      v_detail := jsonb_build_object('reason','already_exists');
      v_saved := v_current;
    else
      v_title := pg_catalog.btrim(p_payload->>'title');
      if v_title is null or char_length(v_title) not between 1 and 200
         or p_payload->>'start_at' is null or p_payload->>'end_at' is null
         or coalesce(p_payload->>'category','other') not in
           ('academic','exam','sports','cultural','admin','holiday','meeting','trip','other') then
        v_detail := jsonb_build_object('reason','invalid_event');
      else
        v_category := coalesce((p_payload->>'category')::public.calendar_category,'other');
        v_start := (p_payload->>'start_at')::timestamptz;
        v_end := (p_payload->>'end_at')::timestamptz;
        if v_end<v_start then
          v_detail := jsonb_build_object('reason','invalid_time_range');
        else
          insert into public.calendar_events
            (id,school_id,title,description,category,start_at,end_at,location,
             all_day,created_by,approval_status)
          values(p_entity_id,v_school,v_title,p_payload->>'description',v_category,
            v_start,v_end,p_payload->>'location',
            coalesce((p_payload->>'all_day')::boolean,false),v_user,'pending')
          returning * into v_saved;
          v_result := 'applied';
        end if;
      end if;
    end if;
  elsif v_current.id is null or v_current.deleted_at is not null then
    v_detail := jsonb_build_object('reason','not_found');
  elsif v_role='teacher' and v_current.created_by is distinct from v_user then
    v_detail := jsonb_build_object('reason','forbidden');
  elsif v_role='teacher' and v_current.approval_status='approved' then
    v_result := 'conflict';
    v_detail := jsonb_build_object('reason','server_wins');
    v_saved := v_current;
  elsif p_base_version is distinct from v_current.sync_version then
    v_result := 'conflict';
    v_detail := jsonb_build_object('reason','stale_version',
      'expected',p_base_version,'actual',v_current.sync_version);
    v_saved := v_current;
  elsif p_op_type='delete' then
    update public.calendar_events set deleted_at=now() where id=p_entity_id
      returning * into v_saved;
    v_result := 'applied';
  else
    v_title := case when p_payload ? 'title' then pg_catalog.btrim(p_payload->>'title')
       else v_current.title end;
    if v_title is null or char_length(v_title) not between 1 and 200 then
      v_detail := jsonb_build_object('reason','invalid_title');
    else
      v_start := case when p_payload ? 'start_at'
        then (p_payload->>'start_at')::timestamptz else v_current.start_at end;
      v_end := case when p_payload ? 'end_at'
        then (p_payload->>'end_at')::timestamptz else v_current.end_at end;
      if v_end<v_start then
        v_detail := jsonb_build_object('reason','invalid_time_range');
      else
        update public.calendar_events set
          title=v_title,
          start_at=v_start,
          end_at=v_end,
          description=case when p_payload ? 'description' then p_payload->>'description' else description end,
          location=case when p_payload ? 'location' then p_payload->>'location' else location end
        where id=p_entity_id returning * into v_saved;
        v_result := 'applied';
      end if;
    end if;
  end if;

  update public.sync_op_log set result=v_result, conflict_detail=v_detail,
    server_row=case when v_saved.id is not null then to_jsonb(v_saved) else null end
  where op_id=p_op_id;
  if v_result='applied' then
    insert into public.audit_log(school_id,actor_id,action,entity,entity_id,diff)
    values(v_school,v_user,p_op_type,'calendar_events',p_entity_id,
      jsonb_build_object('op_id',p_op_id,'base_version',p_base_version));
  end if;
  return jsonb_build_object('result',v_result,
    'server_row',case when v_saved.id is not null then to_jsonb(v_saved) else null end,
    'conflict_detail',v_detail);
end;
$$;
revoke all on function app_private.apply_calendar_op(text,uuid,uuid,text,jsonb,integer,timestamptz)
  from public,anon,authenticated;
grant execute on function app_private.apply_calendar_op(text,uuid,uuid,text,jsonb,integer,timestamptz)
  to authenticated;

create function public.sync_calendar_operation(
  p_device_id text,p_op_id uuid,p_entity_id uuid,p_op_type text,
  p_payload jsonb,p_base_version integer,p_client_ts timestamptz
) returns jsonb language sql security invoker set search_path='' as $$
  select app_private.apply_calendar_op(p_device_id,p_op_id,p_entity_id,p_op_type,
     p_payload,p_base_version,p_client_ts);
$$;
revoke all on function public.sync_calendar_operation(text,uuid,uuid,text,jsonb,integer,timestamptz)
  from public,anon,authenticated;
grant execute on function public.sync_calendar_operation(text,uuid,uuid,text,jsonb,integer,timestamptz)
  to authenticated;

-- RLS-invoker pull. Each row is filtered by the sync_change_own_read policy.
-- Bigint sequence is the cursor, not an untrusted timestamp. Tombstones persist.
create function public.sync_pull_calendar(p_cursor bigint default 0,p_limit integer default 500)
returns jsonb language plpgsql stable security invoker set search_path='' as $$
declare
  v_cursor bigint := greatest(coalesce(p_cursor,0),0);
  v_limit integer := least(greatest(coalesce(p_limit,500),1),500);
  v_next bigint;
  v_rows jsonb;
  v_has_more boolean;
begin
  if (select app_private.current_school_id()) is null then
    raise exception 'Authentication required' using errcode='28000';
  end if;
  with batch as (
     select seq,row_data from public.sync_change_log
       where seq > v_cursor and entity='calendar_events'
       order by seq limit v_limit+1
  ),
  delivered as (
     select seq,row_data from batch order by seq limit v_limit
  )
  select coalesce(jsonb_agg(row_data || jsonb_build_object('_sync_seq',seq) order by seq),'[]'::jsonb),
      coalesce(max(seq),v_cursor),
      (select count(*) from batch)>v_limit
   into v_rows,v_next,v_has_more from delivered;
  return jsonb_build_object('changes',jsonb_build_object('calendar_events',v_rows),
     'next_cursor',v_next::text,'has_more',v_has_more);
end;
$$;
revoke all on function public.sync_pull_calendar(bigint,integer) from public,anon,authenticated;
grant execute on function public.sync_pull_calendar(bigint,integer) to authenticated;

-- Informational head watermark. Never advance a client's durable pull cursor
-- from the push response: only an acknowledged pull page may advance it.
create function public.sync_current_cursor()
returns text language sql stable security invoker set search_path='' as $$
  select coalesce(max(seq),0)::text from public.sync_change_log;
$$;
revoke all on function public.sync_current_cursor() from public,anon,authenticated;
grant execute on function public.sync_current_cursor() to authenticated;
