-- P0-T2: shared schema primitives
create extension if not exists pgcrypto;
create schema if not exists app_private;
revoke all on schema app_private from public, anon, authenticated;
create or replace function public.set_updated_at_and_version()
returns trigger language plpgsql set search_path = '' as $$
begin
  new.updated_at = now();
  new.sync_version = coalesce(old.sync_version, 0) + 1;
  return new;
end;
$$;
revoke all on function public.set_updated_at_and_version() from public, anon, authenticated;
