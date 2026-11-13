-- FICTIONAL school data ONLY. No real students, phone numbers, email addresses or Auth users.
-- Intentionally separate from supabase/seed.sql; manually apply to DEMO environment only.
-- Re-running is safe: deterministic IDs and ON CONFLICT DO NOTHING preserve prior demo edits.
begin;
insert into public.schools(id,name,emis_number,type,province,district,term_system,currency,locale_default,timezone,is_demo)
values ('10000000-0000-4000-8000-000000000001','DEMO — Mkoba Learning Primary (fictional)','DEMO-ZS-MKOBA-001','primary','Midlands','Gweru',3,'USD','sn','Africa/Harare',true)
on conflict (id) do nothing;
insert into public.schools(id,name,emis_number,type,province,district,term_system,currency,locale_default,timezone,is_demo)
values ('10000000-0000-4000-8000-000000000002','DEMO — Gweru Learning Secondary (fictional)','DEMO-ZS-GWERU-002','secondary','Midlands','Gweru',3,'USD','en','Africa/Harare',true)
on conflict (id) do nothing;
insert into public.academic_years(id,school_id,name,start_date,end_date,is_current)
values ('11000000-0000-4000-8000-000000000001','10000000-0000-4000-8000-000000000001','2026','2026-01-01','2026-12-31',true)
on conflict (id) do nothing;
insert into public.academic_years(id,school_id,name,start_date,end_date,is_current)
values ('11000000-0000-4000-8000-000000000002','10000000-0000-4000-8000-000000000001','2027','2027-01-01','2027-12-31',false)
on conflict (id) do nothing;
insert into public.academic_years(id,school_id,name,start_date,end_date,is_current)
values ('11000000-0000-4000-8000-000000000011','10000000-0000-4000-8000-000000000002','2026','2026-01-01','2026-12-31',true)
on conflict (id) do nothing;
insert into public.academic_years(id,school_id,name,start_date,end_date,is_current)
values ('11000000-0000-4000-8000-000000000012','10000000-0000-4000-8000-000000000002','2027','2027-01-01','2027-12-31',false)
on conflict (id) do nothing;
insert into public.terms(id,school_id,academic_year_id,name,term_number,start_date,end_date)
values ('12000000-0000-4000-8000-000000000001','10000000-0000-4000-8000-000000000001','11000000-0000-4000-8000-000000000001','Term 1',1,'2026-01-13','2026-04-02')
on conflict (id) do nothing;
insert into public.terms(id,school_id,academic_year_id,name,term_number,start_date,end_date)
values ('12000000-0000-4000-8000-000000000002','10000000-0000-4000-8000-000000000001','11000000-0000-4000-8000-000000000001','Term 2',2,'2026-05-05','2026-08-06')
on conflict (id) do nothing;
insert into public.terms(id,school_id,academic_year_id,name,term_number,start_date,end_date)
values ('12000000-0000-4000-8000-000000000003','10000000-0000-4000-8000-000000000001','11000000-0000-4000-8000-000000000001','Term 3',3,'2026-09-08','2026-12-04')
on conflict (id) do nothing;
insert into public.terms(id,school_id,academic_year_id,name,term_number,start_date,end_date)
values ('12000000-0000-4000-8000-000000000004','10000000-0000-4000-8000-000000000001','11000000-0000-4000-8000-000000000002','Term 1',1,'2027-01-12','2027-04-01')
on conflict (id) do nothing;
insert into public.terms(id,school_id,academic_year_id,name,term_number,start_date,end_date)
values ('12000000-0000-4000-8000-000000000005','10000000-0000-4000-8000-000000000001','11000000-0000-4000-8000-000000000002','Term 2',2,'2027-05-04','2027-08-05')
on conflict (id) do nothing;
insert into public.terms(id,school_id,academic_year_id,name,term_number,start_date,end_date)
values ('12000000-0000-4000-8000-000000000006','10000000-0000-4000-8000-000000000001','11000000-0000-4000-8000-000000000002','Term 3',3,'2027-09-07','2027-12-03')
on conflict (id) do nothing;
insert into public.terms(id,school_id,academic_year_id,name,term_number,start_date,end_date)
values ('12000000-0000-4000-8000-000000000007','10000000-0000-4000-8000-000000000002','11000000-0000-4000-8000-000000000011','Term 1',1,'2026-01-13','2026-04-02')
on conflict (id) do nothing;
insert into public.terms(id,school_id,academic_year_id,name,term_number,start_date,end_date)
values ('12000000-0000-4000-8000-000000000008','10000000-0000-4000-8000-000000000002','11000000-0000-4000-8000-000000000011','Term 2',2,'2026-05-05','2026-08-06')
on conflict (id) do nothing;
insert into public.terms(id,school_id,academic_year_id,name,term_number,start_date,end_date)
values ('12000000-0000-4000-8000-000000000009','10000000-0000-4000-8000-000000000002','11000000-0000-4000-8000-000000000011','Term 3',3,'2026-09-08','2026-12-04')
on conflict (id) do nothing;
insert into public.terms(id,school_id,academic_year_id,name,term_number,start_date,end_date)
values ('12000000-0000-4000-8000-000000000010','10000000-0000-4000-8000-000000000002','11000000-0000-4000-8000-000000000012','Term 1',1,'2027-01-12','2027-04-01')
on conflict (id) do nothing;
insert into public.terms(id,school_id,academic_year_id,name,term_number,start_date,end_date)
values ('12000000-0000-4000-8000-000000000011','10000000-0000-4000-8000-000000000002','11000000-0000-4000-8000-000000000012','Term 2',2,'2027-05-04','2027-08-05')
on conflict (id) do nothing;
insert into public.terms(id,school_id,academic_year_id,name,term_number,start_date,end_date)
values ('12000000-0000-4000-8000-000000000012','10000000-0000-4000-8000-000000000002','11000000-0000-4000-8000-000000000012','Term 3',3,'2027-09-07','2027-12-03')
on conflict (id) do nothing;
insert into public.classes(id,school_id,name,grade_level,stream,academic_year_id)
values ('13000000-0000-4000-8000-000000000001','10000000-0000-4000-8000-000000000001','ECD A','ECD A','Sunrise','11000000-0000-4000-8000-000000000001')
on conflict (id) do nothing;
insert into public.classes(id,school_id,name,grade_level,stream,academic_year_id)
values ('13000000-0000-4000-8000-000000000002','10000000-0000-4000-8000-000000000001','Grade 3A','Grade 3','A','11000000-0000-4000-8000-000000000001')
on conflict (id) do nothing;
insert into public.classes(id,school_id,name,grade_level,stream,academic_year_id)
values ('13000000-0000-4000-8000-000000000003','10000000-0000-4000-8000-000000000001','Grade 5A','Grade 5','A','11000000-0000-4000-8000-000000000001')
on conflict (id) do nothing;
insert into public.classes(id,school_id,name,grade_level,stream,academic_year_id)
values ('13000000-0000-4000-8000-000000000004','10000000-0000-4000-8000-000000000001','Grade 7A','Grade 7','A','11000000-0000-4000-8000-000000000001')
on conflict (id) do nothing;
insert into public.classes(id,school_id,name,grade_level,stream,academic_year_id)
values ('13000000-0000-4000-8000-000000000005','10000000-0000-4000-8000-000000000002','Form 1A','Form 1','A','11000000-0000-4000-8000-000000000011')
on conflict (id) do nothing;
insert into public.classes(id,school_id,name,grade_level,stream,academic_year_id)
values ('13000000-0000-4000-8000-000000000006','10000000-0000-4000-8000-000000000002','Form 2A','Form 2','A','11000000-0000-4000-8000-000000000011')
on conflict (id) do nothing;
insert into public.classes(id,school_id,name,grade_level,stream,academic_year_id)
values ('13000000-0000-4000-8000-000000000007','10000000-0000-4000-8000-000000000002','Form 3A','Form 3','A','11000000-0000-4000-8000-000000000011')
on conflict (id) do nothing;
insert into public.classes(id,school_id,name,grade_level,stream,academic_year_id)
values ('13000000-0000-4000-8000-000000000008','10000000-0000-4000-8000-000000000002','Form 4A','Form 4','A','11000000-0000-4000-8000-000000000011')
on conflict (id) do nothing;
insert into public.subjects(id,school_id,name,code,curriculum,grade_levels)
values ('14000000-0000-4000-8000-000000000001','10000000-0000-4000-8000-000000000001','Mathematics','MATH','zimsec',ARRAY['Grade 3','Grade 5','Grade 7'])
on conflict (id) do nothing;
insert into public.subjects(id,school_id,name,code,curriculum,grade_levels)
values ('14000000-0000-4000-8000-000000000002','10000000-0000-4000-8000-000000000001','English Language','ENG','zimsec',ARRAY['Grade 3','Grade 5','Grade 7'])
on conflict (id) do nothing;
insert into public.subjects(id,school_id,name,code,curriculum,grade_levels)
values ('14000000-0000-4000-8000-000000000003','10000000-0000-4000-8000-000000000001','ChiShona','SHO','zimsec',ARRAY['Grade 3','Grade 5','Grade 7'])
on conflict (id) do nothing;
insert into public.subjects(id,school_id,name,code,curriculum,grade_levels)
values ('14000000-0000-4000-8000-000000000004','10000000-0000-4000-8000-000000000001','Science and Technology','SCI','zimsec',ARRAY['Grade 3','Grade 5','Grade 7'])
on conflict (id) do nothing;
insert into public.subjects(id,school_id,name,code,curriculum,grade_levels)
values ('14000000-0000-4000-8000-000000000005','10000000-0000-4000-8000-000000000001','Heritage Studies','HER','zimsec',ARRAY['Grade 3','Grade 5','Grade 7'])
on conflict (id) do nothing;
insert into public.subjects(id,school_id,name,code,curriculum,grade_levels)
values ('14000000-0000-4000-8000-000000000006','10000000-0000-4000-8000-000000000001','Physical Education and Arts','PEA','zimsec',ARRAY['Grade 3','Grade 5','Grade 7'])
on conflict (id) do nothing;
insert into public.subjects(id,school_id,name,code,curriculum,grade_levels)
values ('14000000-0000-4000-8000-000000000007','10000000-0000-4000-8000-000000000002','Mathematics','MATH','zimsec',ARRAY['Form 1','Form 2','Form 3','Form 4'])
on conflict (id) do nothing;
insert into public.subjects(id,school_id,name,code,curriculum,grade_levels)
values ('14000000-0000-4000-8000-000000000008','10000000-0000-4000-8000-000000000002','English Language','ENG','zimsec',ARRAY['Form 1','Form 2','Form 3','Form 4'])
on conflict (id) do nothing;
insert into public.subjects(id,school_id,name,code,curriculum,grade_levels)
values ('14000000-0000-4000-8000-000000000009','10000000-0000-4000-8000-000000000002','Integrated Science','SCI','zimsec',ARRAY['Form 1','Form 2'])
on conflict (id) do nothing;
insert into public.subjects(id,school_id,name,code,curriculum,grade_levels)
values ('14000000-0000-4000-8000-000000000010','10000000-0000-4000-8000-000000000002','Biology','BIO','zimsec',ARRAY['Form 3','Form 4'])
on conflict (id) do nothing;
insert into public.subjects(id,school_id,name,code,curriculum,grade_levels)
values ('14000000-0000-4000-8000-000000000011','10000000-0000-4000-8000-000000000002','Geography','GEO','zimsec',ARRAY['Form 1','Form 2','Form 3','Form 4'])
on conflict (id) do nothing;
insert into public.subjects(id,school_id,name,code,curriculum,grade_levels)
values ('14000000-0000-4000-8000-000000000012','10000000-0000-4000-8000-000000000002','History','HIS','zimsec',ARRAY['Form 1','Form 2','Form 3','Form 4'])
on conflict (id) do nothing;
insert into public.subjects(id,school_id,name,code,curriculum,grade_levels)
values ('14000000-0000-4000-8000-000000000013','10000000-0000-4000-8000-000000000002','Computer Science','CPT','zimsec',ARRAY['Form 1','Form 2','Form 3','Form 4'])
on conflict (id) do nothing;
insert into public.feature_flags(id,school_id,key,value)
values('15000000-0000-4000-8000-000000000001','10000000-0000-4000-8000-000000000001','demo_data','true'::jsonb)
on conflict (id) do nothing;
insert into public.feature_flags(id,school_id,key,value)
values('15000000-0000-4000-8000-000000000002','10000000-0000-4000-8000-000000000001','payments_enabled','false'::jsonb)
on conflict (id) do nothing;
insert into public.feature_flags(id,school_id,key,value)
values('15000000-0000-4000-8000-000000000003','10000000-0000-4000-8000-000000000001','live_messaging','false'::jsonb)
on conflict (id) do nothing;
insert into public.feature_flags(id,school_id,key,value)
values('15000000-0000-4000-8000-000000000011','10000000-0000-4000-8000-000000000002','demo_data','true'::jsonb)
on conflict (id) do nothing;
insert into public.feature_flags(id,school_id,key,value)
values('15000000-0000-4000-8000-000000000012','10000000-0000-4000-8000-000000000002','payments_enabled','false'::jsonb)
on conflict (id) do nothing;
insert into public.feature_flags(id,school_id,key,value)
values('15000000-0000-4000-8000-000000000013','10000000-0000-4000-8000-000000000002','live_messaging','false'::jsonb)
on conflict (id) do nothing;
commit;
