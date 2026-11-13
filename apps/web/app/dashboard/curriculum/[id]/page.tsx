import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import {curriculumUnits,getCurriculumUnit,gradeMatches,subjectMatches} from "@/lib/curriculum/catalog";
import {publishCurriculumAssignment} from "../actions";
import Quiz from "./Quiz";
export const dynamic="force-dynamic";

export async function generateStaticParams(){
 return curriculumUnits.map(unit=>({id:unit.id}));
}

export default async function CurriculumUnitPage({params}:{params:Promise<{id:string}>}){
 const {id}=await params;
 const unit=getCurriculumUnit(id);
 if(!unit)notFound();
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();
 if(!user)redirect("/login");
 const {data:actor}=await supabase.from("users")
   .select("id,role,school_id,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active||!["admin","head","teacher","student","parent"].includes(actor.role))
   notFound();
 const isStaff=["admin","head","teacher"].includes(actor.role);
 const trackProgress=actor.role==="student";
 const completion=trackProgress?await supabase.from("lesson_completions")
  .select("lesson_id").eq("school_id",actor.school_id)
  .eq("student_user_id",user.id).eq("lesson_id",unit.id).maybeSingle()
  :{data:null,error:null};
 const initialCompleted=!!completion.data;

 const [classes,subjects,teaching]=isStaff?await Promise.all([
  supabase.from("classes").select("id,name,grade_level,teacher_id")
   .eq("school_id",actor.school_id).is("deleted_at",null).limit(150),
  supabase.from("subjects").select("id,name").eq("school_id",actor.school_id)
   .is("deleted_at",null).limit(150),
  actor.role==="teacher"?supabase.from("class_subject_teacher")
   .select("class_id,subject_id").eq("teacher_id",user.id)
   .is("deleted_at",null).limit(300)
   :Promise.resolve({data:[] as {class_id:string;subject_id:string}[]})
 ]):[{data:[] as {id:string;name:string;grade_level:string|null;teacher_id:string|null}[]},
     {data:[] as {id:string;name:string}[]},
     {data:[] as {class_id:string;subject_id:string}[]}];
 const allowedPairs=isStaff?(classes.data??[]).flatMap(klass=>{
  if(!gradeMatches(unit.level,klass.grade_level??"",klass.name))return [];
  return (subjects.data??[]).filter(subj=>{
   if(!subjectMatches(unit.subject,subj.name))return false;
   return actor.role!=="teacher"||klass.teacher_id===user.id||
    (teaching.data??[]).some(a=>a.class_id===klass.id&&a.subject_id===subj.id);
  }).map(subj=>({klass,subj}));
 }):[];
 return <main className="dashboard-page">
   <header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard/curriculum">← All lessons</Link></header>
   <div className="dash-wrap">
    <p className="eyebrow">{unit.level.toUpperCase()} · {unit.subject.toUpperCase()} · {unit.durationMinutes} MIN</p>
    <h1>{unit.title}</h1>
    <p className="subhead">{unit.topic}</p>
    <section className="notice-panel"><strong>Learning objective</strong><p>{unit.objective}</p></section>
    <h2>Read and understand</h2>
    {unit.explanation.map((paragraph,index)=><p key={index} style={{fontSize:"1.06rem",lineHeight:1.75}}>{paragraph}</p>)}
    <section className="summary-card"><h2>Worked example</h2><p>{unit.workedExample}</p></section>
    <Quiz lessonId={unit.id} question={unit.question} choices={unit.choices} correctIndex={unit.correctIndex} feedback={unit.feedback} trackProgress={trackProgress} initialCompleted={initialCompleted}/>
    <section className="summary-card">
      <h2>Practice exercises</h2>
      <ol>{unit.exercises.map((item,index)=><li key={index} style={{marginBottom:12}}>{item.question}</li>)}</ol>
      <p className="privacy-note">Write your answers on paper or in your classwork. Your teacher can discuss the solutions.</p>
    </section>
    <section className="summary-card">
      <h2>Class assignment</h2><p>{unit.assignment.instructions}</p>
      <span>Maximum: {unit.assignment.maxScore} marks · {unit.durationMinutes} minute lesson</span>
      {isStaff?<div style={{borderTop:"1px solid var(--border)",paddingTop:16}}>
        <h3>Teacher marking guide</h3>
        <ol>{unit.exercises.map((item,index)=><li key={index}>{item.question} <strong>Answer:</strong> {item.answer}</li>)}</ol>
        <ul>{unit.assignment.rubric.map(row=><li key={row}>{row}</li>)}</ul>
        <p className="privacy-note">Answer keys are for staff review; published class tasks contain only the questions and instructions.</p>
      </div>:null}
    </section>
    {isStaff?<section className="summary-card">
      <h2>Publish to your class</h2>
      {allowedPairs.length?<form action={publishCurriculumAssignment} className="resource-form">
        <input type="hidden" name="unit_id" value={unit.id}/>
        <label>Choose an authorised class and subject
          <select name="class_id" required>{allowedPairs.map(p=><option key={p.klass.id+"-"+p.subj.id} value={p.klass.id}>{p.klass.name} — {p.subj.name}</option>)}</select>
        </label>
        <label>Subject
          <select name="subject_id" required>{[...new Map(allowedPairs.map(p=>[p.subj.id,p.subj])).values()].map(s=><option key={s.id} value={s.id}>{s.name}</option>)}</select>
        </label>
        <p className="privacy-note">The server checks that both selections match the grade and subject. Your school's teacher permissions and database rules are also enforced.</p>
        <button className="outline-button" type="submit">Publish this assignment</button>
      </form>:<p className="notice-panel">No matching school class and subject are available to your account. Ask the school administrator to configure the grade, subject and teacher assignment first.</p>}
    </section>:null}
    <p className="privacy-note">Syllabus alignment: {unit.syllabusSource}; topic {unit.topic}. Practical or media suggestion: {unit.mediaActivity} Original ZimSchool lesson, not a copied commercial textbook page.</p>
   </div>
 </main>;
}
