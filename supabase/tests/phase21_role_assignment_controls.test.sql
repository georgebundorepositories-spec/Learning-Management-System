-- Phase 21: role designation changes are school-scoped, audited, and safe.
-- Synthetic transactions only; no hosted school people are modified.
begin;
create extension if not exists pgtap with schema extensions;
set local search_path=public,extensions,pg_catalog;

insert into public.schools(id,name) values
 ('a1111111-1111-4111-8111-111111111111','Role Test School A'),
 ('b2222222-2222-4222-8222-222222222222','Role Test School B');
insert into auth.users(id,instance_id,aud,role,email,encrypted_password)
select id,'00000000-0000-0000-0000-000000000000'::uuid,
 'authenticated','authenticated','role-test-'||right(id::text,4)||'@example.invalid','not_a_real_password'
from unnest(array[
 'd0000000-0000-4000-8000-000000000001'::uuid,
 'd0000000-0000-4000-8000-000000000002'::uuid,
 'd0000000-0000-4000-8000-000000000003'::uuid,
 'd0000000-0000-4000-8000-000000000004'::uuid,
 'd0000000-0000-4000-8000-000000000005'::uuid,
 'd0000000-0000-4000-8000-000000000006'::uuid
]) as id;
insert into public.users(id,school_id,phone,full_name,role) values
 ('d0000000-0000-4000-8000-000000000001','a1111111-1111-4111-8111-111111111111','+263771900001','Fixture administrator A','admin'),
 ('d0000000-0000-4000-8000-000000000002','a1111111-1111-4111-8111-111111111111','+263771900002','Fixture head A','head'),
 ('d0000000-0000-4000-8000-000000000003','a1111111-1111-4111-8111-111111111111','+263771900003','Fixture student A','student'),
 ('d0000000-0000-4000-8000-000000000004','a1111111-1111-4111-8111-111111111111','+263771900004','Fixture teacher A','teacher'),
 ('d0000000-0000-4000-8000-000000000005','a1111111-1111-4111-8111-111111111111','+263771900005','Fixture parent A','parent'),
 ('d0000000-0000-4000-8000-000000000006','b2222222-2222-4222-8222-222222222222','+263771900006','Fixture administrator B','admin');
insert into public.student_profiles(user_id) values ('d0000000-0000-4000-8000-000000000003');
insert into public.parent_profiles(user_id) values ('d0000000-0000-4000-8000-000000000005');
insert into public.staff_profiles(user_id) values ('d0000000-0000-4000-8000-000000000004');
insert into public.student_guardian_link(student_user_id,guardian_user_id)
values('d0000000-0000-4000-8000-000000000003','d0000000-0000-4000-8000-000000000005');

select plan(29);
select ok(exists(select 1 from pg_trigger
 where tgrelid='public.users'::regclass and tgname='trg_users_designation_change'
 and not tgisinternal),'role update trigger installed');
select ok(exists(select 1 from pg_trigger
 where tgrelid='public.users'::regclass and tgname='trg_users_role_provision'
 and not tgisinternal),'insertion guard installed');
select ok(not has_function_privilege('authenticated',
 'app_private.control_user_designation()','EXECUTE'),
 'trigger function cannot be called directly from the Data API');
select ok((select relrowsecurity from pg_class where oid='public.users'::regclass),
 'users table RLS remains enabled');

set local role authenticated;
select set_config('request.jwt.claim.sub','',true);
select throws_ok(
 $$insert into public.users(id,school_id,phone,full_name,role)
 values('d0000000-0000-4000-8000-000000000077',
  'a1111111-1111-4111-8111-111111111111',
  '+263771900077','Unverified Account','admin')$$,
 '42501',null,'missing actor JWT cannot provision roles');
select set_config('request.jwt.claim.sub','d0000000-0000-4000-8000-000000000002',true);
select throws_ok(
 $$update public.users set role='admin' where id='d0000000-0000-4000-8000-000000000004'$$,
 '42501',null,'head cannot promote another user through UPDATE');

select set_config('request.jwt.claim.sub','d0000000-0000-4000-8000-000000000001',true);
select throws_ok(
 $$update public.users set role='student' where id='d0000000-0000-4000-8000-000000000001'$$,
 '42501',null,'admin cannot change own role from browser');
select throws_ok(
 $$update public.users set role='admin' where id='d0000000-0000-4000-8000-000000000003'$$,
 '42501',null,'school admin cannot manufacture another admin');
select throws_ok(
 $$update public.users set role='head' where id='d0000000-0000-4000-8000-000000000003'$$,
 '42501',null,'school admin cannot manufacture a school head');
with attempted_cross_school_update as (
 update public.users set role='teacher'
 where id='d0000000-0000-4000-8000-000000000006'
 returning id
)
select is((select count(*) from attempted_cross_school_update),0::bigint,
 'cross school update blocked by RLS');
select lives_ok(
 $$update public.users set role='teacher' where id='d0000000-0000-4000-8000-000000000003'$$,
 'admin can change an unassigned learner into teacher');

