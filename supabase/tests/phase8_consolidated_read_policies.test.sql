-- Verify consolidated SELECT policies preserve role predicates and avoid
-- multiple permissive policies on sensitive school records.
begin;
create extension if not exists pgtap with schema extensions;
set local search_path=public,extensions,pg_catalog;
select plan(16);

select is(
  (select count(*)::integer from pg_policies
   where schemaname='public' and tablename='users'
     and cmd='SELECT' and roles='{"authenticated"}'::name[]),1,
  'one authenticated user directory read policy'
);
select is(
  (select count(*)::integer from pg_policies
   where schemaname='public' and tablename='student_profiles'
     and cmd='SELECT' and roles='{"authenticated"}'::name[]),1,
  'one authenticated learner-profile read policy'
);
select is(
  (select count(*)::integer from pg_policies
   where schemaname='public' and tablename='assessments'
     and cmd='SELECT' and roles='{"authenticated"}'::name[]),1,
  'one authenticated assessment read policy'
);
select is(
  (select count(*)::integer from pg_policies
   where schemaname='public' and tablename='assessment_results'
     and cmd='SELECT' and roles='{"authenticated"}'::name[]),1,
  'one authenticated grade read policy'
);

select ok(exists(select 1 from pg_policies where schemaname='public'
  and tablename='users' and policyname='users_read'
  and qual like '%can_view_student%' and qual like '%is_school_admin%'),
  'user directory retains family/teacher and admin visibility');
select ok(exists(select 1 from pg_policies where schemaname='public'
  and tablename='student_profiles' and policyname='student_profiles_read'
  and qual like '%can_view_student%' and qual like '%is_guardian_of%'),
  'learner profiles retain teacher and linked guardian visibility');
select ok(exists(select 1 from pg_policies where schemaname='public'
  and tablename='assessments' and policyname='assessments_staff_read'
  and qual like '%can_manage_assessment%' and qual like '%can_view_student%'),
  'assessment visibility retains subject staff and student/guardian predicates');
select ok(exists(select 1 from pg_policies where schemaname='public'
  and tablename='assessment_results' and policyname='assessment_results_staff_read'
  and qual like '%can_manage_assessment%' and qual like '%can_view_student%'),
  'grade visibility retains subject staff and student/guardian predicates');

select ok(not exists(select 1 from pg_policies where schemaname='public'
  and tablename='users' and policyname='users_read_assigned_learners'),
  'redundant user policy removed');
select ok(not exists(select 1 from pg_policies where schemaname='public'
  and tablename='student_profiles' and policyname='student_profiles_teacher_read'),
  'redundant learner profile policy removed');
select ok(not exists(select 1 from pg_policies where schemaname='public'
  and tablename='assessments' and policyname='assessments_student_read'),
  'redundant assessment family policy removed');
select ok(not exists(select 1 from pg_policies where schemaname='public'
  and tablename='assessment_results' and policyname='assessment_results_private_read'),
  'redundant results family policy removed');

select ok((select relrowsecurity from pg_class
  where oid='public.users'::regclass),'users RLS enabled');
select ok((select relrowsecurity from pg_class
  where oid='public.student_profiles'::regclass),'learner profiles RLS enabled');
select ok((select relrowsecurity from pg_class
  where oid='public.assessments'::regclass),'assessments RLS enabled');
select ok((select relrowsecurity from pg_class
  where oid='public.assessment_results'::regclass),'grades RLS enabled');
select * from finish();
rollback;
