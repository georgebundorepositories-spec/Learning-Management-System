-- ZimSchool LMS demo dataset guardrail.
-- Explicitly mark fictional schools: never mistake these for real enrolments.
alter table public.schools add column if not exists is_demo boolean not null default false;
comment on column public.schools.is_demo is
 'True only for synthetic, non-production demonstration schools. Never store real student records in demo schools.';
