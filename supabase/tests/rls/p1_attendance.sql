-- Phase 1 attendance: test real RLS and relationship trigger boundaries.
-- All synthetic users, schools and attendance marks roll back.
begin;
create extension if not exists pgtap with schema extensions;
set local search_path=public,extensions;
select plan(8);

insert into public.schools(id,name) values
 ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3','Attendance School A'),
 ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb3','Attendance School B');
insert into auth.users(id,instance_id,aud,role,email,encrypted_password)
select u,'00000000-0000-0000-0000-000000000000'::uuid,
 'authenticated','authenticated','at-'||right(u::text,6)||'@example.invalid',
 'not-a-real-password' from unnest(array[
 '00000000-0000-4000-8000-000000000031'::uuid,
 '00000000-0000-4000-8000-000000000032'::uuid,
 '00000000-0000-4000-8000-000000000033'::uuid,
 '00000000-0000-4000-8000-000000000034'::uuid,
 '00000000-0000-4000-8000-000000000035'::uuid
]) as u;

insert into public.users(id,school_id,phone,full_name,role) values
 ('00000000-0000-4000-8000-000000000031','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3','attendance-admin-a','Admin A','admin'),
 ('00000000-0000-4000-8000-000000000032','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3','attendance-student-a','Student A','student'),
 ('00000000-0000-4000-8000-000000000033','bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb3','attendance-admin-b','Admin B','admin'),
 ('00000000-0000-4000-8000-000000000034','bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb3','attendance-student-b','Student B','student'),
 ('00000000-0000-4000-8000-000000000035','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3','attendance-teacher-a','Teacher A','teacher');
insert into public.classes(id,school_id,name,teacher_id) values
 ('cccccccc-cccc-4ccc-8ccc-ccccccccccc3','aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3','Attendance A',null),
 ('dddddddd-dddd-4ddd-8ddd-ddddddddddd3','bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb3','Attendance B',null);
insert into public.student_profiles(user_id,admission_no,class_id) values
 ('00000000-0000-4000-8000-000000000032','AT-A-001','cccccccc-cccc-4ccc-8ccc-ccccccccccc3'),
 ('00000000-0000-4000-8000-000000000034','AT-B-001','dddddddd-dddd-4ddd-8ddd-ddddddddddd3');

set local role authenticated;
select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000031',true);
select lives_ok($test$
 insert into public.attendance(school_id,class_id,student_user_id,date,status,marked_by)
 values('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3',
 'cccccccc-cccc-4ccc-8ccc-ccccccccccc3',
 '00000000-0000-4000-8000-000000000032',
 '2026-11-08','present','00000000-0000-4000-8000-000000000031')
$test$,'Admin may mark an enrolled student in own class');

select is((select count(*) from public.attendance),1::bigint,
  'Own school attendance row visible to administrator');

select throws_ok($test$
 insert into public.attendance(school_id,class_id,student_user_id,date,status,marked_by)
 values('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3',
 'cccccccc-cccc-4ccc-8ccc-ccccccccccc3',
 '00000000-0000-4000-8000-000000000034',
 '2026-11-08','absent','00000000-0000-4000-8000-000000000031')
$test$,'P0001','Learner is not enrolled in the selected school and class',
  'School admin cannot falsely mark a learner enrolled in another school');

select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000033',true);
select is((select count(*) from public.attendance),0::bigint,
  'Second school cannot read school A marks');
select throws_ok($test$
 insert into public.attendance(school_id,class_id,student_user_id,date,status,marked_by)
 values('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3',
 'cccccccc-cccc-4ccc-8ccc-ccccccccccc3',
 '00000000-0000-4000-8000-000000000032',
 '2026-11-09','absent','00000000-0000-4000-8000-000000000033')
$test$,'42501',null,'Foreign administrator cannot mark another school');

select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000035',true);
select is((select count(*) from public.attendance),0::bigint,
  'Unassigned teacher cannot inspect class attendance');
select throws_ok($test$
 insert into public.attendance(school_id,class_id,student_user_id,date,status,marked_by)
 values('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3',
 'cccccccc-cccc-4ccc-8ccc-ccccccccccc3',
 '00000000-0000-4000-8000-000000000032',
 '2026-11-09','present','00000000-0000-4000-8000-000000000035')
$test$,'42501',null,'Unassigned teacher cannot mark class');

select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000032',true);
select is((select count(*) from public.attendance),1::bigint,
  'Learner may view own daily attendance only');

reset role;
select * from finish();
rollback;
