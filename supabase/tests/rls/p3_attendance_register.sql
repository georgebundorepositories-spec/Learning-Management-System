-- Phase 3 academic slice: local-only synthetic attendance security tests.
-- No synthetic identities are persisted: the entire test ROLLS BACK.
begin;
create extension if not exists pgtap with schema extensions;
set local search_path=public,extensions;
select plan(19);

insert into public.schools(id,name) values
 ('aaaaaaaa-1111-4111-8111-aaaaaaaaaaa1','Attendance Fictional School A'),
 ('bbbbbbbb-1111-4111-8111-bbbbbbbbbbb1','Attendance Fictional School B');
insert into auth.users(id,instance_id,aud,role,email,encrypted_password)
select id,'00000000-0000-0000-0000-000000000000'::uuid,
 'authenticated','authenticated','attendance-'||right(id::text,5)||'@example.invalid',
 'not-a-real-password'
from unnest(array[
 '10000000-0000-4000-8000-000000001001'::uuid,
 '10000000-0000-4000-8000-000000001002'::uuid,
 '10000000-0000-4000-8000-000000001003'::uuid,
 '10000000-0000-4000-8000-000000001004'::uuid,
 '10000000-0000-4000-8000-000000001005'::uuid,
 '10000000-0000-4000-8000-000000001006'::uuid,
 '10000000-0000-4000-8000-000000001007'::uuid,
 '10000000-0000-4000-8000-000000001008'::uuid
]) as id;

insert into public.users(id,school_id,phone,full_name,role) values
 ('10000000-0000-4000-8000-000000001001','aaaaaaaa-1111-4111-8111-aaaaaaaaaaa1','at-ci-head-a','School A Head','head'),
 ('10000000-0000-4000-8000-000000001002','aaaaaaaa-1111-4111-8111-aaaaaaaaaaa1','at-ci-teacher-a','School A Teacher','teacher'),
 ('10000000-0000-4000-8000-000000001003','aaaaaaaa-1111-4111-8111-aaaaaaaaaaa1','at-ci-other-teacher','School A Other Teacher','teacher'),
 ('10000000-0000-4000-8000-000000001004','bbbbbbbb-1111-4111-8111-bbbbbbbbbbb1','at-ci-head-b','School B Head','head'),
 ('10000000-0000-4000-8000-000000001005','aaaaaaaa-1111-4111-8111-aaaaaaaaaaa1','at-ci-learner-a','School A Learner A','student'),
 ('10000000-0000-4000-8000-000000001006','aaaaaaaa-1111-4111-8111-aaaaaaaaaaa1','at-ci-learner-b','School A Learner B','student'),
 ('10000000-0000-4000-8000-000000001007','bbbbbbbb-1111-4111-8111-bbbbbbbbbbb1','at-ci-learner-c','School B Learner','student'),
 ('10000000-0000-4000-8000-000000001008','aaaaaaaa-1111-4111-8111-aaaaaaaaaaa1','at-ci-guardian-a','School A Guardian','parent');
insert into public.classes(id,school_id,name,grade_level,teacher_id) values
 ('20000000-0000-4000-8000-000000000001','aaaaaaaa-1111-4111-8111-aaaaaaaaaaa1','Grade 3 Fictional','Grade 3','10000000-0000-4000-8000-000000001002'),
 ('20000000-0000-4000-8000-000000000002','bbbbbbbb-1111-4111-8111-bbbbbbbbbbb1','Form 1 Fictional','Form 1',null);
insert into public.student_profiles(user_id,admission_no,class_id) values
 ('10000000-0000-4000-8000-000000001005','AT-TEST-A','20000000-0000-4000-8000-000000000001'),
 ('10000000-0000-4000-8000-000000001006','AT-TEST-B','20000000-0000-4000-8000-000000000001'),
 ('10000000-0000-4000-8000-000000001007','AT-TEST-C','20000000-0000-4000-8000-000000000002');
insert into public.student_guardian_link(student_user_id,guardian_user_id)
values('10000000-0000-4000-8000-000000001005','10000000-0000-4000-8000-000000001008');

select ok((select relrowsecurity from pg_class where oid='public.attendance'::regclass),
 'Attendance RLS must be enabled');
select ok(not has_function_privilege('anon','public.get_attendance_roster(uuid)','execute'),
 'Anonymous clients cannot fetch a class roster');
select ok(has_function_privilege('authenticated','public.get_attendance_roster(uuid)','execute'),
 'Signed-in callers may reach the explicitly checked roster endpoint');

set local role authenticated;
select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000001001',true);
select is((select count(*) from public.get_attendance_roster(
 '20000000-0000-4000-8000-000000000001'::uuid)),2::bigint,
 'School head can read names of two learners in own class');
select is((select count(*) from public.get_attendance_roster(
 '20000000-0000-4000-8000-000000000002'::uuid)),0::bigint,
 'School head cannot enumerate a class in another school');

