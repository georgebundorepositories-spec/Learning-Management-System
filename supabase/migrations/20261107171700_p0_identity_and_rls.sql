-- P0-T3: identity and school data model
-- Security improvement over original spec:
-- offline PIN verifier is kept on the encrypted device, NOT in an API-exposed public.users column.
-- User self-edits cannot alter role, school_id, or is_active.
create type public.user_role as enum ('admin','head','teacher','student','parent','bursar');
create type public.curriculum as enum ('zimsec','cambridge');
create table public.schools (
  id uuid primary key default gen_random_uuid(),
    name text not null,
    emis_number text unique,
    type text check (type in ('primary','secondary','combined')),
    province text, district text,
    term_system integer not null default 3 check(term_system between 1 and 4),
    currency text not null default 'USD',
    locale_default text not null default 'en' check(locale_default in ('en','sn','nd')),
    logo_url text,
    theme jsonb not null default '{"primary":"#0B6E4F","secondary":"#F4A261"}'::jsonb,
    contact_phone text, contact_email text,
    timezone text not null default 'Africa/Harare',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1
);
create table public.users (
  id uuid primary key references auth.users(id) on delete cascade,
    school_id uuid not null references public.schools(id) on delete cascade,
    phone text unique not null, email text, full_name text not null,
    role public.user_role not null,
    preferred_lang text not null default 'en' check (preferred_lang in ('en','sn','nd')),
    avatar_url text, is_active boolean not null default true,
    last_login_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1
);
create table public.staff_profiles (
  id uuid primary key default gen_random_uuid(),
    user_id uuid not null unique references public.users(id) on delete cascade,
    employee_no text, subjects text[], classes uuid[], qualifications text, hire_date date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1
);
create table public.student_profiles (
  id uuid primary key default gen_random_uuid(),
    user_id uuid not null unique references public.users(id) on delete cascade,
    admission_no text, dob date, class_id uuid, stream text,
    house text, enrolled_at date default current_date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1
);
create table public.parent_profiles (
  id uuid primary key default gen_random_uuid(),
    user_id uuid not null unique references public.users(id) on delete cascade,
    relationship text, occupation text, national_id text,
    preferred_channel text not null default 'sms' check(preferred_channel in ('sms','whatsapp','push')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1
);
create table public.student_guardian_link (
  id uuid primary key default gen_random_uuid(),
    student_user_id uuid not null references public.users(id) on delete cascade,
    guardian_user_id uuid not null references public.users(id) on delete cascade,
    is_primary boolean not null default false,
    can_pay_fees boolean not null default true,
    unique(student_user_id, guardian_user_id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1
);
create table public.academic_years (
  id uuid primary key default gen_random_uuid(),
    school_id uuid not null references public.schools(id) on delete cascade,
    name text not null, start_date date, end_date date,
    is_current boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1
);
create table public.terms (
  id uuid primary key default gen_random_uuid(),
    school_id uuid not null references public.schools(id) on delete cascade,
    academic_year_id uuid not null references public.academic_years(id) on delete cascade,
    name text not null, term_number integer not null check(term_number between 1 and 4),
    start_date date, end_date date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1
);
create table public.classes (
  id uuid primary key default gen_random_uuid(),
    school_id uuid not null references public.schools(id) on delete cascade,
    name text not null, grade_level text, stream text,
    teacher_id uuid references public.users(id),
    academic_year_id uuid references public.academic_years(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1
);
create table public.subjects (
  id uuid primary key default gen_random_uuid(),
    school_id uuid not null references public.schools(id) on delete cascade,
    name text not null, code text,
    curriculum public.curriculum not null default 'zimsec',
    grade_levels text[],
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1
);
create table public.class_subject_teacher (
  id uuid primary key default gen_random_uuid(),
    class_id uuid not null references public.classes(id) on delete cascade,
    subject_id uuid not null references public.subjects(id) on delete cascade,
    teacher_id uuid not null references public.users(id),
    term_id uuid not null references public.terms(id),
    unique(class_id, subject_id, term_id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  sync_version integer not null default 1
);
create index idx_users_school_role on public.users(school_id, role) where deleted_at is null;
create unique index idx_student_admission on public.student_profiles(admission_no) where deleted_at is null;
create index idx_classes_school on public.classes(school_id) where deleted_at is null;
create index idx_subjects_school on public.subjects(school_id) where deleted_at is null;
-- Always apply the version trigger to *every* mutable entity, not a partial subset.

create trigger trg_schools_version before update on public.schools
  for each row execute function public.set_updated_at_and_version();
create trigger trg_users_version before update on public.users
  for each row execute function public.set_updated_at_and_version();
create trigger trg_staff_profiles_version before update on public.staff_profiles
  for each row execute function public.set_updated_at_and_version();
create trigger trg_student_profiles_version before update on public.student_profiles
  for each row execute function public.set_updated_at_and_version();
create trigger trg_parent_profiles_version before update on public.parent_profiles
  for each row execute function public.set_updated_at_and_version();
create trigger trg_student_guardian_link_version before update on public.student_guardian_link
  for each row execute function public.set_updated_at_and_version();
create trigger trg_academic_years_version before update on public.academic_years
  for each row execute function public.set_updated_at_and_version();
create trigger trg_terms_version before update on public.terms
  for each row execute function public.set_updated_at_and_version();
create trigger trg_classes_version before update on public.classes
  for each row execute function public.set_updated_at_and_version();
create trigger trg_subjects_version before update on public.subjects
  for each row execute function public.set_updated_at_and_version();
create trigger trg_class_subject_teacher_version before update on public.class_subject_teacher
  for each row execute function public.set_updated_at_and_version();

-- Security definer helpers avoid recursive users-table RLS evaluation.
-- Private schema is NOT exposed through PostgREST.
create function app_private.current_school_id()
returns uuid language sql stable security definer set search_path = '' as $$
  select school_id from public.users
  where id = (select auth.uid()) and is_active and deleted_at is null
  limit 1
$$;
create function app_private.current_role()
returns public.user_role language sql stable security definer set search_path = '' as $$
  select role from public.users
  where id = (select auth.uid()) and is_active and deleted_at is null
  limit 1
$$;
create function app_private.is_school_admin()
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.users
    where id = (select auth.uid()) and role in ('admin','head')
      and is_active and deleted_at is null
  )
$$;
create function app_private.is_guardian_of(student uuid)
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.student_guardian_link l
    join public.users student_profile on student_profile.id = l.student_user_id
    join public.users guardian_profile on guardian_profile.id = l.guardian_user_id
    where l.student_user_id = student
      and l.guardian_user_id = (select auth.uid())
      and student_profile.school_id = guardian_profile.school_id
      and l.deleted_at is null
  )
$$;
revoke all on all functions in schema app_private from public, anon, authenticated;
grant usage on schema app_private to authenticated;
grant execute on function app_private.current_school_id() to authenticated;
grant execute on function app_private.current_role() to authenticated;
grant execute on function app_private.is_school_admin() to authenticated;
grant execute on function app_private.is_guardian_of(uuid) to authenticated;

alter table public.schools enable row level security;
alter table public.users enable row level security;
alter table public.staff_profiles enable row level security;
alter table public.student_profiles enable row level security;
alter table public.parent_profiles enable row level security;
alter table public.student_guardian_link enable row level security;
alter table public.academic_years enable row level security;
alter table public.terms enable row level security;
alter table public.classes enable row level security;
alter table public.subjects enable row level security;
alter table public.class_subject_teacher enable row level security;

-- Only the signed-in school can read its own school profile.
create policy schools_read on public.schools for select to authenticated
  using (id = (select app_private.current_school_id()) and deleted_at is null);
create policy schools_admin_update on public.schools for update to authenticated
  using (id = (select app_private.current_school_id()) and (select app_private.is_school_admin()))
  with check (id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));

-- Regular users may read their own profile. Only same-school admins can read
-- and manage other users. In particular, self-editing role is prohibited.
create policy users_read on public.users for select to authenticated
  using (
    id = (select auth.uid()) or
    ((select app_private.is_school_admin()) and school_id = (select app_private.current_school_id()))
  );
create policy users_admin_insert on public.users for insert to authenticated
  with check (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));
