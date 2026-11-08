-- Live hosted RLS canary against locked, fictional accounts ONLY.
-- All changes, including temporarily toggled is_active and rejected op logs,
-- are wrapped in a transaction and ROLLED BACK. No persistent record changes.
begin;
update public.users
set is_active=true
where id in (
 '20000000-0000-4000-8000-000000000001',
 '20000000-0000-4000-8000-000000000003',
 '20000000-0000-4000-8000-000000000004',
 '20000000-0000-4000-8000-000000000103'
) and phone like 'DEMO-NO-PHONE-%'
and school_id in
 ('10000000-0000-4000-8000-000000000001','10000000-0000-4000-8000-000000000002');
set local role authenticated;
do $verify$
declare
  result jsonb;
  event_count integer;
begin
  perform set_config('request.jwt.claim.sub',
   '20000000-0000-4000-8000-000000000004',true);
  select count(*) into event_count from public.calendar_events;
  if event_count <> 0 then
    raise exception 'Same-school teacher can read unauthorized events: %',event_count;
  end if;
  result := public.sync_calendar_operation(
    'ci-demo-internal-canary',
    '50000000-0000-4000-8000-000000000001'::uuid,
    '30000000-0000-4000-8000-000000000001'::uuid,
    'insert',
    '{"title":"CI collision probe","start_at":"2026-12-02T09:00:00Z","end_at":"2026-12-02T10:00:00Z"}'::jsonb,
    null,now()
  );
  if result->>'result' is distinct from 'rejected'
     or (result ? 'server_row' and result->'server_row' is distinct from 'null'::jsonb) then
    raise exception 'Same-school collision exposed another teacher event: %',result;
  end if;

  perform set_config('request.jwt.claim.sub',
   '20000000-0000-4000-8000-000000000103',true);
  select count(*) into event_count from public.calendar_events;
  if event_count <> 3 then
    raise exception 'Second-school teacher sees unexpected events %',event_count;
  end if;
  result := public.sync_calendar_operation(
    'ci-demo-internal-canary',
    '50000000-0000-4000-8000-000000000002'::uuid,
    '30000000-0000-4000-8000-000000000001'::uuid,
    'update','{"title":"CI cross-school probe"}'::jsonb,1,now()
  );
  if result->>'result' is distinct from 'rejected' or
    (result ? 'server_row' and result->'server_row' is distinct from 'null'::jsonb) then
    raise exception 'Cross-school event details disclosed: %',result;
  end if;

  perform set_config('request.jwt.claim.sub',
   '20000000-0000-4000-8000-000000000003',true);
  select count(*) into event_count from public.calendar_events;
  if event_count <> 3 then
    raise exception 'Teacher owner should see three authored events, saw %',event_count;
  end if;

  perform set_config('request.jwt.claim.sub',
   '20000000-0000-4000-8000-000000000001',true);
  select count(*) into event_count from public.calendar_events;
  if event_count <> 8 then
    raise exception 'School admin should see eight school events, saw %',event_count;
  end if;
  raise notice 'PASS: live school isolation, owner-only reads, admin visibility and no conflict row exfiltration';
end
$verify$;
rollback;
