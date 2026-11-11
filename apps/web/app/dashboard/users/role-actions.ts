"use server";
import {revalidatePath} from "next/cache";
import {createClient} from "@/lib/supabase/server";
import {canSchoolAdminChangeRole} from "@/lib/school-role-management";

export type RoleChangeState={status:"idle"|"success"|"error";message:string};
const uuid=/^[a-f0-9]{8}-(?:[a-f0-9]{4}-){3}[a-f0-9]{12}$/i;

export async function changeSchoolMemberRole(
 _previous:RoleChangeState,form:FormData
):Promise<RoleChangeState>{
 try{
  const db=await createClient();
  const {data:{user},error:authError}=await db.auth.getUser();
  if(authError||!user)return {status:"error",message:"Please sign in again."};
  const {data:actor,error:actorError}=await db.from("users")
   .select("school_id,role,is_active,deleted_at")
   .eq("id",user.id).maybeSingle();
  if(actorError||!actor?.is_active||actor.deleted_at||actor.role!=="admin")
   return {status:"error",message:"Only an active school administrator can change a member's role."};

  const memberId=String(form.get("member_id")??"").trim();
  const newRole=String(form.get("new_role")??"").trim();
  const expectedRole=String(form.get("expected_role")??"").trim();
  if(!uuid.test(memberId)||form.get("confirm")!=="yes")
   return {status:"error",message:"Select a member and confirm the role change."};
  const {data:member,error:memberError}=await db.from("users")
   .select("id,role,school_id,is_active,deleted_at")
   .eq("id",memberId).eq("school_id",actor.school_id).is("deleted_at",null).maybeSingle();
  if(memberError||!member)
   return {status:"error",message:"This member is not available in your school."};
  if(member.role!==expectedRole)
   return {status:"error",message:"This member's role changed. Reload the page and try again."};
  if(!canSchoolAdminChangeRole(actor.role,member.role,newRole,member.id===user.id))
   return {status:"error",message:"That role change is not permitted. Administrator and head roles require trusted provisioning."};

  // Supabase RLS and the Phase 21 database trigger independently enforce
  // school isolation, no self-elevation and safe profile relationship changes.
  const {data:updated,error}=await db.from("users").update({role:newRole})
   .eq("id",memberId).eq("school_id",actor.school_id)
   .eq("role",expectedRole).is("deleted_at",null)
   .select("id,role").maybeSingle();
  if(error){
   if(error.message.includes("teaching duties"))
    return {status:"error",message:"Reassign this teacher's classes and subjects before changing their role."};
   return {status:"error",message:"Role not changed. Check school permissions and any existing learner or guardian links."};
  }
  if(!updated)
   return {status:"error",message:"Role not changed. Refresh the list and try again."};
  revalidatePath("/dashboard/users");
  revalidatePath("/dashboard/admin");
  return {status:"success",message:"Role saved. The member will see the new portal when they open or refresh the app. Reassign class or guardian links if needed."};
 }catch{
  return {status:"error",message:"Unable to change the role. Check the school connection and try again."};
 }
}
