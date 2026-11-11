"use client";

import {useActionState,useEffect,useState} from "react";
import {updateSchoolMembership,type MemberActionState} from "./membership-actions";

type Option={id:string;name:string};
type ClassOption=Option&{teacher_id:string|null};
type Enrolment={user_id:string;class_id:string|null};
type Link={id:string;student_user_id:string;guardian_user_id:string};
const base:MemberActionState={status:"idle",message:""};

function RevokeLink({link,students,parents}:{link:Link;students:Option[];parents:Option[]}){
 const [state,action,pending]=useActionState(updateSchoolMembership,base);
 const [confirmed,setConfirmed]=useState(false);
 const student=students.find(s=>s.id===link.student_user_id);
 const parent=parents.find(s=>s.id===link.guardian_user_id);
 useEffect(()=>{if(state.status==="success")setConfirmed(false)},[state.status]);
 if(!student||!parent)return null;
 return <form action={action} className="guardian-revoke-item">
  <input type="hidden" name="operation" value="unlink_parent"/>
  <input type="hidden" name="link_id" value={link.id}/>
  <div className="guardian-revoke-description">
   <strong>{parent.name}</strong>
   <span>Linked learner: {student.name}</span>
  </div>
  <label className="guardian-confirm-check">
   <input type="checkbox" name="revocation_confirmed" value="yes" required
    checked={confirmed} onChange={e=>setConfirmed(e.target.checked)}/>
   <span>Confirm this relationship must end. Revoking removes the guardian&apos;s access to this learner&apos;s private records.</span>
  </label>
  <button type="submit" className="outline-button" disabled={pending||!confirmed}>
   {pending?"Revoking access…":"Revoke access"}
  </button>
  {state.message?<p className="form-full notice-panel" role="status" aria-live="polite">{state.message}</p>:null}
 </form>;
}

