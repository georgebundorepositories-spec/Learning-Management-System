import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import {createAssessment,recordAssessmentScore} from "./actions";
export const dynamic="force-dynamic";
export default async function Assessments(){
 const supabase=await createClient();const {data:{user}}=await supabase.auth.getUser();if(!user)redirect("/login");
 const {data:actor}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active)notFound();
 const canMark=["admin","head","teacher"].includes(actor.role);
 const {data:classes}=canMark?await supabase.from("classes").select("id,name,teacher_id").eq("school_id",actor.school_id).is("deleted_at",null).limit(100):{data:[]};
 const {data:subjects}=await supabase.from("subjects").select("id,name").eq("school_id",actor.school_id).is("deleted_at",null).limit(100);
 const {data:assessments,error}=await supabase.from("assessments").select("id,class_id,subject_id,title,max_score,assessment_date").eq("school_id",actor.school_id).is("deleted_at",null).order("assessment_date",{ascending:false}).limit(100);
 const {data:enrolments}=canMark?await supabase.from("student_profiles").select("user_id,class_id").is("deleted_at",null).limit(500):{data:[]};
 const ids=[...new Set((enrolments??[]).map(e=>e.user_id))];
 const {data:students}=ids.length?await supabase.from("users").select("id,full_name").eq("school_id",actor.school_id).in("id",ids).limit(500):{data:[]};
 const {data:results}=await supabase.from("assessment_results").select("id,assessment_id,student_user_id,score").limit(500);
 return <main className="dashboard-page"><header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard">← Dashboard</Link></header><div className="dash-wrap"><p className="eyebrow">ACADEMIC PERFORMANCE</p><h1>Assessments & results</h1><p className="subhead">Secure school assessment records</p>
 {canMark?<section className="summary-card"><h2>Create assessment</h2><form action={createAssessment} className="resource-form"><label>Class<select name="class_id" required>{(classes??[]).map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></label><label>Subject<select name="subject_id" required>{(subjects??[]).map(s=><option key={s.id} value={s.id}>{s.name}</option>)}</select></label><label>Assessment title<input name="title" required minLength={3} maxLength={160}/></label><label>Date<input type="date" name="assessment_date" required/></label><label>Maximum score<input type="number" name="max_score" min="0.01" max="10000" step="0.01" required/></label><button type="submit" className="outline-button" disabled={!classes?.length || !subjects?.length}>Create assessment</button></form></section>:null}
 {error?<p className="notice-panel" role="alert">Unable to load assessments.</p>:!assessments?.length?<p className="notice-panel">No assessments available.</p>:<div className="summary-cards">{assessments.map(a=><section className="summary-card" key={a.id}><span>{subjects?.find(s=>s.id===a.subject_id)?.name??"Subject"} · {a.assessment_date}</span><strong>{a.title}</strong><span>Maximum: {a.max_score}</span>{canMark?(enrolments??[]).filter(e=>e.class_id===a.class_id).map(e=>{const student=students?.find(s=>s.id===e.user_id);const score=results?.find(r=>r.assessment_id===a.id && r.student_user_id===e.user_id);return student?<form className="assessment-score-form" action={recordAssessmentScore} key={student.id}><input type="hidden" name="assessment_id" value={a.id}/><input type="hidden" name="student_user_id" value={student.id}/><label>{student.full_name}<input type="number" name="score" min="0" max={a.max_score} step="0.01" defaultValue={score?.score??""} required/></label><button type="submit" className="outline-button">Save score</button></form>:null;}): (results??[]).filter(r=>r.assessment_id===a.id).map(r=><span key={r.id}>Score: {r.score}/{a.max_score}</span>)}</section>)}</div>}</div></main>;
}
