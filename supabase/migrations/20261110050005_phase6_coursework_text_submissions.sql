-- Phase 6: text-based coursework, no file uploads or offline private caching.
create table public.school_assignments (
 id uuid primary key default gen_random_uuid(),
 school_id uuid not null references public.schools(id),
 class_id uuid not null references public.classes(id),
 subject_id uuid not null references public.subjects(id),
 title text not null check(length(btrim(title)) between 3 and 180),
 instructions text not null check(length(btrim(instructions)) between 3 and 5000),
 due_on date,
 created_by uuid not null references public.users(id),
 created_at timestamptz not null default now(),
 deleted_at timestamptz
);
create index school_assignments_school_class_idx on public.school_assignments(school_id,class_id,created_at desc) where deleted_at is null;
create index school_assignments_subject_fk_idx on public.school_assignments(subject_id);
create index school_assignments_created_by_fk_idx on public.school_assignments(created_by);
alter table public.school_assignments enable row level security;
revoke all on public.school_assignments from public, anon, authenticated;
grant select,insert on public.school_assignments to authenticated;

create policy assignments_read_authorised on public.school_assignments
 for select to authenticated using (
 deleted_at is null
 and school_id=(select app_private.current_school_id())
 and (app_private.can_manage_attendance(class_id) or exists (
   select 1 from public.student_profiles sp
   join public.users learner on learner.id=sp.user_id
   where sp.class_id=school_assignments.class_id
     and sp.deleted_at is null
     and learner.school_id=school_assignments.school_id
     and learner.is_active and learner.deleted_at is null
     and app_private.can_view_student(sp.user_id)
 ))
);

create policy assignments_insert_staff on public.school_assignments
 for insert to authenticated with check (
 school_id=(select app_private.current_school_id())
 and created_by=(select auth.uid())
 and deleted_at is null
 and exists(select 1 from public.subjects s where s.id=subject_id
  and s.school_id=school_assignments.school_id and s.deleted_at is null)
 and exists (
  select 1 from public.classes c
  join public.users actor on actor.id=(select auth.uid())
  where c.id=class_id and c.school_id=school_assignments.school_id
  and c.deleted_at is null and actor.is_active and actor.deleted_at is null
  and actor.school_id=school_assignments.school_id
  and (actor.role in ('admin','head') or
    (actor.role='teacher' and (c.teacher_id=actor.id or exists(
       select 1 from public.class_subject_teacher cst
       where cst.class_id=c.id and cst.subject_id=school_assignments.subject_id
         and cst.teacher_id=actor.id and cst.deleted_at is null
    )))
  )
 )
);

create table public.assignment_submissions (
 id uuid primary key default gen_random_uuid(),
 school_id uuid not null references public.schools(id),
 assignment_id uuid not null references public.school_assignments(id),
 student_user_id uuid not null references public.users(id),
 text_answer text not null check(length(btrim(text_answer)) between 1 and 5000),
 submitted_at timestamptz not null default now(),
 updated_at timestamptz not null default now(),
 unique(assignment_id,student_user_id)
);
create index assignment_submissions_school_idx on public.assignment_submissions(school_id,submitted_at desc);
create index assignment_submissions_student_idx on public.assignment_submissions(student_user_id);
alter table public.assignment_submissions enable row level security;
revoke all on public.assignment_submissions from public, anon, authenticated;
grant select,insert on public.assignment_submissions to authenticated;
grant update(text_answer,updated_at) on public.assignment_submissions to authenticated;

create policy assignment_submissions_read on public.assignment_submissions
 for select to authenticated using (
 school_id=(select app_private.current_school_id())
 and exists (
 select 1 from public.school_assignments a where a.id=assignment_id
   and a.school_id=assignment_submissions.school_id
   and a.deleted_at is null
   and (
     student_user_id=(select auth.uid())
     or (select app_private.is_guardian_of(student_user_id))
     or app_private.can_manage_attendance(a.class_id)
   )
 )
);

create policy assignment_submissions_student_insert on public.assignment_submissions
 for insert to authenticated with check (
 school_id=(select app_private.current_school_id())
 and student_user_id=(select auth.uid())
 and exists(
   select 1 from public.school_assignments a
   join public.student_profiles sp on sp.class_id=a.class_id
   join public.users learner on learner.id=sp.user_id
   where a.id=assignment_id and a.school_id=assignment_submissions.school_id
   and a.deleted_at is null and sp.user_id=student_user_id
   and sp.deleted_at is null and learner.is_active and learner.deleted_at is null
   and learner.role='student' and learner.school_id=assignment_submissions.school_id
 )
);

create policy assignment_submissions_student_update on public.assignment_submissions
 for update to authenticated using (
 school_id=(select app_private.current_school_id()) and student_user_id=(select auth.uid())
) with check (
 school_id=(select app_private.current_school_id()) and student_user_id=(select auth.uid())
 and exists(
   select 1 from public.school_assignments a
   join public.student_profiles sp on sp.class_id=a.class_id
   where a.id=assignment_id and a.school_id=assignment_submissions.school_id
   and a.deleted_at is null and sp.user_id=student_user_id and sp.deleted_at is null
 )
);
