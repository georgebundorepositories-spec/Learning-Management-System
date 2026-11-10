import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import {curriculumUnits,gradeMatches,subjectMatches} from "@/lib/curriculum/catalog";

export const dynamic="force-dynamic";

/** Staff-only planning: no learner-facing route or automatic publication. */
export default async function LessonPlanning(){
 const db=await createClient();
 const {data:{user}}=await db.auth.getUser();
 if(!user)redirect("/login");
 const {data:actor}=await db.from("users").select("role,is_active,school_id")
  .eq("id",user.id).maybeSingle();
 if(!actor?.is_active||!["teacher","head","admin"].includes(actor.role))notFound();
 const [classReply,subjectReply,assignmentReply]=await Promise.all([
  db.from("classes").select("id,name,grade_level,teacher_id")
   .eq("school_id",actor.school_id).is("deleted_at",null).limit(150),
  db.from("subjects").select("id,name").eq("school_id",actor.school_id).is("deleted_at",null).limit(150),
  actor.role==="teacher"?db.from("class_subject_teacher").select("class_id,subject_id")
   .eq("teacher_id",user.id).is("deleted_at",null).limit(250):
   Promise.resolve({data:[] as {class_id:string;subject_id:string}[],error:null})
 ]);
 const teacher=actor.role==="teacher";
 const teaching=assignmentReply.data??[];
 const classes=(classReply.data??[]).filter(c=>!teacher||
   c.teacher_id===user.id||teaching.some(t=>t.class_id===c.id));
 const subjects=subjectReply.data??[];
 const matched=curriculumUnits.filter(unit=>classes.some(c=>
   gradeMatches(unit.level,c.grade_level??"",c.name)&&subjects.some(s=>
     subjectMatches(unit.subject,s.name)&&(!teacher||c.teacher_id===user.id||
     teaching.some(t=>t.class_id===c.id&&t.subject_id===s.id)))));
 const problem=!!classReply.error||!!subjectReply.error||!!assignmentReply.error;
 return <main className="dashboard-page">
  <header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard" className="outline-button">← Dashboard</Link></header>
  <div className="dash-wrap">
   <p className="eyebrow">STAFF ONLY · TEACHER PREPARATION</p>
   <h1>Lesson planning</h1>
   <p className="subhead">Use approved syllabus-aligned lesson objectives and demonstrations to prepare your teaching. Publishing graded assignments is a separate authorised action.</p>
   {problem?<p className="notice-panel" role="alert">Unable to verify your teaching assignments.</p>:
    !classes.length?<p className="notice-panel">No assigned classes yet. Ask the administrator to assign your class and subject.</p>:
    <div className="summary-cards">
      <article className="summary-card"><span>Authorised classes</span><strong>{classes.length}</strong></article>
      <article className="summary-card"><span>Matched lesson guides</span><strong>{matched.length}</strong></article>
    </div>}
   {!problem&&<div className="summary-cards">
    {matched.map(unit=><article key={unit.id} className="summary-card">
      <span>{unit.level} · {unit.subject}</span>
      <strong>{unit.title}</strong>
      <p><b>Objective:</b> {unit.objective}</p>
      <p><b>Teaching demonstration:</b> {unit.workedExample}</p>
      <span>Suggested duration: {unit.durationMinutes} minutes</span>
      <p><Link href={`/dashboard/curriculum/${encodeURIComponent(unit.id)}`}>Open full guide and class assignment options</Link></p>
    </article>)}
   </div>}
   <p className="privacy-note">Learners and parents cannot access this staff planning route. Lesson plans are not automatically sent to learners.</p>
  </div>
 </main>;
}
