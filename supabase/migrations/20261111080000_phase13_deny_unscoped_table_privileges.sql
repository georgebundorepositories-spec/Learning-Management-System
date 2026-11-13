-- Phase 13: remove non-row-scoped database privileges from API identities.
-- TRUNCATE/REFERENCES are not protected by PostgreSQL RLS and MAINTAIN
-- can affect whole tables. Browsers and mobile clients never require them.
-- Preserve existing app-specific SELECT/INSERT/UPDATE/DELETE and RLS policies.
revoke truncate, references, trigger on all tables in schema public
 from anon, authenticated;

-- MAINTAIN exists on PostgreSQL 17+; tolerate older local test engines.
do $$
begin
 if current_setting('server_version_num')::integer >= 170000 then
  execute 'revoke maintain on all tables in schema public from anon, authenticated';
 end if;
end $$;

-- Supabase's postgres-owned defaults otherwise restore the dangerous grants
-- whenever an ordinary new application table is created.
alter default privileges for role postgres in schema public
 revoke truncate, references, trigger on tables from anon, authenticated;

do $$
begin
 if current_setting('server_version_num')::integer >= 170000 then
  execute 'alter default privileges for role postgres in schema public ' ||
   'revoke maintain on tables from anon, authenticated';
 end if;
end $$;

-- Supabase-managed supabase_admin defaults belong to a distinct owner and
-- cannot be altered by postgres without membership. Such system-owner
-- defaults require an independent platform-level review.
