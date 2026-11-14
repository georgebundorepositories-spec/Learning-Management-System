"use client";
import {useActionState,useState} from "react";
import {inviteSchoolMember,type InviteState} from "./actions";

type Option={id:string;name:string};
const initial:InviteState={status:"idle",message:""};

export default function InviteForm({classes,learners}:{classes:Option[];learners:Option[]}){
 const [role,setRole]=useState("student");
 const [result,action,pending]=useActionState(inviteSchoolMember,initial);
 return <form action={action} className="resource-form">
  <label>Designation
   <select name="role" value={role} onChange={e=>setRole(e.target.value)}>
    <option value="student">Student / Learner</option>
    <option value="parent">Parent / Guardian</option>
    <option value="teacher">Teacher</option>
   </select>
  </label>
  <label>Full name<input name="full_name" required minLength={3} maxLength={120} autoComplete="name"/></label>
  <label>Email for invitation<input name="email" type="email" required maxLength={254} autoComplete="email"/></label>
  <label>Unique mobile number (+2637...)<input name="phone" type="tel" required pattern="\\+2637[1-8][0-9]{7}" placeholder="+26377..." autoComplete="tel"/></label>
  {role==="student"?<label>Class (optional until enrolled)
   <select name="class_id" defaultValue=""><option value="">Assign class later</option>
    {classes.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}
   </select>
  </label>:null}
  {role==="parent"?<label>Linked learner (optional)
   <select name="linked_student_id" defaultValue=""><option value="">Link a learner later</option>
    {learners.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}
   </select>
  </label>:null}
  <div className="form-full">
   <button className="outline-button" type="submit" disabled={pending}>
    {pending?"Sending invitation…":"Invite school member"}
   </button>
   {result.message?<p className="notice-panel" role="status">{result.message}</p>:null}
  </div>
 </form>;
}
