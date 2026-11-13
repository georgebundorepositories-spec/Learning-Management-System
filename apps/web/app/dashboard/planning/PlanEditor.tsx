"use client";

import {useActionState,useState} from "react";
import {saveTeacherPlan,type PlanState} from "./actions";

type Unit={id:string;title:string;objective:string;steps:string;assessment:string;resources:string};
export type PlanPair={classId:string;subjectId:string;label:string;unitIds:string[]};
export type SavedPlan={
 id:string;class_id:string;subject_id:string;source_lesson_id:string;
 lesson_title:string;objective:string;teaching_steps:string;resources:string;
 assessment:string;reflection:string;planned_on:string|null;status:string;
};

const initialState:PlanState={status:"idle",message:""};

export default function PlanEditor({pairs,units,existing}:{
 pairs:PlanPair[];units:Unit[];existing?:SavedPlan
}){
 const [feedback,action,pending]=useActionState(saveTeacherPlan,initialState);
 const [pairIndex,setPairIndex]=useState(0);
 const pair=pairs[pairIndex];
 const options=units.filter(unit=>pair?.unitIds.includes(unit.id));
 const [unitId,setUnitId]=useState(existing?.source_lesson_id??options[0]?.id??"");
 const unit=units.find(u=>u.id===unitId)??options[0];
 const [title,setTitle]=useState(existing?.lesson_title??unit?.title??"");
 const [objective,setObjective]=useState(existing?.objective??unit?.objective??"");
 const [steps,setSteps]=useState(existing?.teaching_steps??unit?.steps??"");
 const [assessment,setAssessment]=useState(existing?.assessment??unit?.assessment??"");
 const [resources,setResources]=useState(existing?.resources??unit?.resources??"");
 const [reflection,setReflection]=useState(existing?.reflection??"");
 const [status,setStatus]=useState(existing?.status??"draft");
 const [date,setDate]=useState(existing?.planned_on??"");
 function chooseUnit(nextId:string) {
  setUnitId(nextId);
  const selected=units.find(u=>u.id===nextId);
  if(selected){
   setTitle(selected.title);
   setObjective(selected.objective);
   setSteps(selected.steps.slice(0,6000));
   setAssessment(selected.assessment.slice(0,3000));
   setResources(selected.resources.slice(0,3000));
  }
 }
 function changePair(next:number) {
  setPairIndex(next);
  const nextId=pairs[next]?.unitIds[0]??"";
  chooseUnit(nextId);
 }
 const enabled=!!existing||!!(pair&&options.length);
 return <form action={action} className="resource-form">
  {existing?<input type="hidden" name="plan_id" value={existing.id}/>:<>
    <label>Assigned class and subject
     <select value={pairIndex} onChange={e=>changePair(Number(e.target.value))} disabled={!pairs.length}>
      {pairs.map((p,i)=><option key={p.classId+"-"+p.subjectId} value={i}>{p.label}</option>)}
     </select>
    </label>
    <input type="hidden" name="class_id" value={pair?.classId??""}/>
    <input type="hidden" name="subject_id" value={pair?.subjectId??""}/>
    <label>Curriculum lesson
     <select name="source_lesson_id" value={unitId}
      onChange={e=>chooseUnit(e.target.value)} disabled={!options.length}>
      {options.map(u=><option key={u.id} value={u.id}>{u.title}</option>)}
     </select>
    </label>
   </>}
  <label>Lesson date (optional)
   <input name="planned_on" type="date" value={date} onChange={e=>setDate(e.target.value)}/>
  </label>
  <label>Plan status
   <select name="status" value={status} onChange={e=>setStatus(e.target.value)}>
    <option value="draft">Draft — private</option><option value="ready">Ready for teaching — still private</option>
   </select>
  </label>
  <label className="form-full">Lesson title
   <input name="lesson_title" required minLength={3} maxLength={180}
    value={title} onChange={e=>setTitle(e.target.value)}/>
  </label>
  <label className="form-full">Learning objectives
   <textarea name="objective" required rows={3} minLength={10} maxLength={3000}
    value={objective} onChange={e=>setObjective(e.target.value)}/>
  </label>
  <label className="form-full">Lesson introduction, teacher activities and learner activities
   <textarea name="teaching_steps" required rows={7} minLength={20} maxLength={6000}
    value={steps} onChange={e=>setSteps(e.target.value)}/>
  </label>
  <label className="form-full">Materials and resources
   <textarea name="resources" rows={2} maxLength={3000}
    value={resources} onChange={e=>setResources(e.target.value)}/>
  </label>
  <label className="form-full">Assessment and evaluation
   <textarea name="assessment" rows={3} maxLength={3000}
    value={assessment} onChange={e=>setAssessment(e.target.value)}/>
  </label>
  <label className="form-full">Reflection after teaching (optional)
   <textarea name="reflection" rows={3} maxLength={3000}
    value={reflection} onChange={e=>setReflection(e.target.value)}/>
  </label>
  <div className="form-full">
   <button type="submit" className="outline-button" disabled={pending||!enabled}>
    {pending?"Saving…":existing?"Save plan revisions":"Save new lesson plan"}
   </button>
   {feedback.message?<p role="status" className="notice-panel">{feedback.message}</p>:null}
   <p className="privacy-note">This saves a staff-only planning draft; it does not publish an assignment or grade to learners.</p>
  </div>
 </form>;
}
