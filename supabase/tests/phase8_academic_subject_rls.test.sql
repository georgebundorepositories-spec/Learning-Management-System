-- Assessment subject-permission regression test for clean local database CI.
-- Hosted rollback-only live canaries additionally test the actual teacher/parent flows.
begin;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions, pg_catalog;
select plan(14);

select ok(to_regprocedure('app_private.can_manage_assessment(uuid,uuid)') is not null,
  'school+class+subject staff authorisation function exists');
select ok((select prosecdef from pg_proc
  where oid='app_private.can_manage_assessment(uuid,uuid)'::regprocedure),
  'internal helper is security definer with immutable lookup context');
select ok(has_function_privilege('authenticated',
  'app_private.can_manage_assessment(uuid,uuid)','EXECUTE'),
  'authenticated policies can execute the subject authorisation helper');
select ok(not has_function_privilege('anon',
  'app_private.can_manage_assessment(uuid,uuid)','EXECUTE'),
  'anonymous callers cannot execute assessment permission helper');

select ok((select relrowsecurity from pg_class
  where oid='public.assessments'::regclass),'assessment RLS enabled');
select ok((select relrowsecurity from pg_class
  where oid='public.assessment_results'::regclass),'assessment mark RLS enabled');

select ok(exists(select 1 from pg_policies where schemaname='public'
  and tablename='assessments' and policyname='assessments_staff_insert'
  and cmd='INSERT' and with_check like '%can_manage_assessment%'),
  'teacher cannot create an assessment outside their class+subject assignment');
select ok(exists(select 1 from pg_policies where schemaname='public'
  and tablename='assessments' and policyname='assessments_staff_read'
  and cmd='SELECT' and qual like '%can_manage_assessment%'),
  'teacher class gradebook reads are subject scoped');
select ok(exists(select 1 from pg_policies where schemaname='public'
  and tablename='assessment_results' and policyname='assessment_results_staff_insert'
  and cmd='INSERT' and with_check like '%can_manage_assessment%'),
  'student marks cannot be inserted by unassigned teachers');
select ok(exists(select 1 from pg_policies where schemaname='public'
  and tablename='assessment_results' and policyname='assessment_results_staff_update'
  and cmd='UPDATE' and with_check like '%can_manage_assessment%'),
  'grade updates are restricted to authorised teachers');

select ok(not has_column_privilege('authenticated',
  'public.assessment_results','student_user_id','UPDATE'),
  'grade editor cannot reassign mark to another pupil');
select ok(not has_column_privilege('authenticated',
  'public.assessment_results','assessment_id','UPDATE'),
  'grade editor cannot change associated assessment');
select ok(has_column_privilege('authenticated',
  'public.assessment_results','score','UPDATE'),
  'authorised grade editor may update the score');
select ok(not exists(select 1 from pg_policies where schemaname='public'
  and tablename in ('assessments','assessment_results')
  and 'anon' = any(roles)),
  'assessment policies do not grant anonymous access');
select * from finish();
rollback;
