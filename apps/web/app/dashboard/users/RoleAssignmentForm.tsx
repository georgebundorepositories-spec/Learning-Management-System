"use client";
import {useActionState,useState} from "react";
import {useRouter} from "next/navigation";
import {useEffect} from "react";
import {assignableSchoolRoles,schoolRoleLabel} from "@/lib/school-role-management";
import {changeSchoolMemberRole,type RoleChangeState} from "./role-actions";

type Member={id:string;full_name:string;role:string;is_active:boolean};
const initial:RoleChangeState={status:"idle",message:""};

export default function RoleAssignmentForm({members}:{members:Member[]}){
 const router=useRouter();
 const [memberId,setMemberId]=useState("");
 const [newRole,setNewRole]=useState("");
 const [result,action,pending]=useActionState(changeSchoolMemberRole,initial);
 const choices=members.filter(item=>!["admin","head"].includes(item.role));
 const selected=choices.find(item=>item.id===memberId);
 useEffect(()=>{if(result.status==="success")router.refresh()},[result.status,router]);
 return <section className="summary-card" aria-labelledby="role-assignment-heading">
  <h2 id="role-assignment-heading">Assign a member's role</h2>
  <p>Choose a person and the portal they should use. This changes their school role in Supabase, not their login credentials.</p>
  {!choices.length?<p className="notice-panel">No eligible school members yet. Invite a member first.</p>:
   <form action={action} className="resource-form">
    <label>School member
     <select name="member_id" required value={memberId}
      onChange={e=>{setMemberId(e.target.value);setNewRole("")}}>
      <option value="">Select a person</option>
      {choices.map(member=><option key={member.id} value={member.id}>
       {member.full_name} — {schoolRoleLabel(member.role)}{member.is_active?"":" (inactive)"}
      </option>)}
     </select>
    </label>
    <label>New role
     <select name="new_role" required value={newRole} onChange={e=>setNewRole(e.target.value)}>
      <option value="">Choose role</option>
      {assignableSchoolRoles.map(role=><option key={role.value} value={role.value}>
       {role.label}
      </option>)}
     </select>
    </label>
    <input type="hidden" name="expected_role" value={selected?.role??""}/>
    <label className="form-full">
     <span>Confirm change</span>
     <span className="school-role-confirm">
      <input type="checkbox" name="confirm" value="yes" required/>
      I have checked this person's designation and understand existing class or guardian links may need reassigning.
     </span>
    </label>
    <div className="form-full">
     <button type="submit" className="outline-button"
      disabled={pending||!selected||!newRole||newRole===selected.role}>
      {pending?"Saving role…":"Save member role"}
     </button>
     {result.message?<p role={result.status==="error"?"alert":"status"}
      className="notice-panel">{result.message}</p>:null}
    </div>
   </form>}
  <p className="privacy-note">Only the project owner or trusted provisioning can assign administrator or headteacher access. This page cannot give anyone those privileges. Returning a former learner or guardian to their previous role does not automatically restore old links.</p>
 </section>;
}
