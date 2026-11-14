-- Regression: private teacher lesson plans are not learner coursework.
begin;
create extension if not exists pgtap with schema extensions;
set local search_path=public, extensions, pg_catalog;
select plan(16);
select ok(to_regclass('public.teacher_lesson_plans') is not null,'teacher lesson plan table exists');
select ok((select relrowsecurity from pg_class where oid='public.teacher_lesson_plans'::regclass),'RLS is enabled');
select ok(not has_table_privilege('anon','public.teacher_lesson_plans','SELECT'),'anon cannot read private plans');
select ok(has_table_privilege('authenticated','public.teacher_lesson_plans','SELECT'),'authorised staff can read under RLS');
select ok(not has_table_privilege('authenticated','public.teacher_lesson_plans','DELETE'),'no direct deletion');
select ok(not has_column_privilege('authenticated','public.teacher_lesson_plans','teacher_id','UPDATE'),'cannot change teacher ownership');
select ok(not has_column_privilege('authenticated','public.teacher_lesson_plans','school_id','UPDATE'),'cannot move plans between schools');
select ok(not has_column_privilege('authenticated','public.teacher_lesson_plans','class_id','UPDATE'),'cannot move plans to another class');
select ok(not has_column_privilege('authenticated','public.teacher_lesson_plans','subject_id','UPDATE'),'cannot change teaching subject');
select ok(exists(select 1 from pg_policies where schemaname='public' and tablename='teacher_lesson_plans'
 and policyname='teacher_lesson_plans_staff_read' and cmd='SELECT'
 and qual like '%teacher_id%' and qual like '%is_school_admin%'),'read access is staff-scoped');
select ok(exists(select 1 from pg_policies where schemaname='public' and tablename='teacher_lesson_plans'
 and policyname='teacher_lesson_plans_teacher_insert' and cmd='INSERT'
 and with_check like '%can_manage_assessment%' and with_check like '%teacher%'),'insert requires assigned teacher');
select ok(exists(select 1 from pg_policies where schemaname='public' and tablename='teacher_lesson_plans'
 and policyname='teacher_lesson_plans_teacher_update' and cmd='UPDATE'
 and qual like '%can_manage_assessment%' and with_check like '%can_manage_assessment%'),'updates require both owner and assignment');
select ok(exists(select 1 from pg_constraint where conrelid='public.teacher_lesson_plans'::regclass
 and contype='f' and pg_get_constraintdef(oid) like '%lesson_catalog%'),'source unit is a real catalogue lesson');
select ok(has_column_privilege('authenticated','public.teacher_lesson_plans','teaching_steps','UPDATE'),'teacher can revise a plan');
select ok(exists(select 1 from pg_policies where schemaname='public'
 and tablename='teacher_lesson_plans' and policyname='teacher_lesson_plans_staff_read'
 and qual like '%actor.role%' and qual like '%teacher%'),
 'a former teacher cannot retain plan visibility after role change');
select ok((select pg_get_functiondef(p.oid)
 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
 where n.nspname='app_private' and p.proname='is_guardian_of')
 like '%g.role%' and (select pg_get_functiondef(p.oid)
 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
 where n.nspname='app_private' and p.proname='is_guardian_of')
 like '%parent%',
 'guardian relationship helper requires current parent role');
select * from finish();
rollback;
