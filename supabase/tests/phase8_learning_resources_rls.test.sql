-- Subject-specific learning-material publication policy regression.
begin;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions, pg_catalog;
select plan(11);

select ok(to_regprocedure('app_private.can_publish_subject(uuid)') is not null,
  'subject-scoped resource authorisation helper exists');
select ok((select prosecdef from pg_proc
  where oid='app_private.can_publish_subject(uuid)'::regprocedure),
  'resource permission helper uses protected internal lookup');
select ok(has_function_privilege('authenticated',
  'app_private.can_publish_subject(uuid)','EXECUTE'),
  'authenticated RLS policies may check subject access');
select ok(not has_function_privilege('anon',
  'app_private.can_publish_subject(uuid)','EXECUTE'),
  'anon may not execute the resource helper');
select ok((select relrowsecurity from pg_class
  where oid='public.learning_resources'::regclass),
  'resource table has RLS');
select ok(exists(select 1 from pg_policies
  where schemaname='public' and tablename='learning_resources'
  and policyname='learning_resources_insert'
  and cmd='INSERT' and with_check like '%can_publish_subject%'),
  'resource creation requires assigned subject');
select ok(exists(select 1 from pg_policies
  where schemaname='public' and tablename='learning_resources'
  and policyname='learning_resources_update'
  and cmd='UPDATE' and with_check like '%can_publish_subject%'),
  'resource changes must preserve subject permission');
select ok(not has_column_privilege('authenticated','public.learning_resources',
  'school_id','UPDATE'), 'staff cannot move resource to another school');
select ok(not has_column_privilege('authenticated','public.learning_resources',
  'created_by','UPDATE'), 'staff cannot reassign resource ownership');
select ok(has_column_privilege('authenticated','public.learning_resources',
  'title','UPDATE'), 'authorised editors can update a resource title');
select ok(not exists(select 1 from pg_policies
  where schemaname='public' and tablename='learning_resources'
  and 'anon' = any(roles)), 'anonymous users have no resource policy');
select * from finish();
rollback;