create policy users_admin_update on public.users for update to authenticated
  using (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()))
  with check (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));


create policy academic_years_read on public.academic_years for select to authenticated
  using (school_id = (select app_private.current_school_id()) and deleted_at is null);
create policy academic_years_admin_insert on public.academic_years for insert to authenticated
  with check (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));
create policy academic_years_admin_update on public.academic_years for update to authenticated
  using (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()))
  with check (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));
create policy academic_years_admin_delete on public.academic_years for delete to authenticated
  using (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));

create policy terms_read on public.terms for select to authenticated
  using (school_id = (select app_private.current_school_id()) and deleted_at is null);
create policy terms_admin_insert on public.terms for insert to authenticated
  with check (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));
create policy terms_admin_update on public.terms for update to authenticated
  using (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()))
  with check (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));
create policy terms_admin_delete on public.terms for delete to authenticated
  using (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));

create policy classes_read on public.classes for select to authenticated
  using (school_id = (select app_private.current_school_id()) and deleted_at is null);
create policy classes_admin_insert on public.classes for insert to authenticated
  with check (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));
create policy classes_admin_update on public.classes for update to authenticated
  using (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()))
  with check (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));
create policy classes_admin_delete on public.classes for delete to authenticated
  using (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));

create policy subjects_read on public.subjects for select to authenticated
  using (school_id = (select app_private.current_school_id()) and deleted_at is null);
