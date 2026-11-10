import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import {addLearningResource} from "./actions";
export const dynamic="force-dynamic";
export default async function LearningPage(){
 const supabase=await createClient();const {data:{user}}=await supabase.auth.getUser();if(!user)redirect("/login");
 const {data:actor}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();if(!actor?.is_active)notFound();
 const {data:subjects}=await supabase.from("subjects").select("id,name").eq("school_id",actor.school_id).is("deleted_at",null).order("name").limit(100);
 const {data:resources,error}=await supabase.from("learning_resources").select("id,title,description,resource_url,subject_id,created_at").eq("school_id",actor.school_id).is("deleted_at",null).order("created_at",{ascending:false}).limit(100);
 const canCreate=["admin","head","teacher"].includes(actor.role);
 const {data:subjectAssignments}=actor.role==="teacher"
   ?await supabase.from("class_subject_teacher").select("subject_id")
     .eq("teacher_id",user.id).is("deleted_at",null).limit(200)
   :{data:[] as {subject_id:string}[]};
 const {data:homeroomClasses}=actor.role==="teacher"
   ?await supabase.from("classes").select("id")
     .eq("teacher_id",user.id).eq("school_id",actor.school_id)
     .is("deleted_at",null).limit(1)
   :{data:[] as {id:string}[]};
 const allowedSubjectIds=new Set((subjectAssignments??[]).map(a=>a.subject_id));
 const publishableSubjects=actor.role!=="teacher" || (homeroomClasses??[]).length>0
   ?subjects??[]
   :(subjects??[]).filter(s=>allowedSubjectIds.has(s.id));
 return <main className="dashboard-page"><header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard">← Dashboard</Link></header><div className="dash-wrap"><p className="eyebrow">ACADEMIC RESOURCES</p><h1>Learning materials</h1><p className="subhead">School subject resources and teacher-shared links</p>
 {canCreate?<section className="summary-card"><h2>Add learning material</h2><form action={addLearningResource} className="resource-form"><label>Subject<select name="subject_id" required>{publishableSubjects.map(s=><option value={s.id} key={s.id}>{s.name}</option>)}</select></label><label>Title<input name="title" required minLength={3} maxLength={180}/></label><label>Secure resource link<input name="resource_url" required type="url" placeholder="https://..." maxLength={2048}/></label><label>Description<textarea name="description" maxLength={3000} rows={3}/></label><button type="submit" className="outline-button" disabled={!publishableSubjects.length}>Publish resource</button></form></section>:null}
 {error?<p role="alert" className="notice-panel">Unable to load learning resources.</p>:!resources?.length?<p className="notice-panel">No learning resources have been shared yet.</p>:<div className="summary-cards">{resources.map(r=><article className="summary-card" key={r.id}><span>{subjects?.find(s=>s.id===r.subject_id)?.name??"Subject"}</span><strong>{r.title}</strong><p>{r.description}</p>{r.resource_url?<a href={r.resource_url} target="_blank" rel="noopener noreferrer">Open resource ↗</a>:null}</article>)}</div>}</div></main>;
}