insert into public.attendance(school_id,class_id,student_user_id,date,status,marked_by) values
 ('aaaaaaaa-1111-4111-8111-aaaaaaaaaaa1','20000000-0000-4000-8000-000000000001',
 '10000000-0000-4000-8000-000000001005','2026-11-08','present','10000000-0000-4000-8000-000000001001'),
 ('aaaaaaaa-1111-4111-8111-aaaaaaaaaaa1','20000000-0000-4000-8000-000000000001',
 '10000000-0000-4000-8000-000000001006','2026-11-08','absent','10000000-0000-4000-8000-000000001001');
select is((select count(*) from public.attendance),2::bigint,
 'School head can create two attendance records');

insert into public.attendance(school_id,class_id,student_user_id,date,status,marked_by)
values('aaaaaaaa-1111-4111-8111-aaaaaaaaaaa1','20000000-0000-4000-8000-000000000001',
 '10000000-0000-4000-8000-000000001006','2026-11-08','present','10000000-0000-4000-8000-000000001001')
on conflict(class_id,student_user_id,date) do update
set status=excluded.status,marked_by=excluded.marked_by;
select is((select status::text from public.attendance
 where student_user_id='10000000-0000-4000-8000-000000001006'),'present',
 'Saving existing attendance changes status');
select is((select count(*) from public.attendance),2::bigint,
 'Repeated attendance date cannot create duplicate rows');

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000001002',true);
select is((select count(*) from public.attendance),2::bigint,
 'Assigned teacher can see own class attendance');

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000001003',true);
select is((select count(*) from public.attendance),0::bigint,
 'Unassigned teacher cannot read another teacher class attendance');

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000001004',true);
select is((select count(*) from public.attendance),0::bigint,
 'Other school head sees zero attendance in school A');

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000001005',true);
select is((select count(*) from public.attendance),1::bigint,
 'Learner can only read own attendance');

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000001008',true);
select is((select count(*) from public.attendance),1::bigint,
 'Verified guardian can only read linked learners attendance');

select ok(not has_table_privilege('authenticated','public.attendance','DELETE'),
 'Android callers cannot delete audit history');

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000001001',true);
do $test$
declare rejected boolean:=false;
begin
  begin
    insert into public.attendance(school_id,class_id,student_user_id,date,status,marked_by)
    values('bbbbbbbb-1111-4111-8111-bbbbbbbbbbb1',
      '20000000-0000-4000-8000-000000000002',
      '10000000-0000-4000-8000-000000001007','2026-11-08',
      'present','10000000-0000-4000-8000-000000001001');
  exception when others then rejected:=true;
  end;
  if not rejected then raise exception 'FOREIGN SCHOOL attendance insert succeeded'; end if;
end
$test$;
select pass('Forged cross-school attendance insert rejected');

do $test$
declare rejected boolean:=false;
begin
  begin
    insert into public.attendance(school_id,class_id,student_user_id,date,status,marked_by)
    values('aaaaaaaa-1111-4111-8111-aaaaaaaaaaa1',
      '20000000-0000-4000-8000-000000000001',
      '10000000-0000-4000-8000-000000001007','2026-11-08',
      'present','10000000-0000-4000-8000-000000001001');
  exception when others then rejected:=true;
  end;
  if not rejected then raise exception 'MISMATCHED learner enrollment was accepted'; end if;
end
$test$;
select pass('Attendance trigger rejects learner outside selected class/school');

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000001003',true);
do $test$
declare rejected boolean:=false;
begin
  begin
    insert into public.attendance(school_id,class_id,student_user_id,date,status,marked_by)
    values('aaaaaaaa-1111-4111-8111-aaaaaaaaaaa1',
      '20000000-0000-4000-8000-000000000001',
      '10000000-0000-4000-8000-000000001005','2026-11-09',
      'late','10000000-0000-4000-8000-000000001003');
  exception when others then rejected:=true;
  end;
  if not rejected then raise exception 'UNASSIGNED teacher wrote attendance'; end if;
end
$test$;
select pass('Teacher without class assignment cannot write roster');

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000001001',true);
do $test$
declare rejected boolean:=false;
begin
  begin
    update public.attendance
      set student_user_id='10000000-0000-4000-8000-000000001006'
      where student_user_id='10000000-0000-4000-8000-000000001005';
  exception when others then rejected:=true;
  end;
  if not rejected then raise exception 'ATTENDANCE identity was mutated'; end if;
end
$test$;
select pass('Attendance identity immutable after recording');

reset role;
update public.users set is_active=false where id='10000000-0000-4000-8000-000000001001';
set local role authenticated;
select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000001001',true);
select is((select count(*) from public.get_attendance_roster(
 '20000000-0000-4000-8000-000000000001'::uuid)),0::bigint,
 'Deactivated school head cannot enumerate learners');

reset role;
select * from finish();
rollback;
