-- Phase 8 index audit: covering indexes for live foreign keys.
create index if not exists assessment_results_marked_by_idx on public.assessment_results(marked_by);
create index if not exists assessments_class_fk_idx on public.assessments(class_id);
create index if not exists assessments_created_by_idx on public.assessments(created_by);
create index if not exists assessments_subject_fk_idx on public.assessments(subject_id);
create index if not exists attendance_marked_by_idx on public.attendance(marked_by);
create index if not exists fee_invoices_created_by_idx on public.fee_invoices(created_by);
create index if not exists fee_invoices_student_fk_idx on public.fee_invoices(student_user_id);
create index if not exists learning_resources_created_by_idx on public.learning_resources(created_by);
create index if not exists learning_resources_subject_fk_idx on public.learning_resources(subject_id);
create index if not exists school_notices_published_by_idx on public.school_notices(published_by);