-- Regression for cross-subject teacher coursework privacy.
-- Hosted rollback-only canary separately verifies live actor behaviour.
begin;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions, pg_catalog;
select plan(9);

select ok(
  (select relrowsecurity from pg_class
   where oid='public.school_assignments'::regclass),
  'assignment RLS enabled');
select ok(
  exists(select 1 from pg_policies
   where schemaname='public' and tablename='school_assignments'
     and policyname='assignments_read_authorised' and cmd='SELECT'),
  'authorised assignment SELECT policy exists');
select ok(
  exists(select 1 from pg_policies
   where schemaname='public' and tablename='school_assignments'
     and policyname='assignments_read_authorised'
     and qual like '%can_manage_assessment%'),
  'teacher reads require both class and subject assignment');
select ok(
  not exists(select 1 from pg_policies
   where schemaname='public' and tablename='school_assignments'
     and policyname='assignments_read_authorised'
     and qual like '%can_manage_attendance%'),
  'broad class attendance access cannot leak unrelated subjects');
select ok(
  exists(select 1 from pg_policies
   where schemaname='public' and tablename='school_assignments'
     and policyname='assignments_read_authorised'
     and qual like '%student%' and qual like '%parent%'),
  'family fallback explicitly limits actor to student or parent');
select ok(
  exists(select 1 from pg_policies
   where schemaname='public' and tablename='school_assignments'
     and policyname='assignments_read_authorised'
     and qual like '%can_view_student%'),
  'learner and linked guardian class reads remain protected');
select ok(
  exists(select 1 from pg_policies
   where schemaname='public' and tablename='assignment_submissions'
     and policyname='assignment_submissions_read'
     and qual like '%can_manage_assessment%'),
  'staff cannot read answers outside taught subject');
select ok(
  exists(select 1 from pg_policies
   where schemaname='public' and tablename='assignment_reviews'
     and policyname='assignment_reviews_private_read'
     and qual like '%can_manage_assessment%'),
  'staff cannot read feedback outside taught subject');
select ok(
  not has_table_privilege('anon','public.school_assignments','SELECT'),
  'anonymous sessions have no assignment read privilege');

select * from finish();
rollback;
