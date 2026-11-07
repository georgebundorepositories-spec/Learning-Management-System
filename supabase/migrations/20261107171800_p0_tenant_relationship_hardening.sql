-- P0-T4: tenant-bound relationships and access revocation for inactive accounts.
create or replace function app_private.is_guardian_of(student uuid)
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.student_guardian_link l
    join public.users s on s.id = l.student_user_id
    join public.users g on g.id = l.guardian_user_id
    where l.student_user_id = student and l.guardian_user_id = (select auth.uid())
      and s.school_id = g.school_id and s.is_active and g.is_active
      and s.deleted_at is null and g.deleted_at is null and l.deleted_at is null
  )
$$;

drop policy users_read on public.users;
create policy users_read on public.users for select to authenticated
using (
  (id = (select auth.uid()) and is_active and deleted_at is null)
  or ((select app_private.is_school_admin())
      and school_id = (select app_private.current_school_id()))
);

alter policy staff_profiles_read on public.staff_profiles
using (deleted_at is null and (select app_private.current_school_id()) is not null
  and (user_id = (select auth.uid()) or
       ((select app_private.is_school_admin()) and user_id in
        (select id from public.users where school_id = (select app_private.current_school_id())))));
alter policy student_profiles_read on public.student_profiles
using (deleted_at is null and (select app_private.current_school_id()) is not null
  and (user_id = (select auth.uid()) or (select app_private.is_guardian_of(user_id))
       or ((select app_private.is_school_admin()) and user_id in
        (select id from public.users where school_id = (select app_private.current_school_id())))));
alter policy parent_profiles_read on public.parent_profiles
using (deleted_at is null and (select app_private.current_school_id()) is not null
  and (user_id = (select auth.uid()) or
       ((select app_private.is_school_admin()) and user_id in
        (select id from public.users where school_id = (select app_private.current_school_id())))));
alter policy guardians_read on public.student_guardian_link
using (deleted_at is null and (select app_private.current_school_id()) is not null
  and ( (guardian_user_id = (select auth.uid())
           and (select app_private.is_guardian_of(student_user_id)))
        or student_user_id = (select auth.uid())
        or ((select app_private.is_school_admin()) and student_user_id in
            (select id from public.users where school_id = (select app_private.current_school_id()))) ));

-- Validate cross-entity school integrity even for writes made through privileged APIs.
create or replace function app_private.ensure_tenant_consistency()
returns trigger language plpgsql security definer set search_path = '' as $$
declare
  school uuid;
  ref_school uuid;
begin
  if TG_TABLE_NAME = 'terms' then
    select school_id into ref_school from public.academic_years where id = new.academic_year_id;
    if ref_school is distinct from new.school_id then
      raise exception 'term and academic year must belong to same school';
    end if;
  elsif TG_TABLE_NAME = 'classes' then
    if new.academic_year_id is not null then
      select school_id into ref_school from public.academic_years where id = new.academic_year_id;
      if ref_school is distinct from new.school_id then
        raise exception 'class and academic year must belong to same school';
      end if;
    end if;
    if new.teacher_id is not null then
      select school_id into ref_school from public.users where id = new.teacher_id;
      if ref_school is distinct from new.school_id then
        raise exception 'class teacher must belong to same school';
      end if;
    end if;
  elsif TG_TABLE_NAME = 'class_subject_teacher' then
    select school_id into school from public.classes where id = new.class_id;
    select school_id into ref_school from public.subjects where id = new.subject_id;
    if ref_school is distinct from school then raise exception 'subject belongs to different school'; end if;
    select school_id into ref_school from public.users where id = new.teacher_id;
    if ref_school is distinct from school then raise exception 'teacher belongs to different school'; end if;
    select school_id into ref_school from public.terms where id = new.term_id;
    if ref_school is distinct from school then raise exception 'term belongs to different school'; end if;
  elsif TG_TABLE_NAME = 'student_guardian_link' then
    select school_id into school from public.users where id = new.student_user_id and role = 'student';
    select school_id into ref_school from public.users where id = new.guardian_user_id and role = 'parent';
    if school is null or ref_school is distinct from school then
      raise exception 'student and guardian must belong to same school with correct roles';
    end if;
  end if;
  return new;
end;
$$;
revoke all on function app_private.ensure_tenant_consistency() from public, anon, authenticated;
create trigger terms_consistency before insert or update on public.terms
for each row execute function app_private.ensure_tenant_consistency();
create trigger classes_consistency before insert or update on public.classes
for each row execute function app_private.ensure_tenant_consistency();
create trigger class_subject_teacher_consistency before insert or update on public.class_subject_teacher
for each row execute function app_private.ensure_tenant_consistency();
create trigger student_guardian_link_consistency before insert or update on public.student_guardian_link
for each row execute function app_private.ensure_tenant_consistency();

-- Foreign-key indexes recommended by Supabase database advisors.
create index idx_academic_years_school_id on public.academic_years(school_id);
create index idx_assignments_subject_id on public.class_subject_teacher(subject_id);
create index idx_assignments_teacher_id on public.class_subject_teacher(teacher_id);
create index idx_assignments_term_id on public.class_subject_teacher(term_id);
create index idx_classes_academic_year_id on public.classes(academic_year_id);
create index idx_classes_teacher_id on public.classes(teacher_id);
create index idx_guardians_guardian_id on public.student_guardian_link(guardian_user_id);
create index idx_terms_year_id on public.terms(academic_year_id);
create index idx_terms_school_id on public.terms(school_id);
