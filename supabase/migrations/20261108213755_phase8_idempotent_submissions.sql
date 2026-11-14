-- Phase 8: repeat submissions from slow/offline clients must not duplicate school charges or notices.
alter table public.fee_invoices add column if not exists request_key uuid not null default gen_random_uuid();
create unique index if not exists fee_invoices_school_request_uidx on public.fee_invoices(school_id,request_key);
alter table public.school_notices add column if not exists request_key uuid not null default gen_random_uuid();
create unique index if not exists school_notices_school_request_uidx on public.school_notices(school_id,request_key);