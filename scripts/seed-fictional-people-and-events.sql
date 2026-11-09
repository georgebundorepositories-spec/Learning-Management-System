-- DEMONSTRATION-ONLY fictional personnel, pupils, guardians and calendar events.
-- Never import personal data. Accounts are deliberately locked and unconfirmed:
-- no password, no Auth identity provider row, banned_until 2099 and is_active=false.
-- This seed is NOT a migration and should only run when both destination schools
-- are explicitly marked is_demo=true. Idempotent deterministic synthetic UUIDs.
begin;
do $check$
begin
  if (select count(*) from public.schools
      where id in ('10000000-0000-4000-8000-000000000001'::uuid,'10000000-0000-4000-8000-000000000002'::uuid)
        and is_demo=true) <> 2 then
    raise exception 'Refusing to seed fictional records into non-demo schools';
  end if;
end
$check$;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000001','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-1@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000001','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-1','fictional-zs-1@example.invalid','Demo Administrator Primary','admin'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000002','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-2@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000002','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-2','fictional-zs-2@example.invalid','Demo Headteacher Primary','head'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000003','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-3@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000003','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-3','fictional-zs-3@example.invalid','Demo Teacher A Primary','teacher'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000004','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-4@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000004','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-4','fictional-zs-4@example.invalid','Demo Teacher B Primary','teacher'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000005','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-5@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000005','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-5','fictional-zs-5@example.invalid','Demo Teacher C Primary','teacher'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000006','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-6@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000006','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-6','fictional-zs-6@example.invalid','Demo Bursar Primary','bursar'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000021','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-21@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000021','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-21','fictional-zs-21@example.invalid','Demo Learner 01 Primary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000022','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-22@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000022','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-22','fictional-zs-22@example.invalid','Demo Learner 02 Primary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000023','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-23@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000023','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-23','fictional-zs-23@example.invalid','Demo Learner 03 Primary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000024','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-24@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000024','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-24','fictional-zs-24@example.invalid','Demo Learner 04 Primary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000025','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-25@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000025','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-25','fictional-zs-25@example.invalid','Demo Learner 05 Primary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000026','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-26@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000026','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-26','fictional-zs-26@example.invalid','Demo Learner 06 Primary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000027','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-27@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000027','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-27','fictional-zs-27@example.invalid','Demo Learner 07 Primary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000028','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-28@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000028','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-28','fictional-zs-28@example.invalid','Demo Learner 08 Primary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000029','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-29@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000029','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-29','fictional-zs-29@example.invalid','Demo Learner 09 Primary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000030','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-30@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000030','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-30','fictional-zs-30@example.invalid','Demo Learner 10 Primary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000061','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-61@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000061','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-61','fictional-zs-61@example.invalid','Demo Guardian 01 Primary','parent'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000062','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-62@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000062','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-62','fictional-zs-62@example.invalid','Demo Guardian 02 Primary','parent'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000063','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-63@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000063','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-63','fictional-zs-63@example.invalid','Demo Guardian 03 Primary','parent'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000064','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-64@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000064','10000000-0000-4000-8000-000000000001','DEMO-NO-PHONE-64','fictional-zs-64@example.invalid','Demo Guardian 04 Primary','parent'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000101','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-101@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000101','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-101','fictional-zs-101@example.invalid','Demo Administrator Secondary','admin'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000102','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-102@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000102','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-102','fictional-zs-102@example.invalid','Demo Headteacher Secondary','head'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000103','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-103@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000103','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-103','fictional-zs-103@example.invalid','Demo Teacher A Secondary','teacher'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000104','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-104@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000104','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-104','fictional-zs-104@example.invalid','Demo Teacher B Secondary','teacher'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000105','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-105@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000105','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-105','fictional-zs-105@example.invalid','Demo Teacher C Secondary','teacher'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000106','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-106@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000106','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-106','fictional-zs-106@example.invalid','Demo Bursar Secondary','bursar'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000121','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-121@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000121','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-121','fictional-zs-121@example.invalid','Demo Learner 01 Secondary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000122','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-122@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000122','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-122','fictional-zs-122@example.invalid','Demo Learner 02 Secondary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000123','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-123@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000123','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-123','fictional-zs-123@example.invalid','Demo Learner 03 Secondary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000124','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-124@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000124','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-124','fictional-zs-124@example.invalid','Demo Learner 04 Secondary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000125','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-125@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000125','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-125','fictional-zs-125@example.invalid','Demo Learner 05 Secondary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000126','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-126@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000126','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-126','fictional-zs-126@example.invalid','Demo Learner 06 Secondary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000127','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-127@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000127','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-127','fictional-zs-127@example.invalid','Demo Learner 07 Secondary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000128','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-128@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000128','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-128','fictional-zs-128@example.invalid','Demo Learner 08 Secondary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000129','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-129@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000129','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-129','fictional-zs-129@example.invalid','Demo Learner 09 Secondary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000130','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-130@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000130','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-130','fictional-zs-130@example.invalid','Demo Learner 10 Secondary','student'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000161','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-161@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000161','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-161','fictional-zs-161@example.invalid','Demo Guardian 01 Secondary','parent'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000162','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-162@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000162','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-162','fictional-zs-162@example.invalid','Demo Guardian 02 Secondary','parent'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000163','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-163@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000163','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-163','fictional-zs-163@example.invalid','Demo Guardian 03 Secondary','parent'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into auth.users(id,instance_id,aud,role,email,encrypted_password,raw_app_meta_data,is_anonymous,is_sso_user,banned_until,created_at,updated_at)
values ('20000000-0000-4000-8000-000000000164','00000000-0000-0000-0000-000000000000'::uuid,
'authenticated','authenticated','fictional-zs-164@example.invalid',null,
'{"provider":"none","providers":[],"fictional_demo":true}'::jsonb,false,false,'2099-01-01T00:00:00Z',now(),now())
on conflict(id) do nothing;
insert into public.users(id,school_id,phone,email,full_name,role,preferred_lang,is_active)
values('20000000-0000-4000-8000-000000000164','10000000-0000-4000-8000-000000000002','DEMO-NO-PHONE-164','fictional-zs-164@example.invalid','Demo Guardian 04 Secondary','parent'::public.user_role,'en',false)
on conflict(id) do nothing;
insert into public.staff_profiles(user_id,employee_no,qualifications,hire_date)
values('20000000-0000-4000-8000-000000000001','DEMO-PRI-STAFF-01','Fictional sample employment record','2026-01-13')
on conflict(user_id) do nothing;
insert into public.staff_profiles(user_id,employee_no,qualifications,hire_date)
values('20000000-0000-4000-8000-000000000002','DEMO-PRI-STAFF-02','Fictional sample employment record','2026-01-13')
on conflict(user_id) do nothing;
insert into public.staff_profiles(user_id,employee_no,qualifications,hire_date)
values('20000000-0000-4000-8000-000000000003','DEMO-PRI-STAFF-03','Fictional sample employment record','2026-01-13')
on conflict(user_id) do nothing;
insert into public.staff_profiles(user_id,employee_no,qualifications,hire_date)
values('20000000-0000-4000-8000-000000000004','DEMO-PRI-STAFF-04','Fictional sample employment record','2026-01-13')
on conflict(user_id) do nothing;
insert into public.staff_profiles(user_id,employee_no,qualifications,hire_date)
values('20000000-0000-4000-8000-000000000005','DEMO-PRI-STAFF-05','Fictional sample employment record','2026-01-13')
on conflict(user_id) do nothing;
insert into public.staff_profiles(user_id,employee_no,qualifications,hire_date)
values('20000000-0000-4000-8000-000000000006','DEMO-PRI-STAFF-06','Fictional sample employment record','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000021','DEMO-PRI-ADM-001','2017-02-02','13000000-0000-4000-8000-000000000001','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000021','20000000-0000-4000-8000-000000000061',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000022','DEMO-PRI-ADM-002','2017-03-03','13000000-0000-4000-8000-000000000002','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000022','20000000-0000-4000-8000-000000000062',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000023','DEMO-PRI-ADM-003','2017-04-04','13000000-0000-4000-8000-000000000003','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000023','20000000-0000-4000-8000-000000000063',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000024','DEMO-PRI-ADM-004','2017-05-05','13000000-0000-4000-8000-000000000004','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000024','20000000-0000-4000-8000-000000000064',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000025','DEMO-PRI-ADM-005','2017-06-06','13000000-0000-4000-8000-000000000001','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000025','20000000-0000-4000-8000-000000000061',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000026','DEMO-PRI-ADM-006','2017-07-07','13000000-0000-4000-8000-000000000002','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000026','20000000-0000-4000-8000-000000000062',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000027','DEMO-PRI-ADM-007','2017-08-08','13000000-0000-4000-8000-000000000003','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000027','20000000-0000-4000-8000-000000000063',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000028','DEMO-PRI-ADM-008','2017-09-09','13000000-0000-4000-8000-000000000004','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000028','20000000-0000-4000-8000-000000000064',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000029','DEMO-PRI-ADM-009','2017-10-10','13000000-0000-4000-8000-000000000001','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000029','20000000-0000-4000-8000-000000000061',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000030','DEMO-PRI-ADM-010','2017-11-11','13000000-0000-4000-8000-000000000002','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000030','20000000-0000-4000-8000-000000000062',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.parent_profiles(user_id,relationship,preferred_channel)
values('20000000-0000-4000-8000-000000000061','Fictional demonstration guardian','sms')
on conflict(user_id) do nothing;
insert into public.parent_profiles(user_id,relationship,preferred_channel)
values('20000000-0000-4000-8000-000000000062','Fictional demonstration guardian','sms')
on conflict(user_id) do nothing;
insert into public.parent_profiles(user_id,relationship,preferred_channel)
values('20000000-0000-4000-8000-000000000063','Fictional demonstration guardian','sms')
on conflict(user_id) do nothing;
insert into public.parent_profiles(user_id,relationship,preferred_channel)
values('20000000-0000-4000-8000-000000000064','Fictional demonstration guardian','sms')
on conflict(user_id) do nothing;
insert into public.class_subject_teacher(class_id,subject_id,teacher_id,term_id)
values ('13000000-0000-4000-8000-000000000001','14000000-0000-4000-8000-000000000001','20000000-0000-4000-8000-000000000003','12000000-0000-4000-8000-000000000003')
on conflict(class_id,subject_id,term_id) do nothing;
insert into public.class_subject_teacher(class_id,subject_id,teacher_id,term_id)
values ('13000000-0000-4000-8000-000000000002','14000000-0000-4000-8000-000000000002','20000000-0000-4000-8000-000000000004','12000000-0000-4000-8000-000000000003')
on conflict(class_id,subject_id,term_id) do nothing;
insert into public.class_subject_teacher(class_id,subject_id,teacher_id,term_id)
values ('13000000-0000-4000-8000-000000000003','14000000-0000-4000-8000-000000000003','20000000-0000-4000-8000-000000000005','12000000-0000-4000-8000-000000000003')
on conflict(class_id,subject_id,term_id) do nothing;
insert into public.class_subject_teacher(class_id,subject_id,teacher_id,term_id)
values ('13000000-0000-4000-8000-000000000004','14000000-0000-4000-8000-000000000004','20000000-0000-4000-8000-000000000003','12000000-0000-4000-8000-000000000003')
on conflict(class_id,subject_id,term_id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000001','10000000-0000-4000-8000-000000000001','12000000-0000-4000-8000-000000000003','DEMO — Teaching plans review',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'academic'::public.calendar_category,
'2026-11-10T09:00:00+02:00','2026-11-10T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000003','pending',false,'staff')
on conflict(id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000002','10000000-0000-4000-8000-000000000001','12000000-0000-4000-8000-000000000003','DEMO — Grade 7 exhibition',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'cultural'::public.calendar_category,
'2026-11-13T09:00:00+02:00','2026-11-13T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000004','pending',false,'staff')
on conflict(id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000003','10000000-0000-4000-8000-000000000001','12000000-0000-4000-8000-000000000003','DEMO — Inter-house athletics',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'sports'::public.calendar_category,
'2026-11-16T09:00:00+02:00','2026-11-16T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000005','pending',false,'staff')
on conflict(id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000004','10000000-0000-4000-8000-000000000001','12000000-0000-4000-8000-000000000003','DEMO — ECD creative play',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'academic'::public.calendar_category,
'2026-11-19T09:00:00+02:00','2026-11-19T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000003','pending',false,'staff')
on conflict(id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000005','10000000-0000-4000-8000-000000000001','12000000-0000-4000-8000-000000000003','DEMO — Term progress review',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'academic'::public.calendar_category,
'2026-12-01T09:00:00+02:00','2026-12-01T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000004','pending',false,'staff')
on conflict(id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000006','10000000-0000-4000-8000-000000000001','12000000-0000-4000-8000-000000000003','DEMO — Library reading hour',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'academic'::public.calendar_category,
'2026-12-03T09:00:00+02:00','2026-12-03T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000005','pending',false,'staff')
on conflict(id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000007','10000000-0000-4000-8000-000000000001','12000000-0000-4000-8000-000000000003','DEMO — ZIMSEC subject planning',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'academic'::public.calendar_category,
'2026-12-05T09:00:00+02:00','2026-12-05T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000003','pending',false,'staff')
on conflict(id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000008','10000000-0000-4000-8000-000000000001','12000000-0000-4000-8000-000000000003','DEMO — End-of-term gathering',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'academic'::public.calendar_category,
'2026-12-07T09:00:00+02:00','2026-12-07T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000004','pending',false,'staff')
on conflict(id) do nothing;
insert into public.staff_profiles(user_id,employee_no,qualifications,hire_date)
values('20000000-0000-4000-8000-000000000101','DEMO-SEC-STAFF-01','Fictional sample employment record','2026-01-13')
on conflict(user_id) do nothing;
insert into public.staff_profiles(user_id,employee_no,qualifications,hire_date)
values('20000000-0000-4000-8000-000000000102','DEMO-SEC-STAFF-02','Fictional sample employment record','2026-01-13')
on conflict(user_id) do nothing;
insert into public.staff_profiles(user_id,employee_no,qualifications,hire_date)
values('20000000-0000-4000-8000-000000000103','DEMO-SEC-STAFF-03','Fictional sample employment record','2026-01-13')
on conflict(user_id) do nothing;
insert into public.staff_profiles(user_id,employee_no,qualifications,hire_date)
values('20000000-0000-4000-8000-000000000104','DEMO-SEC-STAFF-04','Fictional sample employment record','2026-01-13')
on conflict(user_id) do nothing;
insert into public.staff_profiles(user_id,employee_no,qualifications,hire_date)
values('20000000-0000-4000-8000-000000000105','DEMO-SEC-STAFF-05','Fictional sample employment record','2026-01-13')
on conflict(user_id) do nothing;
insert into public.staff_profiles(user_id,employee_no,qualifications,hire_date)
values('20000000-0000-4000-8000-000000000106','DEMO-SEC-STAFF-06','Fictional sample employment record','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000121','DEMO-SEC-ADM-001','2010-02-02','13000000-0000-4000-8000-000000000005','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000121','20000000-0000-4000-8000-000000000161',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000122','DEMO-SEC-ADM-002','2010-03-03','13000000-0000-4000-8000-000000000006','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000122','20000000-0000-4000-8000-000000000162',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000123','DEMO-SEC-ADM-003','2010-04-04','13000000-0000-4000-8000-000000000007','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000123','20000000-0000-4000-8000-000000000163',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000124','DEMO-SEC-ADM-004','2010-05-05','13000000-0000-4000-8000-000000000008','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000124','20000000-0000-4000-8000-000000000164',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000125','DEMO-SEC-ADM-005','2010-06-06','13000000-0000-4000-8000-000000000005','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000125','20000000-0000-4000-8000-000000000161',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000126','DEMO-SEC-ADM-006','2010-07-07','13000000-0000-4000-8000-000000000006','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000126','20000000-0000-4000-8000-000000000162',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000127','DEMO-SEC-ADM-007','2010-08-08','13000000-0000-4000-8000-000000000007','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000127','20000000-0000-4000-8000-000000000163',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000128','DEMO-SEC-ADM-008','2010-09-09','13000000-0000-4000-8000-000000000008','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000128','20000000-0000-4000-8000-000000000164',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000129','DEMO-SEC-ADM-009','2010-10-10','13000000-0000-4000-8000-000000000005','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000129','20000000-0000-4000-8000-000000000161',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.student_profiles(user_id,admission_no,dob,class_id,stream,house,enrolled_at)
values('20000000-0000-4000-8000-000000000130','DEMO-SEC-ADM-010','2010-11-11','13000000-0000-4000-8000-000000000006','A','Demo House','2026-01-13')
on conflict(user_id) do nothing;
insert into public.student_guardian_link(student_user_id,guardian_user_id,is_primary,can_pay_fees)
values('20000000-0000-4000-8000-000000000130','20000000-0000-4000-8000-000000000162',true,false)
on conflict(student_user_id,guardian_user_id) do nothing;
insert into public.parent_profiles(user_id,relationship,preferred_channel)
values('20000000-0000-4000-8000-000000000161','Fictional demonstration guardian','sms')
on conflict(user_id) do nothing;
insert into public.parent_profiles(user_id,relationship,preferred_channel)
values('20000000-0000-4000-8000-000000000162','Fictional demonstration guardian','sms')
on conflict(user_id) do nothing;
insert into public.parent_profiles(user_id,relationship,preferred_channel)
values('20000000-0000-4000-8000-000000000163','Fictional demonstration guardian','sms')
on conflict(user_id) do nothing;
insert into public.parent_profiles(user_id,relationship,preferred_channel)
values('20000000-0000-4000-8000-000000000164','Fictional demonstration guardian','sms')
on conflict(user_id) do nothing;
insert into public.class_subject_teacher(class_id,subject_id,teacher_id,term_id)
values ('13000000-0000-4000-8000-000000000005','14000000-0000-4000-8000-000000000007','20000000-0000-4000-8000-000000000103','12000000-0000-4000-8000-000000000009')
on conflict(class_id,subject_id,term_id) do nothing;
insert into public.class_subject_teacher(class_id,subject_id,teacher_id,term_id)
values ('13000000-0000-4000-8000-000000000006','14000000-0000-4000-8000-000000000008','20000000-0000-4000-8000-000000000104','12000000-0000-4000-8000-000000000009')
on conflict(class_id,subject_id,term_id) do nothing;
insert into public.class_subject_teacher(class_id,subject_id,teacher_id,term_id)
values ('13000000-0000-4000-8000-000000000007','14000000-0000-4000-8000-000000000009','20000000-0000-4000-8000-000000000105','12000000-0000-4000-8000-000000000009')
on conflict(class_id,subject_id,term_id) do nothing;
insert into public.class_subject_teacher(class_id,subject_id,teacher_id,term_id)
values ('13000000-0000-4000-8000-000000000008','14000000-0000-4000-8000-000000000010','20000000-0000-4000-8000-000000000103','12000000-0000-4000-8000-000000000009')
on conflict(class_id,subject_id,term_id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000021','10000000-0000-4000-8000-000000000002','12000000-0000-4000-8000-000000000009','DEMO — Form 4 revision planning',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'academic'::public.calendar_category,
'2026-11-10T09:00:00+02:00','2026-11-10T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000103','pending',false,'staff')
on conflict(id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000022','10000000-0000-4000-8000-000000000002','12000000-0000-4000-8000-000000000009','DEMO — Science club exhibition',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'cultural'::public.calendar_category,
'2026-11-13T09:00:00+02:00','2026-11-13T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000104','pending',false,'staff')
on conflict(id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000023','10000000-0000-4000-8000-000000000002','12000000-0000-4000-8000-000000000009','DEMO — Inter-form football',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'sports'::public.calendar_category,
'2026-11-16T09:00:00+02:00','2026-11-16T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000105','pending',false,'staff')
on conflict(id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000024','10000000-0000-4000-8000-000000000002','12000000-0000-4000-8000-000000000009','DEMO — Geography presentation',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'academic'::public.calendar_category,
'2026-11-19T09:00:00+02:00','2026-11-19T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000103','pending',false,'staff')
on conflict(id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000025','10000000-0000-4000-8000-000000000002','12000000-0000-4000-8000-000000000009','DEMO — School council meeting',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'academic'::public.calendar_category,
'2026-12-01T09:00:00+02:00','2026-12-01T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000104','pending',false,'staff')
on conflict(id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000026','10000000-0000-4000-8000-000000000002','12000000-0000-4000-8000-000000000009','DEMO — Library reading circle',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'academic'::public.calendar_category,
'2026-12-03T09:00:00+02:00','2026-12-03T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000105','pending',false,'staff')
on conflict(id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000027','10000000-0000-4000-8000-000000000002','12000000-0000-4000-8000-000000000009','DEMO — Mock exam preparation',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'academic'::public.calendar_category,
'2026-12-05T09:00:00+02:00','2026-12-05T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000103','pending',false,'staff')
on conflict(id) do nothing;
insert into public.calendar_events(id,school_id,term_id,title,description,category,start_at,end_at,location,created_by,approval_status,is_published,visibility)
values('30000000-0000-4000-8000-000000000028','10000000-0000-4000-8000-000000000002','12000000-0000-4000-8000-000000000009','DEMO — End-of-term assembly',
'Entirely fictional activity for ZimSchool LMS demonstration.',
'academic'::public.calendar_category,
'2026-12-07T09:00:00+02:00','2026-12-07T11:00:00+02:00','Demo School Campus',
'20000000-0000-4000-8000-000000000104','pending',false,'staff')
on conflict(id) do nothing;
commit;
-- No working login credentials were created. These user profiles remain inactive.
