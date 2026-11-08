-- Phase 4/5: invoice issuance foundation and in-app school notices.
-- Deliberately NO payment settlement or external delivery API.
create table if not exists public.fee_invoices (
 id uuid primary key default gen_random_uuid(),
 school_id uuid not null references public.schools(id),
 student_user_id uuid not null references public.users(id),
 description text not null check (length(trim(description)) between 3 and 300),
 amount numeric(12,2) not null check (amount > 0 and amount <= 100000000),
 currency text not null default 'USD' check (currency in ('USD','ZWG')),
 due_on date,
 status text not null default 'issued' check (status in ('issued','void')),
 created_by uuid not null references public.users(id),
 created_at timestamptz not null default now()
);
create index if not exists fee_invoices_student_idx
 on public.fee_invoices(school_id,student_user_id,created_at desc);
alter table public.fee_invoices enable row level security;
grant select,insert on public.fee_invoices to authenticated;

drop policy if exists fee_invoices_private_read on public.fee_invoices;
create policy fee_invoices_private_read on public.fee_invoices
 for select to authenticated using (
 school_id=(select app_private.current_school_id())
 and exists(select 1 from public.users actor where actor.id=(select auth.uid())
  and actor.school_id=fee_invoices.school_id and actor.is_active and actor.deleted_at is null
  and (
   actor.role in ('admin','head','bursar')
   or (fee_invoices.status='issued' and (
    fee_invoices.student_user_id=actor.id
    or (actor.role='parent' and app_private.is_guardian_of(fee_invoices.student_user_id))
   ))
  )
 )
);
drop policy if exists fee_invoices_staff_insert on public.fee_invoices;
create policy fee_invoices_staff_insert on public.fee_invoices
 for insert to authenticated with check (
 school_id=(select app_private.current_school_id())
 and created_by=(select auth.uid())
 and status='issued'
 and exists(select 1 from public.users actor where actor.id=(select auth.uid()) and actor.is_active and actor.deleted_at is null
 and actor.school_id=fee_invoices.school_id and actor.role in ('admin','head','bursar'))
 and exists(select 1 from public.users learner where learner.id=fee_invoices.student_user_id
 and learner.school_id=fee_invoices.school_id and learner.role='student' and learner.is_active and learner.deleted_at is null)
);

create table if not exists public.school_notices (
 id uuid primary key default gen_random_uuid(),
 school_id uuid not null references public.schools(id),
 title text not null check(length(trim(title)) between 3 and 160),
 body text not null check(length(trim(body)) between 3 and 4000),
 audience_role text not null default 'all' check(audience_role in ('all','admin','head','teacher','student','parent','bursar')),
 published_by uuid not null references public.users(id),
 created_at timestamptz not null default now(),
 expires_at timestamptz,
 constraint notice_expiry_valid check(expires_at is null or expires_at>created_at)
);
create index if not exists school_notices_current_idx on public.school_notices(school_id,created_at desc);
alter table public.school_notices enable row level security;
grant select,insert on public.school_notices to authenticated;

drop policy if exists school_notices_audience_read on public.school_notices;
create policy school_notices_audience_read on public.school_notices
 for select to authenticated using (
 school_id=(select app_private.current_school_id())
 and (expires_at is null or expires_at>now())
 and exists(select 1 from public.users actor
 where actor.id=(select auth.uid()) and actor.school_id=school_notices.school_id
 and actor.is_active and actor.deleted_at is null
 and (audience_role='all' or audience_role=actor.role::text))
);
drop policy if exists school_notices_admin_insert on public.school_notices;
create policy school_notices_admin_insert on public.school_notices
 for insert to authenticated with check (
 school_id=(select app_private.current_school_id())
 and published_by=(select auth.uid())
 and exists(select 1 from public.users actor where actor.id=(select auth.uid())
 and actor.school_id=school_notices.school_id and actor.is_active and actor.deleted_at is null
 and actor.role in ('admin','head'))
);