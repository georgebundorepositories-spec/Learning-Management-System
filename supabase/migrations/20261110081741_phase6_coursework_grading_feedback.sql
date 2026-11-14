-- Phase 6: class coursework grades and teacher feedback with separate student read-only records.
alter table public.school_assignments
  add column if not exists max_score numeric(8,2) not null default 100
    check (max_score > 0 and max_score <= 10000);

-- An independent table prevents learner submission updates from touching grades.
create table public.assignment_reviews (
  id uuid primary key default gen_random_uuid(),
  school_id uuid not null references public.schools(id),
  submission_id uuid not null unique references public.assignment_submissions(id),
  score numeric(8,2) not null check(score >= 0 and score <= 10000),
  feedback text not null default '' check (char_length(feedback) <= 2000),
  reviewed_by uuid not null references public.users(id),
  reviewed_at timestamptz not null default now()
);
create index assignment_reviews_school_idx on public.assignment_reviews(school_id,reviewed_at desc);
create index assignment_reviews_reviewer_idx on public.assignment_reviews(reviewed_by);
alter table public.assignment_reviews enable row level security;
revoke all on public.assignment_reviews from public,anon,authenticated;
grant select,insert on public.assignment_reviews to authenticated;
grant update(score,feedback,reviewed_by,reviewed_at) on public.assignment_reviews to authenticated;

-- Teacher access must match the assigned subject, not any subject taught in the class.
drop policy if exists assignments_read_authorised on public.school_assignments;
create policy assignments_read_authorised on public.school_assignments
 for select to authenticated using(
  deleted_at is null and school_id=(select app_private.current_school_id())
  and (app_private.can_manage_assessment(class_id,subject_id) or exists(
     select 1 from public.student_profiles sp
     join public.users learner on learner.id=sp.user_id
     where sp.class_id=school_assignments.class_id and sp.deleted_at is null
       and learner.school_id=school_assignments.school_id
       and learner.is_active and learner.deleted_at is null
       and app_private.can_view_student(sp.user_id)
  ))
);
drop policy if exists assignment_submissions_read on public.assignment_submissions;
create policy assignment_submissions_read on public.assignment_submissions
for select to authenticated using(
  school_id=(select app_private.current_school_id())
  and exists(
    select 1 from public.school_assignments a
    where a.id=assignment_id and a.school_id=assignment_submissions.school_id
      and a.deleted_at is null
      and (
        student_user_id=(select auth.uid())
        or (select app_private.is_guardian_of(student_user_id))
        or app_private.can_manage_assessment(a.class_id,a.subject_id)
      )
  )
);

-- Published reviews prevent changing already-graded work and invalidating the score.
drop policy if exists assignment_submissions_student_update on public.assignment_submissions;
create policy assignment_submissions_student_update on public.assignment_submissions
for update to authenticated using(
 school_id=(select app_private.current_school_id())
 and student_user_id=(select auth.uid())
 and not exists(select 1 from public.assignment_reviews r where r.submission_id=id)
) with check(
 school_id=(select app_private.current_school_id())
 and student_user_id=(select auth.uid())
 and not exists(select 1 from public.assignment_reviews r where r.submission_id=id)
 and exists(
   select 1 from public.school_assignments a
   join public.student_profiles sp on sp.class_id=a.class_id
   where a.id=assignment_id and a.school_id=assignment_submissions.school_id
     and a.deleted_at is null and sp.user_id=student_user_id and sp.deleted_at is null
 )
);

create policy assignment_reviews_private_read on public.assignment_reviews
for select to authenticated using(
  school_id=(select app_private.current_school_id())
  and exists(
    select 1 from public.assignment_submissions s
    join public.school_assignments a on a.id=s.assignment_id
    where s.id=assignment_reviews.submission_id
      and s.school_id=assignment_reviews.school_id
      and a.school_id=assignment_reviews.school_id and a.deleted_at is null
      and (
        s.student_user_id=(select auth.uid())
        or (select app_private.is_guardian_of(s.student_user_id))
        or app_private.can_manage_assessment(a.class_id,a.subject_id)
      )
  )
);
create policy assignment_reviews_staff_insert on public.assignment_reviews
for insert to authenticated with check(
  school_id=(select app_private.current_school_id())
  and reviewed_by=(select auth.uid())
  and exists(
    select 1 from public.assignment_submissions s
    join public.school_assignments a on a.id=s.assignment_id
    where s.id=assignment_reviews.submission_id
      and s.school_id=assignment_reviews.school_id
      and a.school_id=assignment_reviews.school_id and a.deleted_at is null
      and app_private.can_manage_assessment(a.class_id,a.subject_id)
      and assignment_reviews.score<=a.max_score
  )
);
create policy assignment_reviews_staff_update on public.assignment_reviews
for update to authenticated using(
  school_id=(select app_private.current_school_id())
  and exists(
    select 1 from public.assignment_submissions s
    join public.school_assignments a on a.id=s.assignment_id
    where s.id=assignment_reviews.submission_id
      and s.school_id=assignment_reviews.school_id
      and a.school_id=assignment_reviews.school_id and a.deleted_at is null
      and app_private.can_manage_assessment(a.class_id,a.subject_id)
  )
) with check(
  school_id=(select app_private.current_school_id())
  and reviewed_by=(select auth.uid())
  and exists(
    select 1 from public.assignment_submissions s
    join public.school_assignments a on a.id=s.assignment_id
    where s.id=assignment_reviews.submission_id
      and s.school_id=assignment_reviews.school_id
      and a.school_id=assignment_reviews.school_id and a.deleted_at is null
      and app_private.can_manage_assessment(a.class_id,a.subject_id)
      and assignment_reviews.score<=a.max_score
  )
);
