import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import {curriculumUnits,gradeMatches,subjectMatches} from "@/lib/curriculum/catalog";
import PlanEditor,{type PlanPair,type SavedPlan} from "./PlanEditor";

export const dynamic="force-dynamic";

/** All lesson plan reads are checked again by Supabase RLS; this server
 * component also denies direct access to learner/guardian roles.
 */
export default async function LessonPlanning(){
 const db=await createClient();
 const {data:{user}}=await db.auth.getUser();
 if(!user)redirect("/login");
 const {data:actor}=await db.from("users")
  .select("role,is_active,deleted_at,school_id").eq("id",user.id).maybeSingle();
 if(!actor?.is_active||actor.deleted_at||!["teacher","head","admin"].includes(actor.role))notFound();
 const teacher=actor.role==="teacher";
 const [classReply,subjectReply,assignmentReply,plansReply]=await Promise.all([
  db.from("classes").select("id,name,grade_level,teacher_id")
   .eq("school_id",actor.school_id).is("deleted_at",null).limit(150),
  db.from("subjects").select("id,name").eq("school_id",actor.school_id).is("deleted_at",null).limit(150),
  teacher?db.from("class_subject_teacher").select("class_id,subject_id")
   .eq("teacher_id",user.id).is("deleted_at",null).limit(250):
   Promise.resolve({data:[] as {class_id:string;subject_id:string}[],error:null}),
  db.from("teacher_lesson_plans")
   .select("id,teacher_id,class_id,subject_id,source_lesson_id,lesson_title,objective,teaching_steps,resources,assessment,reflection,planned_on,status,updated_at")
   .eq("school_id",actor.school_id).is("deleted_at",null)
   .order("updated_at",{ascending:false}).limit(80)
 ]);
 const classes=classReply.data??[];
 const subjects=subjectReply.data??[];
 const teaching=assignmentReply.data??[];
 const canEdit=teacher&&!classReply.error&&!subjectReply.error&&!assignmentReply.error;
 const authorisedClasses=classes.filter(c=>!teacher||c.teacher_id===user.id||teaching.some(t=>t.class_id===c.id));
 const pairs:PlanPair[]=authorisedClasses.flatMap(c=>subjects.flatMap(s=>{
   if(teacher && c.teacher_id!==user.id&&!teaching.some(t=>t.class_id===c.id&&t.subject_id===s.id))return [];
   const allowed=curriculumUnits.filter(u=>gradeMatches(u.level,c.grade_level??"",c.name)&&subjectMatches(u.subject,s.name));
   return allowed.length?[{classId:c.id,subjectId:s.id,label:`${c.name} · ${s.name}`,unitIds:allowed.map(u=>u.id)}]:[];
 }));
 const unitIds=new Set(pairs.flatMap(p=>p.unitIds));
 const units=curriculumUnits.filter(u=>unitIds.has(u.id)).map(u=>({
  id:u.id,title:u.title,objective:u.objective,
  steps:[...u.explanation,"Worked example: "+u.workedExample,
    u.mediaActivity?"Practical activity: "+u.mediaActivity:""].filter(Boolean).join("\n\n").slice(0,6000),
  assessment:u.exercises.map((e,i)=>`${i+1}. ${e.question}`).join("\n").slice(0,3000),
  resources:""
 }));
 const plans=(plansReply.data??[]) as (SavedPlan&{teacher_id:string;updated_at:string})[];
 const listProblem=!!classReply.error||!!subjectReply.error||!!assignmentReply.error||!!plansReply.error;
 return <main className="dashboard-page">
  <header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard" className="outline-button">← Dashboard</Link></header>
  <div className="dash-wrap">
   <p className="eyebrow">STAFF ONLY · PRIVATE TEACHER PLANS</p>
   <h1>Lesson planning</h1>
   <p className="subhead">Save and revise your own school lesson plans, aligned with assigned classes and the curriculum. Drafts and ready plans are never published automatically to learners.</p>
   {listProblem?<p role="alert" className="notice-panel">Your class assignments or saved plans could not be loaded. Please retry.</p>:<>
    <div className="summary-cards">
     <article className="summary-card"><span>My authorised teaching groups</span><strong>{pairs.length}</strong></article>
     <article className="summary-card"><span>Visible saved plans</span><strong>{plans.length}</strong></article>
    </div>
    {canEdit && (pairs.length? <section className="summary-card">
      <h2>Create a lesson plan</h2>
      <PlanEditor pairs={pairs} units={units}/>
    </section>:<p className="notice-panel">No assigned curriculum class and subject yet. Ask the administrator to assign you before creating plans.</p>)}
    <h2>{teacher?"My saved lesson plans":"School lesson plans"}</h2>
    {!plans.length?<p className="notice-panel">No saved plans are available for this account.</p>:
     <div className="summary-cards">{plans.map(plan=><section className="summary-card" key={plan.id}>
      <span>{plan.planned_on??"Unscheduled"} · {plan.status.toUpperCase()}</span>
      <strong>{plan.lesson_title}</strong>
      <p><b>Objective:</b> {plan.objective}</p>
      <p><b>Teaching activities:</b> {plan.teaching_steps}</p>
      {plan.resources?<p><b>Materials:</b> {plan.resources}</p>:null}
      {plan.assessment?<p><b>Assessment:</b> {plan.assessment}</p>:null}
      {plan.reflection?<p><b>Reflection:</b> {plan.reflection}</p>:null}
      {teacher&&plan.teacher_id===user.id?<details>
       <summary>Revise this lesson plan</summary>
       <PlanEditor pairs={pairs} units={units} existing={plan}/>
      </details>:null}
     </section>)}</div>}
   </>}
   <p className="privacy-note">Teachers may edit only their own plans for currently assigned subjects. Administrators and school heads can read school planning records. Learners and guardians have no access to this database table.</p>
  </div>
 </main>;
}
