-- Phase 10: safe, append-only, learner-owned lesson progress across devices.
begin;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions, pg_catalog;
select plan(15);

select ok(to_regclass('public.lesson_catalog') is not null,
 'authoritative lesson catalog exists');
select is((select count(*)::integer from public.lesson_catalog),75,
 'catalog contains exactly the 12 starter plus 63 curriculum lesson IDs');
select ok((select relrowsecurity from pg_class where oid='public.lesson_catalog'::regclass),
 'catalog has RLS');
select ok((select relrowsecurity from pg_class where oid='public.lesson_completions'::regclass),
 'private completion table has RLS');
select ok(not has_table_privilege('anon','public.lesson_completions','SELECT'),
 'anonymous users cannot read learner completions');
select ok(has_table_privilege('authenticated','public.lesson_completions','SELECT'),
 'authenticated sessions can read permitted learner rows');
select ok(has_column_privilege('authenticated','public.lesson_completions','lesson_id','INSERT'),
 'students can insert a checked lesson ID');
select ok(not has_column_privilege('authenticated','public.lesson_completions','completed_at','INSERT'),
 'clients cannot forge completion timestamps');
select ok(not has_table_privilege('authenticated','public.lesson_completions','UPDATE'),
 'existing completions cannot be edited');
select ok(not has_table_privilege('authenticated','public.lesson_completions','DELETE'),
 'learners cannot delete already synced progress');
select ok(exists(select 1 from pg_policies where schemaname='public'
 and tablename='lesson_completions' and policyname='lesson_completions_private_read'
 and cmd='SELECT' and qual like '%can_view_student%'),
 'read visibility is restricted to an authorised learner or linked role');
select ok(exists(select 1 from pg_policies where schemaname='public'
 and tablename='lesson_completions' and policyname='lesson_completions_student_insert'
 and cmd='INSERT' and with_check like '%student%' and with_check like '%auth.uid%'),
 'writes require signed-in student identity');
select ok(exists(select 1 from pg_constraint where conrelid='public.lesson_completions'::regclass
 and contype='f' and pg_get_constraintdef(oid) like '%lesson_catalog%'),
 'unknown lesson IDs are blocked by catalog foreign key');
select ok(exists(select 1 from pg_constraint where conrelid='public.lesson_completions'::regclass
 and contype='p' and pg_get_constraintdef(oid) like '%student_user_id%'),
 'idempotent school, student and lesson composite key');
select ok(not has_table_privilege('anon','public.lesson_catalog','SELECT'),
 'anonymous sessions cannot enumerate even the catalog without sign-in');

select * from finish();
rollback;
