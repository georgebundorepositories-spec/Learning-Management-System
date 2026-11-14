-- Phase 11: teacher-owned planning drafts. Drafts are never visible to learners or guardians.
create table public.teacher_lesson_plans (
 id uuid primary key default gen_random_uuid(),
 school_id uuid not null references public.schools(id),
 teacher_id uuid not null references public.users(id),
 class_id uuid not null references public.classes(id),
 subject_id uuid not null references public.subjects(id),
 source_lesson_id text not null references public.lesson_catalog(lesson_id),
 lesson_title text not null check (char_length(btrim(lesson_title)) between 3 and 180),
 objective text not null check (char_length(btrim(objective)) between 10 and 3000),
 teaching_steps text not null check (char_length(btrim(teaching_steps)) between 20 and 6000),
 resources text not null default '' check (char_length(resources) <= 3000),
 assessment text not null default '' check (char_length(assessment) <= 3000),
 reflection text not null default '' check (char_length(reflection) <= 3000),
 planned_on date,
 status text not null default 'draft' check (status in ('draft','ready')),
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now(),
 deleted_at timestamptz
);
create index teacher_lesson_plans_owner_idx
 on public.teacher_lesson_plans(school_id,teacher_id,updated_at desc) where deleted_at is null;
create index teacher_lesson_plans_class_fk_idx on public.teacher_lesson_plans(class_id);
create index teacher_lesson_plans_subject_fk_idx on public.teacher_lesson_plans(subject_id);
create index teacher_lesson_plans_source_fk_idx on public.teacher_lesson_plans(source_lesson_id);
alter table public.teacher_lesson_plans enable row level security;
revoke all on public.teacher_lesson_plans from public,anon,authenticated;
grant select on public.teacher_lesson_plans to authenticated;
grant insert(school_id,teacher_id,class_id,subject_id,source_lesson_id,
 lesson_title,objective,teaching_steps,resources,assessment,reflection,planned_on,status)
 on public.teacher_lesson_plans to authenticated;
-- Critical: links/ownership cannot be changed by UPDATE, even if application code is bypassed.
grant update(lesson_title,objective,teaching_steps,resources,assessment,reflection,planned_on,
 status,updated_at,deleted_at) on public.teacher_lesson_plans to authenticated;

create policy teacher_lesson_plans_staff_read on public.teacher_lesson_plans
 for select to authenticated using (
   deleted_at is null and school_id=(select app_private.current_school_id())
   and (teacher_id=(select auth.uid()) or (select app_private.is_school_admin()))
 );

create policy teacher_lesson_plans_teacher_insert on public.teacher_lesson_plans
 for insert to authenticated with check (
   deleted_at is null and school_id=(select app_private.current_school_id())
   and teacher_id=(select auth.uid())
   and exists (select 1 from public.users actor
    where actor.id=(select auth.uid()) and actor.role='teacher'
      and actor.is_active and actor.deleted_at is null and actor.school_id=teacher_lesson_plans.school_id)
   and (select app_private.can_manage_assessment(class_id,subject_id))
 );

create policy teacher_lesson_plans_teacher_update on public.teacher_lesson_plans
 for update to authenticated
 using (
   deleted_at is null and school_id=(select app_private.current_school_id())
   and teacher_id=(select auth.uid())
   and exists (select 1 from public.users actor
    where actor.id=(select auth.uid()) and actor.role='teacher'
      and actor.is_active and actor.deleted_at is null and actor.school_id=teacher_lesson_plans.school_id)
   and (select app_private.can_manage_assessment(class_id,subject_id))
 )
 with check (
   school_id=(select app_private.current_school_id())
   and teacher_id=(select auth.uid())
   and exists (select 1 from public.users actor
    where actor.id=(select auth.uid()) and actor.role='teacher'
      and actor.is_active and actor.deleted_at is null and actor.school_id=teacher_lesson_plans.school_id)
   and (select app_private.can_manage_assessment(class_id,subject_id))
 );
-- No DELETE privilege or policy; teacher may soft-archive their own plan via UPDATE.
