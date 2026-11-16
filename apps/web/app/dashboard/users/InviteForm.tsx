"use client";
import {useActionState,useState} from "react";
import {inviteSchoolMember,type InviteState} from "./actions";
import {zimbabweMobilePattern} from "@/lib/validation";

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
  <p className="privacy-note">If this person already has a verified login, enter their existing email below. The school will assign the selected role without replacing their password.</p>
  <label>Full name<input name="full_name" required minLength={3} maxLength={120} autoComplete="name"/></label>
  <label>Email for invitation or existing login<input name="email" type="email" required maxLength={254} autoComplete="email"/></label>
  <label>Unique mobile number (+2637...)<input name="phone" type="tel" required pattern={zimbabweMobilePattern} title="Use +263 followed by nine mobile digits, e.g. +263771234567" maxLength={13} inputMode="tel" placeholder="+26377..." autoComplete="tel"/></label>
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
    {pending?"Assigning school membership…":"Invite or assign school member"}
   </button>
   {result.message?<p className="notice-panel" role={result.status==="error"?"alert":"status"}>{result.message}</p>:null}
  </div>
 </form>;
}
