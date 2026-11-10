import Link from "next/link";
import {redirect,notFound} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import {markAttendance} from "./actions";
export const dynamic="force-dynamic";
export default async function AttendancePage(){
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser(); if(!user) redirect("/login");
 const {data:profile}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!profile?.is_active || !["admin","head","teacher"].includes(profile.role)) notFound();
 const {data:classes}=await supabase.from("classes").select("id,name,teacher_id").eq("school_id",profile.school_id).is("deleted_at",null).limit(100);
 const {data:students}=await supabase.from("users").select("id,full_name").eq("school_id",profile.school_id).eq("role","student").eq("is_active",true).limit(200);
 const {data:enrolments}=await supabase.from("student_profiles").select("user_id,class_id").limit(200);
 const {data:marks}=await supabase.from("attendance").select("id,student_user_id,date,status").eq("school_id",profile.school_id).is("deleted_at",null).order("date",{ascending:false}).limit(100);
 const {data:teachingAssignments}=profile.role==="teacher"
   ?await supabase.from("class_subject_teacher").select("class_id")
     .eq("teacher_id",user.id).is("deleted_at",null).limit(200)
   :{data:[] as {class_id:string}[]};
 const assignedIds=new Set((teachingAssignments??[]).map(a=>a.class_id));
 const assigned=(classes??[]).filter(c=>profile.role!=="teacher" || c.teacher_id===user.id || assignedIds.has(c.id));
 return <main className="dashboard-page"><header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard">← Dashboard</Link></header><div className="dash-wrap"><p className="eyebrow">TEACHING OPERATIONS</p><h1>Attendance register</h1><p className="subhead">Mark a learner present, absent, late or excused.</p>
 <div className="summary-cards"><article className="summary-card"><span>Available classes</span><strong>{assigned.length}</strong></article><article className="summary-card"><span>Recent marks</span><strong>{marks?.length??0}</strong></article></div>
 {assigned.length===0?<p className="notice-panel">No assigned classes available. Contact your school administrator.</p>:<div className="summary-cards">{assigned.map(klass=><section key={klass.id} className="summary-card"><h2>{klass.name}</h2>{(enrolments??[]).filter(e=>e.class_id===klass.id).map(e=>{const student=(students??[]).find(s=>s.id===e.user_id);return student?<form key={e.user_id} action={markAttendance} className="attendance-form"><strong>{student.full_name}</strong><input type="hidden" name="student_id" value={student.id}/><input type="hidden" name="class_id" value={klass.id}/><label>Date <input type="date" name="date" required defaultValue={new Date().toISOString().slice(0,10)}/></label><label>Status <select name="status" defaultValue="present"><option value="present">Present</option><option value="absent">Absent</option><option value="late">Late</option><option value="excused">Excused</option></select></label><button type="submit" className="outline-button">Save</button></form>:null;})}</section>)}</div>}
 <h2>Recent attendance</h2><div className="summary-cards">{(marks??[]).map(mark=><article className="summary-card" key={mark.id}><strong>{students?.find(s=>s.id===mark.student_user_id)?.full_name??"Learner"}</strong><span>{mark.date} · {mark.status}</span></article>)}</div></div></main>;
}
