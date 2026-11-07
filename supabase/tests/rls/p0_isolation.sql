-- P0-T8: role and tenant RLS matrix with transactional, entirely synthetic fixtures.
-- Every fixture is rolled back. No production student information is involved.
begin;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions;

insert into public.schools (id, name) values
 ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa','Example School A'),
 ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb','Example School B');

insert into auth.users (id, instance_id, aud, role, email, encrypted_password)
select id, '00000000-0000-0000-0000-000000000000'::uuid, 'authenticated', 'authenticated',
       'test-' || right(id::text, 6) || '@example.invalid', 'fixture_not_a_password'
from unnest(array[
 '00000000-0000-4000-8000-000000000001'::uuid,
 '00000000-0000-4000-8000-000000000002'::uuid,
 '00000000-0000-4000-8000-000000000003'::uuid,
 '00000000-0000-4000-8000-000000000004'::uuid,
 '00000000-0000-4000-8000-000000000005'::uuid,
 '00000000-0000-4000-8000-000000000006'::uuid,
 '00000000-0000-4000-8000-000000000007'::uuid,
 '00000000-0000-4000-8000-000000000008'::uuid,
 '00000000-0000-4000-8000-000000000009'::uuid
]) as id;

insert into public.users (id, school_id, phone, full_name, role) values
 ('00000000-0000-4000-8000-000000000001','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa','fixture01','School A Admin','admin'),
 ('00000000-0000-4000-8000-000000000002','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa','fixture02','School A Teacher 1','teacher'),
 ('00000000-0000-4000-8000-000000000003','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa','fixture03','School A Teacher 2','teacher'),
 ('00000000-0000-4000-8000-000000000004','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa','fixture04','School A Student 1','student'),
 ('00000000-0000-4000-8000-000000000005','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa','fixture05','School A Student 2','student'),
 ('00000000-0000-4000-8000-000000000006','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa','fixture06','School A Parent','parent'),
 ('00000000-0000-4000-8000-000000000007','bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb','fixture07','School B Admin','admin'),
 ('00000000-0000-4000-8000-000000000008','bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb','fixture08','School B Student','student'),
 ('00000000-0000-4000-8000-000000000009','bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb','fixture09','School B Parent','parent');

insert into public.student_profiles (user_id, admission_no) values
 ('00000000-0000-4000-8000-000000000004','TEST-A-001'),
 ('00000000-0000-4000-8000-000000000005','TEST-A-002'),
 ('00000000-0000-4000-8000-000000000008','TEST-B-001');
insert into public.parent_profiles (user_id) values
 ('00000000-0000-4000-8000-000000000006'),
 ('00000000-0000-4000-8000-000000000009');
insert into public.student_guardian_link (student_user_id, guardian_user_id) values
 ('00000000-0000-4000-8000-000000000004','00000000-0000-4000-8000-000000000006'),
 ('00000000-0000-4000-8000-000000000008','00000000-0000-4000-8000-000000000009');

insert into public.classes (id,school_id,name,teacher_id) values
 ('cccccccc-cccc-4ccc-8ccc-ccccccccccc1','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa','Test A1','00000000-0000-4000-8000-000000000002'),
 ('cccccccc-cccc-4ccc-8ccc-ccccccccccc2','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa','Test A2','00000000-0000-4000-8000-000000000003'),
 ('cccccccc-cccc-4ccc-8ccc-ccccccccccc3','bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb','Test B1',null);
insert into public.subjects (id,school_id,name) values
 ('dddddddd-dddd-4ddd-8ddd-ddddddddddd1','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa','Mathematics'),
 ('dddddddd-dddd-4ddd-8ddd-ddddddddddd2','bbbbbbbb-bbbb-4bbb-8bbb-ddddddddddd2','Mathematics');
insert into public.academic_years (id,school_id,name) values
 ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee1','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa','2030'),
 ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee2','bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb','2030');
