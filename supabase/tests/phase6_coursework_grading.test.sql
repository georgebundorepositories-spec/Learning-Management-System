-- Phase 6: security regression for marking and reviewing coursework.
-- Local Supabase tests run with pgTAP; RLS actor tests run as rollback-only canaries on hosted staging.
begin;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions, pg_catalog;
select plan(16);

select ok(to_regclass('public.assignment_reviews') is not null,
  'coursework review table exists');
select ok((select relrowsecurity from pg_class where oid='public.assignment_reviews'::regclass),
  'grade feedback table has RLS enabled');
select ok(not has_table_privilege('anon','public.assignment_reviews','SELECT'),
  'anonymous callers cannot read coursework grades');
select ok(has_table_privilege('authenticated','public.assignment_reviews','SELECT'),
  'authenticated RLS callers can read their permitted feedback');
select ok(has_table_privilege('authenticated','public.assignment_reviews','INSERT'),
  'authorised RLS staff can create review records');
select ok(not has_column_privilege('authenticated','public.assignment_reviews','school_id','UPDATE'),
  'review tenant cannot be reassigned');
select ok(not has_column_privilege('authenticated','public.assignment_reviews','submission_id','UPDATE'),
  'review cannot be reassigned to another learner');
select ok(has_column_privilege('authenticated','public.assignment_reviews','score','UPDATE'),
  'authorised teacher may revise a score');
select ok(has_column_privilege('authenticated','public.assignment_reviews','feedback','UPDATE'),
  'authorised teacher may revise written feedback');
select ok(exists(select 1 from information_schema.columns where table_schema='public'
  and table_name='school_assignments' and column_name='max_score'),
  'assignment has explicit maximum mark');
select ok(exists(select 1 from pg_policies where schemaname='public'
  and tablename='assignment_reviews' and policyname='assignment_reviews_private_read'
  and cmd='SELECT' and qual like '%can_manage_assessment%'),
  'subject-scoped teacher access and private family reads');
select ok(exists(select 1 from pg_policies where schemaname='public'
  and tablename='assignment_reviews' and policyname='assignment_reviews_staff_insert'
  and cmd='INSERT' and with_check like '%can_manage_assessment%'),
  'only assigned teaching staff can grade');
select ok(exists(select 1 from pg_policies where schemaname='public'
  and tablename='assignment_reviews' and policyname='assignment_reviews_staff_update'
  and cmd='UPDATE' and with_check like '%can_manage_assessment%'),
  'only assigned teaching staff can revise reviews');
select ok(exists(select 1 from pg_policies where schemaname='public'
  and tablename='assignment_submissions'
  and policyname='assignment_submissions_student_update'
  and cmd='UPDATE' and qual like '%is_own_submission_reviewed%'),
  'graded learner answer cannot be revised');
select ok((select prosecdef from pg_proc
  where oid='app_private.is_own_submission_reviewed(uuid)'::regprocedure),
  'non-recursive review-lock check uses internal restricted function');
select ok(not has_function_privilege('anon',
  'app_private.is_own_submission_reviewed(uuid)','EXECUTE'),
  'anonymous users cannot query internal review status');
select * from finish();
rollback;