export default function MembershipManager({
 classes,students,parents,teachers,enrolments,links
}:{
 classes:ClassOption[];students:Option[];parents:Option[];teachers:Option[];
 enrolments:Enrolment[];links:Link[]
}){
 const [enrolState,enrolAction,enrolPending]=useActionState(updateSchoolMembership,base);
 const [teacherState,teacherAction,teacherPending]=useActionState(updateSchoolMembership,base);
 const [parentState,parentAction,parentPending]=useActionState(updateSchoolMembership,base);

 const [studentId,setStudentId]=useState(students[0]?.id??"");
 const [classId,setClassId]=useState(
  enrolments.find(x=>x.user_id===students[0]?.id)?.class_id??""
 );
 const [selectedClass,setSelectedClass]=useState(classes[0]?.id??"");
 const [teacherId,setTeacherId]=useState(classes[0]?.teacher_id??"");
 const [selectedParent,setSelectedParent]=useState(parents[0]?.id??"");
 const [linkedStudent,setLinkedStudent]=useState(students[0]?.id??"");
 const [verifiedGuardian,setVerifiedGuardian]=useState(false);

 useEffect(()=>{
  if(parentState.status==="success")setVerifiedGuardian(false);
 },[parentState.status]);

 const currentClass=enrolments.find(x=>x.user_id===studentId)?.class_id??"";
 const currentTeacher=classes.find(x=>x.id===selectedClass)?.teacher_id??"";
 const alreadyLinked=links.some(x=>
  x.guardian_user_id===selectedParent&&x.student_user_id===linkedStudent);
 return <div className="membership-management-grid">
  <section className="summary-card">
   <h2>Assign learner to class</h2>
   <p>Choose an active learner. The selected class updates to show their current enrolment before you save.</p>
   <form action={enrolAction} className="resource-form">
    <input type="hidden" name="operation" value="enrol_learner"/>
    <label>Learner
     <select name="student_id" required value={studentId}
      onChange={e=>{
       const next=e.target.value;
       setStudentId(next);
       setClassId(enrolments.find(x=>x.user_id===next)?.class_id??"");
      }}>
      {students.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}
     </select>
    </label>
    <label>Class
     <select name="class_id" value={classId} onChange={e=>setClassId(e.target.value)}>
      <option value="">No class assigned</option>
      {classes.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}
     </select>
    </label>
    <p className="form-full privacy-note">Current: {classes.find(c=>c.id===currentClass)?.name??"Unassigned"}</p>
    <button type="submit" className="outline-button"
     disabled={enrolPending||!students.length||classId===currentClass}>
     {enrolPending?"Saving…":"Save class assignment"}
    </button>
    {enrolState.message?<p className="form-full notice-panel" role="status" aria-live="polite">{enrolState.message}</p>:null}
   </form>
  </section>

  <section className="summary-card">
   <h2>Assign class teacher</h2>
   <p>Select a class to see its assigned homeroom teacher. Only changed assignments can be submitted.</p>
   <form action={teacherAction} className="resource-form">
    <input type="hidden" name="operation" value="assign_homeroom_teacher"/>
    <label>Class
     <select name="class_id" required value={selectedClass}
      onChange={e=>{
       setSelectedClass(e.target.value);
       setTeacherId(classes.find(c=>c.id===e.target.value)?.teacher_id??"");
      }}>
      {classes.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}
     </select>
    </label>
    <label>Teacher
     <select name="teacher_id" value={teacherId} onChange={e=>setTeacherId(e.target.value)}>
      <option value="">No homeroom teacher</option>
      {teachers.map(t=><option key={t.id} value={t.id}>{t.name}</option>)}
     </select>
    </label>
    <p className="form-full privacy-note">Current: {teachers.find(t=>t.id===currentTeacher)?.name??"Unassigned"}</p>
    <button type="submit" className="outline-button"
     disabled={teacherPending||!classes.length||teacherId===currentTeacher}>
     {teacherPending?"Saving…":"Save teacher assignment"}
    </button>
    {teacherState.message?<p className="form-full notice-panel" role="status" aria-live="polite">{teacherState.message}</p>:null}
   </form>
  </section>

  <section className="summary-card">
   <h2>Link verified guardian to learner</h2>
   <p>Linking a guardian immediately permits access to that learner&apos;s private school information. Verify legal authority and the school&apos;s required consent before submission.</p>
   <form action={parentAction} className="resource-form">
    <input type="hidden" name="operation" value="link_parent"/>
    <label>Parent or guardian
     <select name="parent_id" required value={selectedParent} onChange={e=>{
       setSelectedParent(e.target.value);setVerifiedGuardian(false);
      }}>
      {parents.map(p=><option key={p.id} value={p.id}>{p.name}</option>)}
     </select>
    </label>
    <label>Learner
     <select name="student_id" required value={linkedStudent} onChange={e=>{
       setLinkedStudent(e.target.value);setVerifiedGuardian(false);
      }}>
      {students.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}
     </select>
    </label>
    {alreadyLinked?<p className="form-full privacy-note">This guardian is already linked to this learner.</p>:null}
    <label className="guardian-confirm-check form-full">
     <input type="checkbox" name="guardian_verified" value="yes" required
      checked={verifiedGuardian} onChange={e=>setVerifiedGuardian(e.target.checked)}/>
     <span>I have verified this guardian&apos;s identity, relationship and any required school consent.</span>
    </label>
    <button type="submit" className="outline-button"
     disabled={parentPending||!parents.length||!students.length||!verifiedGuardian||alreadyLinked}>
     {parentPending?"Linking…":"Link verified guardian"}
    </button>
    {parentState.message?<p className="form-full notice-panel" role="status" aria-live="polite">{parentState.message}</p>:null}
   </form>

   <h3>Current guardian relationships</h3>
   {links.length?links.map(link=><RevokeLink key={link.id} link={link}
    students={students} parents={parents}/>):
    <p className="privacy-note">No active guardian relationships are available to this school administrator.</p>}
  </section>
 </div>;
}