insert into public.terms (id,school_id,academic_year_id,name,term_number) values
 ('ffffffff-ffff-4fff-8fff-fffffffffff1','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa','eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee1','Term 1',1),
 ('ffffffff-ffff-4fff-8fff-fffffffffff2','bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb','eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee2','Term 1',1);
insert into public.class_subject_teacher (class_id,subject_id,teacher_id,term_id) values
 ('cccccccc-cccc-4ccc-8ccc-ccccccccccc1','dddddddd-dddd-4ddd-8ddd-ddddddddddd1','00000000-0000-4000-8000-000000000002','ffffffff-ffff-4fff-8fff-fffffffffff1'),
 ('cccccccc-cccc-4ccc-8ccc-ccccccccccc2','dddddddd-dddd-4ddd-8ddd-ddddddddddd1','00000000-0000-4000-8000-000000000003','ffffffff-ffff-4fff-8fff-fffffffffff1');

select plan(23);

set local role anon;
select set_config('request.jwt.claim.sub','','true');
select is((select count(*) from public.schools), 0::bigint, 'Anonymous users cannot list schools');
select is((select count(*) from public.users), 0::bigint, 'Anonymous users cannot list people');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000002',true);
select is((select count(*) from public.schools where id='aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'),1::bigint,'Teacher sees own school');
select is((select count(*) from public.schools where id='bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb'),0::bigint,'Teacher cannot see other school');
select is((select count(*) from public.users where id='00000000-0000-4000-8000-000000000002'),1::bigint,'Teacher sees own profile');
select is((select count(*) from public.users where id='00000000-0000-4000-8000-000000000003'),0::bigint,'Teacher cannot enumerate colleagues');
select is((select count(*) from public.class_subject_teacher where teacher_id='00000000-0000-4000-8000-000000000002'),1::bigint,'Teacher sees own teaching assignment');
select is((select count(*) from public.class_subject_teacher where teacher_id='00000000-0000-4000-8000-000000000003'),0::bigint,'Teacher cannot read another teachers assignment');
select is((select count(*) from public.parent_profiles),0::bigint,'Teacher cannot read parent sensitive profiles');
with blocked as (
  update public.users set role='admin'
  where id='00000000-0000-4000-8000-000000000002'
  returning id
)
select is((select count(*) from blocked),0::bigint,'Teacher cannot promote own role via update');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000006',true);
select is((select count(*) from public.student_profiles where user_id='00000000-0000-4000-8000-000000000004'),1::bigint,'Guardian sees linked child');
select is((select count(*) from public.student_profiles where user_id='00000000-0000-4000-8000-000000000005'),0::bigint,'Guardian cannot see unrelated student');
select is((select count(*) from public.student_profiles where user_id='00000000-0000-4000-8000-000000000008'),0::bigint,'Guardian cannot see another schools student');
select is((select count(*) from public.student_guardian_link where student_user_id='00000000-0000-4000-8000-000000000004'),1::bigint,'Guardian sees own child relationship');
select is((select count(*) from public.student_guardian_link where student_user_id='00000000-0000-4000-8000-000000000008'),0::bigint,'Guardian cannot see another schools relationship');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000004',true);
select is((select count(*) from public.users where id='00000000-0000-4000-8000-000000000004'),1::bigint,'Student sees own user record');
select is((select count(*) from public.users where id='00000000-0000-4000-8000-000000000005'),0::bigint,'Student cannot see another student record');
select is((select count(*) from public.student_guardian_link where student_user_id='00000000-0000-4000-8000-000000000008'),0::bigint,'Student cannot see other schools guardian links');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000001',true);
select is((select count(*) from public.users where school_id='aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'),6::bigint,'Admin lists own schools six users');
select is((select count(*) from public.users where school_id='bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb'),0::bigint,'Admin cannot list another schools users');
select is((select count(*) from public.classes where school_id='bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb'),0::bigint,'Admin cannot see another schools classes');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000007',true);
select is((select count(*) from public.users where school_id='bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb'),3::bigint,'School B admin lists own users');
select is((select count(*) from public.users where school_id='aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'),0::bigint,'School B admin cannot list School A users');
reset role;

select * from finish();
rollback;
