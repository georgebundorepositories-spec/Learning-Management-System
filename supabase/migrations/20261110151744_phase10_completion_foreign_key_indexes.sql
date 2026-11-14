-- Cover foreign-key lookups for the school-scoped, append-only lesson ledger.
create index if not exists lesson_completions_student_fk_idx
 on public.lesson_completions(student_user_id,school_id);
create index if not exists lesson_completions_lesson_fk_idx
 on public.lesson_completions(lesson_id);