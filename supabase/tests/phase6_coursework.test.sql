-- Phase 6 coursework security regression (run with "supabase test db").
begin;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions, pg_catalog;
select plan(12);

select ok(to_regclass('public.school_assignments') is not null,
  'class assignments table exists');
select ok(to_regclass('public.assignment_submissions') is not null,
  'assignment submission table exists');
select ok((select relrowsecurity from pg_class
  where oid='public.school_assignments'::regclass), 'assignments RLS enabled');
select ok((select relrowsecurity from pg_class
  where oid='public.assignment_submissions'::regclass), 'submissions RLS enabled');

select ok(exists(select 1 from pg_policies where schemaname='public'
 and tablename='school_assignments' and policyname='assignments_read_authorised'
 and cmd='SELECT'), 'assignment reads restricted by policy');
select ok(exists(select 1 from pg_policies where schemaname='public'
 and tablename='school_assignments' and policyname='assignments_insert_staff'
 and cmd='INSERT'), 'staff assignment writes restricted by policy');
select ok(exists(select 1 from pg_policies where schemaname='public'
 and tablename='assignment_submissions' and policyname='assignment_submissions_student_insert'
 and cmd='INSERT'), 'learner submission writes restricted by policy');
select ok(exists(select 1 from pg_policies where schemaname='public'
 and tablename='assignment_submissions' and policyname='assignment_submissions_student_update'
 and cmd='UPDATE'), 'learner revisions restricted by policy');

select ok(not has_table_privilege('anon','public.school_assignments','SELECT'),
 'unauthenticated role cannot list class assignments');
select ok(not has_table_privilege('anon','public.assignment_submissions','SELECT'),
 'unauthenticated role cannot read private answers');
select ok(has_column_privilege('authenticated',
 'public.assignment_submissions','text_answer','UPDATE'),
 'only answer text can be revised by authenticated clients');
select ok(not has_column_privilege('authenticated',
 'public.assignment_submissions','student_user_id','UPDATE'),
 'authenticated clients cannot reassign answer ownership');
select * from finish();
rollback;
