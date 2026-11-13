-- Phase 10: authoritative, append-only learner self-check completion ledger.
-- Formative practice only: never equate completion with a teacher assessment or grade.
create table public.lesson_catalog (
  lesson_id text primary key check(lesson_id ~ '^[a-z0-9][a-z0-9-]{2,120}$'),
  content_source text not null check(content_source in ('starter','curriculum')),
  grade_level text not null check(char_length(grade_level) between 3 and 40)
);
insert into public.lesson_catalog (lesson_id,content_source,grade_level) values
  ('ecd-b-english-language-sounds-around-us','curriculum','ECD B'),
  ('ecd-b-english-language-greetings-and-polite-replies','curriculum','ECD B'),
  ('ecd-b-english-language-sequencing-picture-stories','curriculum','ECD B'),
  ('ecd-b-english-language-tracing-patterns-and-the-letter-o','curriculum','ECD B'),
  ('ecd-b-science-and-technology-washing-hands-with-soap','curriculum','ECD B'),
  ('ecd-b-science-and-technology-foods-from-plants-and-animals','curriculum','ECD B'),
  ('ecd-b-indigenous-languages-greetings-and-unhu','curriculum','ECD B'),
  ('grade-1-english-language-following-two-instructions','curriculum','Grade 1'),
  ('grade-2-science-and-technology-recording-the-weather','curriculum','Grade 2'),
  ('grade-3-mathematics-place-value-up-to-one-thousand','curriculum','Grade 3'),
  ('grade-3-mathematics-multiplication-as-equal-groups','curriculum','Grade 3'),
  ('grade-3-english-language-main-idea-and-supporting-detail','curriculum','Grade 3'),
  ('grade-3-science-and-technology-food-sources-and-good-hygiene','curriculum','Grade 3'),
  ('grade-4-english-language-common-nouns-and-proper-nouns','curriculum','Grade 4'),
  ('grade-4-english-language-a-friendly-letter','curriculum','Grade 4'),
  ('grade-4-mathematics-four-digit-place-value','curriculum','Grade 4'),
  ('grade-4-indigenous-languages-retelling-a-folktale','curriculum','Grade 4'),
  ('grade-5-science-and-technology-the-digestive-system','curriculum','Grade 5'),
  ('grade-5-science-and-technology-preventing-water-pollution','curriculum','Grade 5'),
  ('grade-5-science-and-technology-using-solar-energy','curriculum','Grade 5'),
  ('grade-5-english-language-writing-a-short-summary','curriculum','Grade 5'),
  ('grade-6-mathematics-whole-numbers-to-one-million','curriculum','Grade 6'),
  ('grade-6-mathematics-adding-proper-fractions','curriculum','Grade 6'),
  ('grade-6-mathematics-using-ratios','curriculum','Grade 6'),
  ('grade-6-english-language-a-formal-request-letter','curriculum','Grade 6'),
  ('grade-6-english-language-reporting-real-observations','curriculum','Grade 6'),
  ('grade-6-science-and-technology-planning-balanced-meals','curriculum','Grade 6'),
  ('grade-6-science-and-technology-water-filtration-and-safe-storage','curriculum','Grade 6'),
  ('grade-6-indigenous-languages-proverbs-about-cooperation','curriculum','Grade 6'),
  ('grade-7-science-and-technology-breathing-and-the-diaphragm','curriculum','Grade 7'),
  ('grade-7-science-and-technology-comparing-energy-sources','curriculum','Grade 7'),
  ('grade-7-science-and-technology-reduce-reuse-and-recycle','curriculum','Grade 7'),
  ('grade-7-mathematics-reading-bar-graphs','curriculum','Grade 7'),
  ('grade-7-mathematics-percentages-and-discounts','curriculum','Grade 7'),
  ('grade-7-english-language-persuasive-writing-for-school-improvement','curriculum','Grade 7'),
  ('ecd-a-english-language-hearing-and-naming-everyday-sounds','curriculum','ECD A'),
  ('ecd-a-english-language-saying-hello-and-goodbye','curriculum','ECD A'),
  ('ecd-a-science-and-technology-my-body-and-clean-hands','curriculum','ECD A'),
  ('ecd-a-science-and-technology-seeds-leaves-and-growing-plants','curriculum','ECD A'),
  ('ecd-a-indigenous-languages-listening-to-local-rhymes','curriculum','ECD A'),
  ('grade-1-english-language-recognising-beginning-sounds','curriculum','Grade 1'),
  ('grade-2-english-language-writing-sentences-from-pictures','curriculum','Grade 2'),
  ('grade-4-science-and-technology-caring-for-teeth','curriculum','Grade 4'),
  ('grade-4-science-and-technology-safe-food-handling','curriculum','Grade 4'),
  ('grade-5-mathematics-finding-fractions-of-quantities','curriculum','Grade 5'),
  ('grade-5-indigenous-languages-summarising-a-community-story','curriculum','Grade 5'),
  ('grade-7-indigenous-languages-writing-about-a-traditional-skill','curriculum','Grade 7'),
  ('ecd-a-english-listening-instructions','curriculum','ECD A'),
  ('ecd-b-english-picture-vocabulary','curriculum','ECD B'),
  ('ecd-b-indigenous-greetings','curriculum','ECD B'),
  ('grade-2-science-soil-water','curriculum','Grade 2'),
  ('grade-3-science-emergency-helpers','curriculum','Grade 3'),
  ('grade-4-english-informal-letter','curriculum','Grade 4'),
  ('grade-4-english-collective-nouns','curriculum','Grade 4'),
  ('grade-5-science-water-pollution','curriculum','Grade 5'),
  ('grade-5-science-solar-energy','curriculum','Grade 5'),
  ('grade-6-maths-ratio-scale','curriculum','Grade 6'),
  ('grade-6-maths-data-tables','curriculum','Grade 6'),
  ('grade-6-science-nutrition','curriculum','Grade 6'),
  ('grade-6-english-report-writing','curriculum','Grade 6'),
  ('grade-7-science-respiratory-system','curriculum','Grade 7'),
  ('grade-7-science-waste-resources','curriculum','Grade 7'),
  ('grade-7-indigenous-folktale','curriculum','Grade 7'),
  ('ecd-count-5','starter','ECD A'),
  ('g3-multiply','starter','Grade 3'),
  ('g3-reading','starter','Grade 3'),
  ('g3-heritage','starter','Grade 3'),
  ('g5-fractions','starter','Grade 5'),
  ('g5-science','starter','Grade 5'),
  ('g7-percentage','starter','Grade 7'),
  ('g7-environment','starter','Grade 7'),
  ('f1-algebra','starter','Form 1'),
  ('f2-science','starter','Form 2'),
  ('f3-biology','starter','Form 3'),
  ('f4-probability','starter','Form 4');