reset role;
select is((select role::text from public.users
 where id='d0000000-0000-4000-8000-000000000003'),'teacher','database stores new role');
select ok(exists(select 1 from public.staff_profiles
 where user_id='d0000000-0000-4000-8000-000000000003' and deleted_at is null),
 'new teacher receives active staff profile');
select ok(exists(select 1 from public.student_profiles
 where user_id='d0000000-0000-4000-8000-000000000003' and deleted_at is not null),
 'previous learner profile archived');
select ok(exists(select 1 from public.student_guardian_link
 where student_user_id='d0000000-0000-4000-8000-000000000003' and deleted_at is not null),
 'previous family connection revoked');
select ok(exists(select 1 from public.audit_log
 where entity_id='d0000000-0000-4000-8000-000000000003'
 and action='user_role_changed'
 and diff->>'previous_role'='student' and diff->>'new_role'='teacher'),
 'role transition is recorded in audit log');

insert into public.classes(id,school_id,name,teacher_id)
values('c4444444-4444-4444-8444-444444444444',
 'a1111111-1111-4111-8111-111111111111','Test Class',
 'd0000000-0000-4000-8000-000000000004');
set local role authenticated;
select set_config('request.jwt.claim.sub','d0000000-0000-4000-8000-000000000001',true);
select throws_ok(
 $$update public.users set role='parent' where id='d0000000-0000-4000-8000-000000000004'$$,
 '23514',null,'cannot silently discard assigned teacher duties');
reset role;

-- Project-owner SQL Editor access can assign the high-trust head designation.
select set_config('request.jwt.claim.sub','',true);
select lives_ok(
 $$update public.users set role='head' where id='d0000000-0000-4000-8000-000000000005'$$,
 'project owner can assign head explicitly');
select ok(exists(select 1 from public.staff_profiles
 where user_id='d0000000-0000-4000-8000-000000000005' and deleted_at is null),
 'trusted head assignment creates staff profile');
select ok(exists(select 1 from public.student_guardian_link
 where guardian_user_id='d0000000-0000-4000-8000-000000000005' and deleted_at is not null),
 'old guardian links never follow a new designation');
-- A role change must archive links permanently until newly verified by
-- the school administrator. Ordinary parents can never select archived links.
select lives_ok(
 $role_statement$update public.users set role='student' where id='d0000000-0000-4000-8000-000000000003'$role_statement$,
 'trusted provisioning restores student role without prior class');
select lives_ok(
 $role_statement$update public.users set role='parent' where id='d0000000-0000-4000-8000-000000000005'$role_statement$,
 'trusted provisioning restores parent designation without family privileges');

set local role authenticated;
select set_config('request.jwt.claim.sub','d0000000-0000-4000-8000-000000000005',true);
select is((select count(*) from public.student_guardian_link
 where guardian_user_id='d0000000-0000-4000-8000-000000000005'),0::bigint,
 'restored parent cannot see archived guardian link');
select set_config('request.jwt.claim.sub','d0000000-0000-4000-8000-000000000001',true);
select is((select count(*) from public.student_guardian_link
 where guardian_user_id='d0000000-0000-4000-8000-000000000005'),1::bigint,
 'same-school administrator may verify archived guardian link');
with reverified as (
 update public.student_guardian_link
 set deleted_at=null,is_primary=false,can_pay_fees=false
 where student_user_id='d0000000-0000-4000-8000-000000000003'
 and guardian_user_id='d0000000-0000-4000-8000-000000000005'
 and deleted_at is not null
 returning id
)
select is((select count(*) from reverified),1::bigint,
 'administrator can explicitly restore reverified archived relationship');
select set_config('request.jwt.claim.sub','d0000000-0000-4000-8000-000000000005',true);
select is((select count(*) from public.student_guardian_link
 where guardian_user_id='d0000000-0000-4000-8000-000000000005'),1::bigint,
 'guardian sees only freshly authorised active relationship');
select set_config('request.jwt.claim.sub','d0000000-0000-4000-8000-000000000001',true);
with revoked as (
 delete from public.student_guardian_link
 where guardian_user_id='d0000000-0000-4000-8000-000000000005'
 returning id
)
select is((select count(*) from revoked),1::bigint,
 'school administrator can revoke restored relationship');
select set_config('request.jwt.claim.sub','d0000000-0000-4000-8000-000000000005',true);
select is((select count(*) from public.student_guardian_link
 where guardian_user_id='d0000000-0000-4000-8000-000000000005'),0::bigint,
 'guardian instantly loses access after final revocation');
reset role;
-- Trusted SQL Editor statements have no signed-in actor JWT.
select set_config('request.jwt.claim.sub','',true);

select throws_ok(
 $role_statement$update public.users set role='teacher' where id='d0000000-0000-4000-8000-000000000001'$role_statement$,
 '23514',null,'the last active administrator cannot be demoted');

select * from finish();
rollback;
