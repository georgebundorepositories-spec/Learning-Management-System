-- Phase 1: school-scoped, transactional calendar sync security and replay tests.
-- Synthetic fixtures are ROLLED BACK. Never run test data against hosted production.
begin;
create extension if not exists pgtap with schema extensions;
set local search_path=public,extensions;
select plan(13);

insert into public.schools(id,name) values
 ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1','P1 Synthetic School One'),
 ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1','P1 Synthetic School Two');

insert into auth.users(id,instance_id,aud,role,email,encrypted_password)
select id, '00000000-0000-0000-0000-000000000000'::uuid,
 'authenticated','authenticated', 'p1-'||right(id::text,6)||'@example.invalid','not-a-real-password'
from unnest(array[
 '00000000-0000-4000-8000-000000000011'::uuid,
 '00000000-0000-4000-8000-000000000012'::uuid,
 '00000000-0000-4000-8000-000000000013'::uuid,
 '00000000-0000-4000-8000-000000000014'::uuid
]) as id;
insert into public.users(id,school_id,phone,full_name,role) values
 ('00000000-0000-4000-8000-000000000011','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1','p1fixture11','School One Teacher','teacher'),
 ('00000000-0000-4000-8000-000000000012','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1','p1fixture12','School One Head','head'),
 ('00000000-0000-4000-8000-000000000013','bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1','p1fixture13','School Two Teacher','teacher'),
 ('00000000-0000-4000-8000-000000000014','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1','p1fixture14','School One Other Teacher','teacher');

set local role authenticated;
select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000011',true);

select is(
 (public.sync_calendar_operation(
   'device-a','11111111-1111-4111-8111-111111111111'::uuid,
   'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee1'::uuid,'insert',
   '{"title":"Offline science fair","start_at":"2026-12-04T09:00:00Z","end_at":"2026-12-04T11:00:00Z","school_id":"bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1"}'::jsonb,
   null,now())->>'result'), 'applied', 'Teacher may insert their own calendar event'
);
select is(
 (select school_id::text from public.calendar_events where id='eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee1'::uuid),
 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1', 'Forged school_id in payload ignored'
);
select is(
 (public.sync_calendar_operation(
   'device-a','11111111-1111-4111-8111-111111111111'::uuid,
   'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee1'::uuid,'insert',
   '{"title":"Offline science fair","start_at":"2026-12-04T09:00:00Z","end_at":"2026-12-04T11:00:00Z"}'::jsonb,
   null,now())->>'result'), 'applied', 'Replayed same op_id is idempotent'
);
select is((select count(*) from public.sync_change_log),1::bigint,
 'Idempotent replay created exactly one change entry');

select is(
 (public.sync_calendar_operation(
   'device-a','11111111-1111-4111-8111-111111111112'::uuid,
   'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee1'::uuid,'update',
   '{"title":"Stale local title"}'::jsonb,0,now())->>'result'),
 'conflict','Stale server version yields conflict, not silent overwrite'
);
select is(
 (public.sync_calendar_operation(
   'device-a','11111111-1111-4111-8111-111111111113'::uuid,
   'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee1'::uuid,'update',
   '{"title":"Updated title"}'::jsonb,1,now())->>'result'),
 'applied','Matching server version allows update'
);
select is((select sync_version from public.calendar_events
 where id='eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee1'::uuid),
 2,'Server increments authoritative sync_version');

select is(
 (public.sync_calendar_operation(
   'device-a','11111111-1111-4111-8111-111111111114'::uuid,
   'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee1'::uuid,'delete',
   '{}'::jsonb,2,now())->>'result'),
 'applied','Offline deletion produces tombstone'
);
select isnt((select deleted_at::text from public.calendar_events
 where id='eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee1'::uuid),
 null::text,'Deleted event retains server tombstone');
select is((select count(*) from public.sync_change_log),3::bigint,
 'Insert, update and delete produce three ordered entries');

select is((public.sync_pull_calendar(0,1)->>'has_more'),'true',
 'Pull has_more indicates a limited change page');

select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000013',true);
select is((public.sync_pull_calendar(0,500)->'changes'->'calendar_events'),
 '[]'::jsonb,'Teacher from another school cannot pull events');
select is(
 (public.sync_calendar_operation(
   'device-b','11111111-1111-4111-8111-111111111115'::uuid,
   'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee1'::uuid,'update',
   '{"title":"Cross-school modification"}'::jsonb,3,now())->>'result'),
 'rejected','Teacher cannot update another school event'
);

reset role;
select * from finish();
rollback;