alter table public.lesson_catalog enable row level security;
revoke all on public.lesson_catalog from public,anon,authenticated;
grant select on public.lesson_catalog to authenticated;
create policy lesson_catalog_authenticated_read on public.lesson_catalog
for select to authenticated using (true);

create table public.lesson_completions (
  school_id uuid not null references public.schools(id),
  student_user_id uuid not null references public.users(id),
  lesson_id text not null references public.lesson_catalog(lesson_id),
  completed_at timestamptz not null default now(),
  primary key(school_id,student_user_id,lesson_id)
);
create index lesson_completions_recent_idx on public.lesson_completions
  (school_id,student_user_id,completed_at desc);
alter table public.lesson_completions enable row level security;
revoke all on public.lesson_completions from public,anon,authenticated;
grant select on public.lesson_completions to authenticated;
grant insert(school_id,student_user_id,lesson_id)
  on public.lesson_completions to authenticated;
create policy lesson_completions_private_read on public.lesson_completions
for select to authenticated using (
  school_id=(select app_private.current_school_id())
  and app_private.can_view_student(student_user_id)
);
create policy lesson_completions_student_insert on public.lesson_completions
for insert to authenticated with check (
  school_id=(select app_private.current_school_id())
  and student_user_id=(select auth.uid())
  and exists(
    select 1 from public.users actor where actor.id=(select auth.uid())
      and actor.role='student' and actor.school_id=lesson_completions.school_id
      and actor.is_active and actor.deleted_at is null
  )
);
-- No UPDATE/DELETE privileges or policies: completion is monotonic and idempotent.