create policy subjects_admin_insert on public.subjects for insert to authenticated
  with check (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));
create policy subjects_admin_update on public.subjects for update to authenticated
  using (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()))
  with check (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));
create policy subjects_admin_delete on public.subjects for delete to authenticated
  using (school_id = (select app_private.current_school_id()) and (select app_private.is_school_admin()));

create policy staff_profiles_read on public.staff_profiles for select to authenticated
  using (deleted_at is null and
    (user_id = (select auth.uid())  or
      ((select app_private.is_school_admin()) and user_id in
       (select id from public.users where school_id = (select app_private.current_school_id())))));
create policy staff_profiles_admin_insert on public.staff_profiles for insert to authenticated
  with check ((select app_private.is_school_admin()) and user_id in
    (select id from public.users where school_id = (select app_private.current_school_id())));
create policy staff_profiles_admin_update on public.staff_profiles for update to authenticated
  using ((select app_private.is_school_admin()) and user_id in
    (select id from public.users where school_id = (select app_private.current_school_id())))
  with check ((select app_private.is_school_admin()) and user_id in
    (select id from public.users where school_id = (select app_private.current_school_id())));
create policy staff_profiles_admin_delete on public.staff_profiles for delete to authenticated
  using ((select app_private.is_school_admin()) and user_id in
    (select id from public.users where school_id = (select app_private.current_school_id())));

create policy student_profiles_read on public.student_profiles for select to authenticated
  using (deleted_at is null and
    (user_id = (select auth.uid()) or (select app_private.is_guardian_of(user_id)) or
      ((select app_private.is_school_admin()) and user_id in
       (select id from public.users where school_id = (select app_private.current_school_id())))));
create policy student_profiles_admin_insert on public.student_profiles for insert to authenticated
  with check ((select app_private.is_school_admin()) and user_id in
    (select id from public.users where school_id = (select app_private.current_school_id())));
create policy student_profiles_admin_update on public.student_profiles for update to authenticated
  using ((select app_private.is_school_admin()) and user_id in
    (select id from public.users where school_id = (select app_private.current_school_id())))
  with check ((select app_private.is_school_admin()) and user_id in
    (select id from public.users where school_id = (select app_private.current_school_id())));
create policy student_profiles_admin_delete on public.student_profiles for delete to authenticated
  using ((select app_private.is_school_admin()) and user_id in
    (select id from public.users where school_id = (select app_private.current_school_id())));

create policy parent_profiles_read on public.parent_profiles for select to authenticated
  using (deleted_at is null and
    (user_id = (select auth.uid())  or
      ((select app_private.is_school_admin()) and user_id in
       (select id from public.users where school_id = (select app_private.current_school_id())))));
