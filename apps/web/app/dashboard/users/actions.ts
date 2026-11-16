"use server";
import {revalidatePath} from "next/cache";
import {createClient} from "@/lib/supabase/server";
import {FunctionsHttpError,FunctionsFetchError,FunctionsRelayError} from "@supabase/supabase-js";
import {invitationFailureMessage} from "@/lib/invitations/invitation-feedback";

export type InviteState={status:"idle"|"error"|"success";message:string};
export async function inviteSchoolMember(_state:InviteState,form:FormData):Promise<InviteState>{
 const db=await createClient();
 const {data:{user}}=await db.auth.getUser();
 if(!user)return {status:"error",message:"Sign in again."};
 const {data:actor}=await db.from("users")
  .select("role,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active||actor.role!=="admin")
  return {status:"error",message:"Only a school administrator can invite a member."};
 const full_name=String(form.get("full_name")??"").trim();
 const email=String(form.get("email")??"").trim().toLowerCase();
 const phone=String(form.get("phone")??"").trim();
 const role=String(form.get("role")??"").trim();
 const class_id=String(form.get("class_id")??"").trim();
 const linked_student_id=String(form.get("linked_student_id")??"").trim();
 if(!["student","parent","teacher"].includes(role)||full_name.length<3||full_name.length>120||
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)||!/^\+2637[1-8]\d{7}$/.test(phone))
  return {status:"error",message:"Enter a name, valid email, unique Zimbabwe mobile number and role."};
 let data:unknown;
 let error:Error|null;
 try{
  const response=await db.functions.invoke("invite-school-member",{
   body:{role,full_name,email,phone,class_id,linked_student_id}
  });
  data=response.data;
  error=response.error;
 }catch{
  return {status:"error",message:"Could not reach the school invitation service. Check the connection and try again."};
 }
 if(error instanceof FunctionsHttpError){
  const response=error.context;
  let body:unknown=null;
  try{body=await response.json()}catch{ /* May not contain JSON. The status is still useful. */ }
  return {status:"error",message:invitationFailureMessage(response.status,body)};
 }
 if(error instanceof FunctionsFetchError||error instanceof FunctionsRelayError){
  return {status:"error",message:"The invitation service could not be reached. Check the connection and try again."};
 }
 if(error)return {status:"error",message:"The invitation service returned an unexpected error. Try again later."};
 if(!data||typeof data!=="object"||!("ok" in data)||data.ok!==true)
  return {status:"error",message:"The invitation service did not confirm delivery. Check the school account before retrying."};
 revalidatePath("/dashboard/users");
 revalidatePath("/dashboard/admin");
 const linkedExisting=("message" in data && data.message===
  "Existing verified login assigned to this school. The member can now sign in using their current password.");
 return {status:"success",message:linkedExisting
  ? "Existing verified login attached to this school with the selected role. The member can now sign in with their current password."
  : "Invitation sent. The new member must use their emailed link to choose a password."};
}
