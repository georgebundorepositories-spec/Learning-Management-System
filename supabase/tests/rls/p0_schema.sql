-- Phase 0 pgTAP smoke assertions. Comprehensive role/school fixtures follow in P0 gate.
begin;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions;
select plan(4);

select ok(
  (select count(*) >= 11 from pg_class c join pg_namespace n on n.oid=c.relnamespace
    where n.nspname='public' and c.relkind='r'),
  'Initial identity and school tables are present'
);

select is(
  (select count(*) from pg_class c join pg_namespace n on n.oid=c.relnamespace
   where n.nspname='public' and c.relkind='r' and not c.relrowsecurity),
  0::bigint,
  'Every application table has RLS enabled'
);

select is(
  (select count(*) from pg_class c join pg_namespace n on n.oid=c.relnamespace
   where n.nspname='public' and c.relkind='r'
     and not exists(select 1 from pg_policy p where p.polrelid=c.oid)),
  0::bigint,
  'No application table is without an RLS policy'
);

select ok(
  (select count(*) >= 11 from pg_trigger t join pg_class c on c.oid=t.tgrelid
    join pg_namespace n on n.oid=c.relnamespace
    where n.nspname='public' and not t.tgisinternal
      and t.tgname like 'trg_%_version'),
  'Versioning trigger exists on each Phase 0 entity'
);

select * from finish();
rollback;