create policy parent_profiles_admin_insert on public.parent_profiles for insert to authenticated
  with check ((select app_private.is_school_admin()) and user_id in
    (select id from public.users where school_id = (select app_private.current_school_id())));
create policy parent_profiles_admin_update on public.parent_profiles for update to authenticated
  using ((select app_private.is_school_admin()) and user_id in
    (select id from public.users where school_id = (select app_private.current_school_id())))
  with check ((select app_private.is_school_admin()) and user_id in
    (select id from public.users where school_id = (select app_private.current_school_id())));
create policy parent_profiles_admin_delete on public.parent_profiles for delete to authenticated
  using ((select app_private.is_school_admin()) and user_id in
    (select id from public.users where school_id = (select app_private.current_school_id())));

create policy guardians_read on public.student_guardian_link for select to authenticated
  using (deleted_at is null and (
     guardian_user_id = (select auth.uid()) or student_user_id = (select auth.uid())
     or ((select app_private.is_school_admin()) and student_user_id in
       (select id from public.users where school_id = (select app_private.current_school_id())))
  ));
create policy guardians_admin_insert on public.student_guardian_link for insert to authenticated
  with check ((select app_private.is_school_admin())
    and student_user_id in (select id from public.users where school_id = (select app_private.current_school_id()) and role='student')
    and guardian_user_id in (select id from public.users where school_id = (select app_private.current_school_id()) and role='parent'));
create policy guardians_admin_update on public.student_guardian_link for update to authenticated
  using ((select app_private.is_school_admin())
    and student_user_id in (select id from public.users where school_id = (select app_private.current_school_id())))
  with check ((select app_private.is_school_admin())
    and student_user_id in (select id from public.users where school_id = (select app_private.current_school_id()) and role='student')
    and guardian_user_id in (select id from public.users where school_id = (select app_private.current_school_id()) and role='parent'));
create policy guardians_admin_delete on public.student_guardian_link for delete to authenticated
  using ((select app_private.is_school_admin())
    and student_user_id in (select id from public.users where school_id = (select app_private.current_school_id())));

create policy assignments_read on public.class_subject_teacher for select to authenticated
  using (deleted_at is null and
    (teacher_id = (select auth.uid()) or
      ((select app_private.is_school_admin()) and class_id in
        (select id from public.classes where school_id = (select app_private.current_school_id())))));
create policy assignments_admin_insert on public.class_subject_teacher for insert to authenticated
  with check ((select app_private.is_school_admin())
    and class_id in (select id from public.classes where school_id = (select app_private.current_school_id()))
    and subject_id in (select id from public.subjects where school_id = (select app_private.current_school_id()))
    and teacher_id in (select id from public.users where school_id = (select app_private.current_school_id()) and role='teacher')
    and term_id in (select id from public.terms where school_id = (select app_private.current_school_id())));
create policy assignments_admin_update on public.class_subject_teacher for update to authenticated
  using ((select app_private.is_school_admin()) and class_id in
    (select id from public.classes where school_id = (select app_private.current_school_id())))
  with check ((select app_private.is_school_admin())
    and class_id in (select id from public.classes where school_id = (select app_private.current_school_id()))
    and subject_id in (select id from public.subjects where school_id = (select app_private.current_school_id()))
    and teacher_id in (select id from public.users where school_id = (select app_private.current_school_id()) and role='teacher')
    and term_id in (select id from public.terms where school_id = (select app_private.current_school_id())));
create policy assignments_admin_delete on public.class_subject_teacher for delete to authenticated
  using ((select app_private.is_school_admin()) and class_id in
    (select id from public.classes where school_id = (select app_private.current_school_id())));

-- Schema privilege grants are separate from row-level policies.
grant select, update on public.schools to authenticated;
grant select, insert, update on public.users to authenticated;
grant select, insert, update, delete on
  public.staff_profiles, public.student_profiles, public.parent_profiles,
  public.student_guardian_link, public.academic_years, public.terms,
  public.classes, public.subjects, public.class_subject_teacher to authenticated;

