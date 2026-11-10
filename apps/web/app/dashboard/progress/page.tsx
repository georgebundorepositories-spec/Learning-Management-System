import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import {progressSnapshot} from "@/lib/progress/summary";

export const dynamic="force-dynamic";
const LIMIT=500;

/** Only the signed-in person's own/linked or server-authorised class roster.
 * All underlying reads also retain the database's RLS authorisation.
 */
export default async function LearningProgressPage(){
 const db=await createClient();
 const {data:{user}}=await db.auth.getUser();
 if(!user)redirect("/login");
 const {data:actor}=await db.from("users").select("school_id,role,is_active")
  .eq("id",user.id).maybeSingle();
 if(!actor?.is_active||!["student","parent","teacher","admin","head"].includes(actor.role))
  notFound();

 const isStaff=["teacher","admin","head"].includes(actor.role);
 const school=actor.school_id;
 let allowedStudents:string[]=[];
 let roster:{user_id:string;class_id:string}[]=[];
 let classNames=new Map<string,string>();
 let scopeError=false;

 if(isStaff){
  const classes=await db.from("classes")
    .select("id,name,teacher_id").eq("school_id",school).is("deleted_at",null)
    .limit(150);
  const assignments=actor.role==="teacher"
   ?await db.from("class_subject_teacher").select("class_id")
     .eq("teacher_id",user.id).is("deleted_at",null).limit(LIMIT)
   :{data:[] as {class_id:string}[],error:null};
  scopeError=!!classes.error||!!assignments.error;
  const teachable=new Set((assignments.data??[]).map(a=>a.class_id));
  const allowedClasses=(classes.data??[]).filter(c=>actor.role!=="teacher"||
   c.teacher_id===user.id||teachable.has(c.id));
  classNames=new Map(allowedClasses.map(c=>[c.id,c.name]));
  const classIds=allowedClasses.map(c=>c.id);
  if(classIds.length && !scopeError){
   const reply=await db.from("student_profiles")
     .select("user_id,class_id").in("class_id",classIds)
     .is("deleted_at",null).limit(LIMIT);
   scopeError=!!reply.error;
   roster=reply.data??[];
  }
  allowedStudents=[...new Set(roster.map(s=>s.user_id))];
 }else{
  if(actor.role==="student")allowedStudents=[user.id];
  else{
   const linked=await db.from("student_guardian_link")
    .select("student_user_id").eq("guardian_user_id",user.id)
    .is("deleted_at",null).limit(40);
   scopeError=!!linked.error;
   allowedStudents=[...new Set((linked.data??[]).map(x=>x.student_user_id))];
  }
  if(allowedStudents.length && !scopeError){
   const p=await db.from("student_profiles")
    .select("user_id,class_id").in("user_id",allowedStudents)
    .is("deleted_at",null).limit(40);
   scopeError=!!p.error;
   roster=p.data??[];
   // A guardian never receives students other than those explicitly linked.
   roster=roster.filter(x=>allowedStudents.includes(x.user_id));
  }
 }
 // All downstream queries are limited to the roster previously allowed
 // by the user's role, school and database RLS.
 const learnerIds=[...new Set(roster.map(x=>x.user_id))];
 const classIds=[...new Set(roster.map(x=>x.class_id))];
 const empty={data:[],error:null};
 const [names,schoolAssignments,submissions,assessments,attendance]=
  learnerIds.length && classIds.length && !scopeError?await Promise.all([
   db.from("users").select("id,full_name").eq("school_id",school)
     .eq("role","student").in("id",learnerIds).limit(LIMIT),
   db.from("school_assignments").select("id,class_id,max_score")
     .eq("school_id",school).in("class_id",classIds)
     .is("deleted_at",null).order("created_at",{ascending:false}).limit(LIMIT),
   db.from("assignment_submissions")
     .select("id,assignment_id,student_user_id").eq("school_id",school)
     .in("student_user_id",learnerIds).order("submitted_at",{ascending:false}).limit(LIMIT),
   db.from("assessments").select("id,class_id,max_score")
     .eq("school_id",school).in("class_id",classIds)
     .is("deleted_at",null).order("assessment_date",{ascending:false}).limit(LIMIT),
   db.from("attendance").select("student_user_id,status")
     .eq("school_id",school).in("student_user_id",learnerIds)
     .is("deleted_at",null).order("date",{ascending:false}).limit(LIMIT),
  ]):[empty,empty,empty,empty,empty];

 const submissionIds=(submissions.data??[]).map(s=>s.id);
 const assessmentIds=(assessments.data??[]).map(a=>a.id);
 const [reviews,marks]=await Promise.all([
  submissionIds.length && !scopeError
   ?db.from("assignment_reviews").select("submission_id,score")
    .eq("school_id",school).in("submission_id",submissionIds).limit(LIMIT)
   :Promise.resolve(empty),
  assessmentIds.length && !scopeError
   ?db.from("assessment_results").select("assessment_id,student_user_id,score")
    .in("assessment_id",assessmentIds).in("student_user_id",learnerIds).limit(LIMIT)
   :Promise.resolve(empty)
 ]);
 const error=scopeError||[names,schoolAssignments,submissions,assessments,
  attendance,reviews,marks].some(x=>!!x.error);
 const nameById=new Map((names.data??[]).map(x=>[x.id,x.full_name]));
 const rows=error?[]:roster.map(profile=>({
  name:nameById.get(profile.user_id)??"Learner",
  grade:classNames.get(profile.class_id)??"Enrolled class",
  summary:progressSnapshot(profile.user_id,profile.class_id,
   schoolAssignments.data??[],submissions.data??[],reviews.data??[],
   assessments.data??[],marks.data??[],attendance.data??[])
 }));
 const formatPercent=(n:number|null)=>n===null?"Not yet marked":`${n}%`;
 return <main className="dashboard-page">
  <header className="topbar">
   <div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div>
   <Link href="/dashboard" className="outline-button">← Dashboard</Link>
  </header>
  <div className="dash-wrap">
   <p className="eyebrow">ACADEMIC LEARNING · PRIVATE PROGRESS</p>
   <h1>{isStaff?"Class learning progress":actor.role==="parent"?"My children's progress":"My progress"}</h1>
   <p className="subhead">Coursework, recorded assessment marks and attendance from your authorised school records.</p>
   {error?<p className="notice-panel" role="alert">Your school records could not be loaded securely. Try signing in again or contact a school administrator.</p>
   :rows.length===0?<p className="notice-panel">No enrolled learner records are currently visible to your account.</p>
   :<div className="summary-cards">
    {rows.map(({name,grade,summary:s})=><section className="summary-card" key={s.learnerId}>
     <p className="eyebrow">{grade}</p><h2>{name}</h2>
     <div className="summary-cards" style={{gap:12}}>
      <article className="summary-card"><span>Coursework submitted</span><strong>{s.submitted} / {s.assigned}</strong><small>{s.pending} awaiting submission · {s.reviewed} reviewed</small></article>
      <article className="summary-card"><span>Reviewed coursework</span><strong>{formatPercent(s.courseworkPercent)}</strong><small>Only marked submissions count</small></article>
      <article className="summary-card"><span>Formal assessment marks</span><strong>{formatPercent(s.assessmentPercent)}</strong><small>{s.markedAssessments} / {s.assessmentCount} assessments marked</small></article>
      <article className="summary-card"><span>Attendance entries</span><strong>{s.attendancePresent} / {s.attendanceRecorded}</strong><small>Present / recorded, recent entries only</small></article>
     </div>
    </section>)}
   </div>}
   <div className="summary-card">
    <h2>Continue learning</h2>
    <p><Link href="/dashboard/curriculum">Browse all 63 curriculum units</Link> · <Link href="/dashboard/coursework">Class assignments and feedback</Link> · <Link href="/dashboard/assessments">Assessment register</Link></p>
   </div>
   <p className="privacy-note">
    These are read-only snapshots of accessible records (at most {LIMIT} recent items per source), not complete term reports.
    Missing marks are not treated as zero. Coursework and formal assessments are shown separately;
    offline Android practice completions are not synchronised to these figures.
    Access is checked against school membership and the database's row-level security rules.
   </p>
  </div>
 </main>;
}
