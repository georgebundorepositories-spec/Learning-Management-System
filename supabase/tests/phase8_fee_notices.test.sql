-- Phase 8 regression checks. Executed transactionally by "supabase test db".
begin;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions, pg_catalog;
select plan(12);
select ok(to_regclass('public.fee_invoices') is not null, 'fee invoices table exists');
select ok(to_regclass('public.school_notices') is not null, 'in-app notices table exists');
select ok((select relrowsecurity from pg_class where oid='public.fee_invoices'::regclass), 'fee invoice RLS enabled');
select ok((select relrowsecurity from pg_class where oid='public.school_notices'::regclass), 'notice RLS enabled');
select ok(exists(select 1 from pg_policies where schemaname='public' and tablename='fee_invoices' and policyname='fee_invoices_private_read' and cmd='SELECT'), 'invoice reading is policy constrained');
select ok(exists(select 1 from pg_policies where schemaname='public' and tablename='fee_invoices' and policyname='fee_invoices_staff_insert' and cmd='INSERT'), 'invoice issuance is policy constrained');
select ok(exists(select 1 from pg_policies where schemaname='public' and tablename='school_notices' and policyname='school_notices_audience_read' and cmd='SELECT'), 'notices are audience filtered');
select ok(exists(select 1 from pg_policies where schemaname='public' and tablename='school_notices' and policyname='school_notices_admin_insert' and cmd='INSERT'), 'notice authors are policy constrained');
select ok(exists(select 1 from pg_indexes where schemaname='public' and indexname='fee_invoices_school_request_uidx'), 'invoices have duplicate-request protection');
select ok(exists(select 1 from pg_indexes where schemaname='public' and indexname='school_notices_school_request_uidx'), 'notices have duplicate-request protection');
select ok(not exists(select 1 from pg_policies where schemaname='public' and tablename in ('fee_invoices','school_notices') and 'anon' = any(roles)), 'no anonymous-access policy');
select ok((select count(*) from pg_policies where schemaname='public' and tablename in ('fee_invoices','school_notices') and cmd='UPDATE')=0, 'billing and notices are append-only to ordinary clients');
select * from finish();
rollback;
