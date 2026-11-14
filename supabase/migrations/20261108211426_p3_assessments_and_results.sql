create table if not exists public.assessments (
 id uuid primary key default gen_random_uuid(),
 school_id uuid not null references public.schools(id),
 class_id uuid not null references public.classes(id),
 subject_id uuid not null references public.subjects(id),
 title text not null check(char_length(title) between 3 and 160),
 max_score numeric(8,2) not null check(max_score>0 and max_score<=10000),
 assessment_date date not null,
 created_by uuid not null references public.users(id),
 created_at timestamptz not null default now(),
 deleted_at timestamptz
);
create table if not exists public.assessment_results (
 id uuid primary key default gen_random_uuid(),
 assessment_id uuid not null references public.assessments(id),
 student_user_id uuid not null references public.users(id),
 score numeric(8,2) not null check(score>=0),
 feedback text not null default '',
 marked_by uuid not null references public.users(id),
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now(),
 unique(assessment_id,student_user_id)
);
create index if not exists assessments_school_class_idx on public.assessments(school_id,class_id) where deleted_at is null;
create index if not exists assessment_results_student_idx on public.assessment_results(student_user_id);
alter table public.assessments enable row level security;
alter table public.assessment_results enable row level security;
create policy assessments_staff_read on public.assessments for select to authenticated using
(deleted_at is null and school_id=(select app_private.current_school_id()) and app_private.can_manage_attendance(class_id));
create policy assessments_student_read on public.assessments for select to authenticated using
(deleted_at is null and school_id=(select app_private.current_school_id()) and exists(
select 1 from public.student_profiles sp where sp.class_id=assessments.class_id and sp.deleted_at is null and app_private.can_view_student(sp.user_id)));
create policy assessments_staff_insert on public.assessments for insert to authenticated with check
(school_id=(select app_private.current_school_id()) and created_by=(select auth.uid()) and app_private.can_manage_attendance(class_id)
and exists(select 1 from public.subjects s where s.id=subject_id and s.school_id=school_id));
create policy assessment_results_staff_read on public.assessment_results for select to authenticated using
(exists(select 1 from public.assessments a where a.id=assessment_id and a.deleted_at is null and app_private.can_manage_attendance(a.class_id)));
create policy assessment_results_private_read on public.assessment_results for select to authenticated using
(app_private.can_view_student(student_user_id) and exists(select 1 from public.assessments a where a.id=assessment_id and a.deleted_at is null and a.school_id=(select app_private.current_school_id())));
create policy assessment_results_staff_insert on public.assessment_results for insert to authenticated with check
(marked_by=(select auth.uid()) and exists(select 1 from public.assessments a join public.student_profiles sp on sp.class_id=a.class_id and sp.user_id=student_user_id and sp.deleted_at is null where a.id=assessment_id and a.deleted_at is null and app_private.can_manage_attendance(a.class_id) and score<=a.max_score));
create policy assessment_results_staff_update on public.assessment_results for update to authenticated using
(exists(select 1 from public.assessments a where a.id=assessment_id and a.deleted_at is null and app_private.can_manage_attendance(a.class_id)))
with check(marked_by=(select auth.uid()) and exists(select 1 from public.assessments a join public.student_profiles sp on sp.class_id=a.class_id and sp.user_id=student_user_id and sp.deleted_at is null where a.id=assessment_id and a.deleted_at is null and app_private.can_manage_attendance(a.class_id) and score<=a.max_score));
grant select,insert on public.assessments to authenticated;
grant select,insert,update on public.assessment_results to authenticated;