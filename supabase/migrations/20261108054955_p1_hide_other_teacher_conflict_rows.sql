-- Harden previously deployed sync RPC against same-school calendar content disclosure.
-- Keep invoker-only entrypoint and tenant-scoped RLS unchanged.
CREATE OR REPLACE FUNCTION app_private.apply_calendar_op(p_device_id text, p_op_id uuid, p_entity_id uuid, p_op_type text, p_payload jsonb, p_base_version integer, p_client_ts timestamp with time zone)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
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
      -- A teacher must not learn another teacher's event content by
      -- guessing an entity UUID and asking for a conflicting insert.
      if v_role = 'teacher' and v_current.created_by is distinct from v_user then
        v_detail := jsonb_build_object('reason','not_found');
      else
        v_result := 'conflict';
        v_detail := jsonb_build_object('reason','already_exists');
        v_saved := v_current;
      end if;
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
$function$

