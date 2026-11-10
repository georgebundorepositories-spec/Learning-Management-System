"use client";

import {useActionState} from "react";
import {updateSchoolMembership,type MemberActionState} from "./membership-actions";

type Option={id:string;name:string};
type ClassOption=Option&{teacher_id:string|null};
type Enrolment={user_id:string;class_id:string|null};
type Link={id:string;student_user_id:string;guardian_user_id:string};
const base:MemberActionState={status:"idle",message:""};

function RevokeLink({link,students,parents}:{link:Link;students:Option[];parents:Option[]}){
 const [state,action,pending]=useActionState(updateSchoolMembership,base);
 const student=students.find(s=>s.id===link.student_user_id);
 const parent=parents.find(s=>s.id===link.guardian_user_id);
 if(!student||!parent)return null;
 return <form action={action} className="resource-form">
  <input type="hidden" name="operation" value="unlink_parent"/>
  <input type="hidden" name="link_id" value={link.id}/>
  <span>{parent.name} — {student.name}</span>
  <button type="submit" className="outline-button" disabled={pending}>
   {pending?"Revoking…":"Revoke access"}
  </button>
  {state.message?<p className="form-full" role="status">{state.message}</p>:null}
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
 return <div>
  <section className="summary-card">
   <h2>Assign learner to class</h2>
   <p>Only active learners in this school can be assigned. Each learner can have one current class.</p>
   <form action={enrolAction} className="resource-form">
    <input type="hidden" name="operation" value="enrol_learner"/>
    <label>Learner<select name="student_id" required>
     {students.map(s=><option key={s.id} value={s.id}>{s.name} — {classes.find(c=>c.id===enrolments.find(x=>x.user_id===s.id)?.class_id)?.name??"No class"}</option>)}
    </select></label>
    <label>Class<select name="class_id">
     <option value="">No class assigned</option>
     {classes.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}
    </select></label>
    <button type="submit" className="outline-button" disabled={enrolPending||!students.length}>{enrolPending?"Saving…":"Save class assignment"}</button>
    {enrolState.message?<p className="form-full notice-panel" role="status">{enrolState.message}</p>:null}
   </form>
  </section>
  <section className="summary-card">
   <h2>Assign class teacher</h2>
   <p>Assign a verified teacher to a homeroom class, unlocking relevant attendance and lesson-planning tools.</p>
   <form action={teacherAction} className="resource-form">
    <input type="hidden" name="operation" value="assign_homeroom_teacher"/>
    <label>Class<select name="class_id" required>
     {classes.map(c=><option key={c.id} value={c.id}>{c.name} — {teachers.find(t=>t.id===c.teacher_id)?.name??"Unassigned"}</option>)}
    </select></label>
    <label>Teacher<select name="teacher_id">
     <option value="">Unassigned</option>
     {teachers.map(t=><option key={t.id} value={t.id}>{t.name}</option>)}
    </select></label>
    <button type="submit" className="outline-button" disabled={teacherPending||!classes.length}>{teacherPending?"Saving…":"Save teacher assignment"}</button>
    {teacherState.message?<p className="form-full notice-panel" role="status">{teacherState.message}</p>:null}
   </form>
  </section>
  <section className="summary-card">
   <h2>Link a parent to a learner</h2>
   <p>Only verified active school members can be linked. The link grants guardian-scoped access to learner records; check the legal guardian's identity before applying it.</p>
   <form action={parentAction} className="resource-form">
    <input type="hidden" name="operation" value="link_parent"/>
    <label>Parent<select name="parent_id" required>
     {parents.map(p=><option key={p.id} value={p.id}>{p.name}</option>)}
    </select></label>
    <label>Learner<select name="student_id" required>
     {students.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}
    </select></label>
    <button type="submit" className="outline-button" disabled={parentPending||!parents.length||!students.length}>
     {parentPending?"Linking…":"Link guardian"}
    </button>
    {parentState.message?<p className="form-full notice-panel" role="status">{parentState.message}</p>:null}
   </form>
   {links.length>0?<><h3>Current guardian links</h3>
    {links.map(link=><RevokeLink key={link.id} link={link} students={students} parents={parents}/>)}
   </>:<p className="privacy-note">No active guardian relationships for the selected school members.</p>}
  </section>
 </div>;
}
