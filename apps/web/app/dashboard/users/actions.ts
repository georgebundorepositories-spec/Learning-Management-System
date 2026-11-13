"use server";
import {revalidatePath} from "next/cache";
import {createClient} from "@/lib/supabase/server";

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
 const {data,error}=await db.functions.invoke("invite-school-member",{
  body:{role,full_name,email,phone,class_id,linked_student_id}
 });
 if(error||!data?.ok)return {status:"error",message:data?.message??"Unable to send invitation. Check school email settings."};
 revalidatePath("/dashboard/users");
 revalidatePath("/dashboard/admin");
 return {status:"success",message:"Invitation sent. The new member must use their emailed link to choose a password."};
}
