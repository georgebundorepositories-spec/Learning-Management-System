-- Regression: non-row-scoped permissions must never be granted to web/mobile users.
begin;
create extension if not exists pgtap with schema extensions;
set local search_path=public, extensions, pg_catalog;
select plan(9);

select ok(not has_table_privilege('anon','public.classes','TRUNCATE'),
 'anonymous clients cannot truncate class records');
select ok(not has_table_privilege('authenticated','public.classes','TRUNCATE'),
 'signed-in users cannot truncate any class');
select ok(not has_table_privilege('anon','public.users','TRUNCATE'),
 'anonymous clients cannot truncate memberships');
select ok(not has_table_privilege('authenticated','public.users','TRUNCATE'),
 'signed-in users cannot truncate memberships');
select ok(not has_table_privilege('anon','public.student_profiles','REFERENCES'),
 'anonymous clients cannot create unrestricted foreign references');
select ok(not has_table_privilege('authenticated','public.student_profiles','TRIGGER'),
 'authenticated users cannot create row-bypassing triggers');
select ok(not exists(
 select 1 from pg_class c
 join pg_namespace n on n.oid=c.relnamespace
 where n.nspname='public' and c.relkind in ('r','p') and (
  has_table_privilege('anon',c.oid,'TRUNCATE') or
  has_table_privilege('authenticated',c.oid,'TRUNCATE') or
  has_table_privilege('anon',c.oid,'REFERENCES') or
  has_table_privilege('authenticated',c.oid,'REFERENCES') or
  has_table_privilege('anon',c.oid,'TRIGGER') or
  has_table_privilege('authenticated',c.oid,'TRIGGER')
 )
), 'all current application tables deny non-RLS privileges');

select ok(not exists(
 select 1 from pg_default_acl d
 join pg_namespace n on n.oid=d.defaclnamespace
 cross join lateral aclexplode(d.defaclacl) a
 where n.nspname='public'
  and d.defaclrole='postgres'::regrole and d.defaclobjtype='r'
  and a.grantee in ('anon'::regrole::oid,'authenticated'::regrole::oid)
  and a.privilege_type in ('TRUNCATE','REFERENCES','TRIGGER','MAINTAIN')
), 'postgres-created future tables do not restore dangerous grants');

select ok(
 current_setting('server_version_num')::integer < 170000
 or (
   not has_table_privilege('anon','public.classes','MAINTAIN')
   and not has_table_privilege('authenticated','public.classes','MAINTAIN')
 ),
 'PostgreSQL 17+ whole-table maintenance is unavailable to application roles'
);
select * from finish();
rollback;
