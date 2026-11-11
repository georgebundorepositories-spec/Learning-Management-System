"use server";

import {revalidatePath} from "next/cache";
import {createClient} from "@/lib/supabase/server";
import {confirmedGuardianAction} from "@/lib/membership-guard";

export type MemberActionState={status:"idle"|"success"|"error";message:string};
const uuid=/^[a-f0-9]{8}-(?:[a-f0-9]{4}-){3}[a-f0-9]{12}$/i;
const get=(data:FormData,key:string)=>String(data.get(key)??"").trim();

export async function updateSchoolMembership(_prev:MemberActionState,form:FormData):Promise<MemberActionState>{
 try{
  const db=await createClient();
  const {data:{user}}=await db.auth.getUser();
  if(!user)return {status:"error",message:"Sign in required."};
  const {data:actor}=await db.from("users").select("school_id,role,is_active,deleted_at")
   .eq("id",user.id).maybeSingle();
  if(!actor?.is_active||actor.deleted_at||actor.role!=="admin")
   return {status:"error",message:"Only the active school administrator can update member links."};
  const schoolId=actor.school_id;
  const op=get(form,"operation");

  if(op==="enrol_learner"){
   const studentId=get(form,"student_id");
   const classId=get(form,"class_id");
   if(!uuid.test(studentId)||classId&&!uuid.test(classId))
    return {status:"error",message:"Invalid learner or class selection."};
   const {data:learner}=await db.from("users").select("id").eq("id",studentId)
    .eq("school_id",schoolId).eq("role","student").eq("is_active",true)
    .is("deleted_at",null).maybeSingle();
   if(!learner)return {status:"error",message:"Select an active learner in this school."};
   if(classId){
    const {data:klass}=await db.from("classes").select("id").eq("id",classId)
     .eq("school_id",schoolId).is("deleted_at",null).maybeSingle();
    if(!klass)return {status:"error",message:"Class not available in your school."};
   }
   const {data:profile,error:readError}=await db.from("student_profiles")
    .select("id").eq("user_id",studentId).is("deleted_at",null).maybeSingle();
   if(readError)return {status:"error",message:"Cannot verify learner enrolment."};
   if(profile){
    const {data,error}=await db.from("student_profiles").update({class_id:classId||null})
     .eq("id",profile.id).select("id").maybeSingle();
    if(error||!data)return {status:"error",message:"Unable to update learner's class."};
   }else{
    const {error}=await db.from("student_profiles")
     .insert({user_id:studentId,class_id:classId||null});
    if(error)return {status:"error",message:"Unable to create learner enrolment."};
   }
   revalidatePath("/dashboard/users");
   revalidatePath("/dashboard/my-learning");
   return {status:"success",message:classId?"Learner assigned to the selected class.":"Learner removed from their previous class."};
  }

  if(op==="assign_homeroom_teacher"){
   const classId=get(form,"class_id");
   const teacherId=get(form,"teacher_id");
   if(!uuid.test(classId)||teacherId&&!uuid.test(teacherId))
    return {status:"error",message:"Invalid class or teacher."};
   const {data:klass}=await db.from("classes").select("id").eq("id",classId)
    .eq("school_id",schoolId).is("deleted_at",null).maybeSingle();
   if(!klass)return {status:"error",message:"Class is outside this school."};
   if(teacherId){
    const {data:teacher}=await db.from("users").select("id").eq("id",teacherId)
     .eq("school_id",schoolId).eq("role","teacher").eq("is_active",true)
     .is("deleted_at",null).maybeSingle();
    if(!teacher)return {status:"error",message:"Choose an active teacher from this school."};
   }
   const {data,error}=await db.from("classes")
    .update({teacher_id:teacherId||null}).eq("id",classId)
    .eq("school_id",schoolId).is("deleted_at",null).select("id").maybeSingle();
   if(error||!data)return {status:"error",message:"Unable to update class teacher."};
   revalidatePath("/dashboard/users");
   revalidatePath("/dashboard/planning");
   return {status:"success",message:"Class teaching assignment updated."};
  }

  if(op==="link_parent"){
   if(!confirmedGuardianAction(form,"link"))
    return {status:"error",message:"Confirm that this guardian relationship and required school consent have been verified."};
   const studentId=get(form,"student_id");
   const parentId=get(form,"parent_id");
   if(!uuid.test(studentId)||!uuid.test(parentId))
    return {status:"error",message:"Choose a learner and parent."};
   const [{data:learner},{data:parent}]=await Promise.all([
    db.from("users").select("id").eq("id",studentId).eq("school_id",schoolId)
     .eq("role","student").eq("is_active",true).is("deleted_at",null).maybeSingle(),
    db.from("users").select("id").eq("id",parentId).eq("school_id",schoolId)
     .eq("role","parent").eq("is_active",true).is("deleted_at",null).maybeSingle()
   ]);
   if(!learner||!parent)return {status:"error",message:"The learner and guardian must both be active in the same school."};
   const {data:existing,error:readError}=await db.from("student_guardian_link")
    .select("id").eq("student_user_id",studentId).eq("guardian_user_id",parentId)
    .is("deleted_at",null).maybeSingle();
   if(readError)return {status:"error",message:"Unable to check existing family links."};
   if(existing)return {status:"success",message:"This parent is already linked to that learner."};
   const {error}=await db.from("student_guardian_link").insert({
    student_user_id:studentId,guardian_user_id:parentId,
    is_primary:false,can_pay_fees:false
   });
   if(error)return {status:"error",message:"Unable to link the accounts. Check for a previously deactivated relationship."};
   revalidatePath("/dashboard/users");
   revalidatePath("/dashboard/my-learning");
   revalidatePath("/dashboard/progress");
   return {status:"success",message:"Guardian link established. This parent can now view the linked learner's authorised records."};
  }

  if(op==="unlink_parent"){
   if(!confirmedGuardianAction(form,"revoke"))
    return {status:"error",message:"Confirm the guardian relationship must be revoked before submitting."};
   const id=get(form,"link_id");
   if(!uuid.test(id))return {status:"error",message:"Invalid guardian link."};
   const {data:link}=await db.from("student_guardian_link")
    .select("id,student_user_id,guardian_user_id").eq("id",id)
    .is("deleted_at",null).maybeSingle();
   if(!link)return {status:"error",message:"Link not found."};
   const [{data:learner},{data:guardian}]=await Promise.all([
    db.from("users").select("id").eq("id",link.student_user_id).eq("school_id",schoolId)
     .eq("role","student").maybeSingle(),
    db.from("users").select("id").eq("id",link.guardian_user_id).eq("school_id",schoolId)
     .eq("role","parent").maybeSingle()
   ]);
   if(!learner||!guardian)return {status:"error",message:"The relationship is not in your school."};
   // DELETE intentionally removes read access immediately; the database permits
   // only same-school administrators to delete an existing relationship.
   const {data,error}=await db.from("student_guardian_link").delete()
    .eq("id",id).select("id").maybeSingle();
   if(error||!data)return {status:"error",message:"Unable to revoke this guardian link."};
   revalidatePath("/dashboard/users");
   revalidatePath("/dashboard/my-learning");
   revalidatePath("/dashboard/progress");
   return {status:"success",message:"Guardian link revoked. The parent no longer has this learner relationship."};
  }
  return {status:"error",message:"Unknown membership action."};
 }catch{
  return {status:"error",message:"Unable to change membership. Verify your connection."};
 }
}
