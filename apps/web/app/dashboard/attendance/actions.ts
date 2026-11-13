"use server";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import {isCalendarDate} from "@/lib/attendance/register";

const allowed = new Set(["present", "absent", "late", "excused"]);

export async function markAttendance(formData: FormData) {
 const supabase = await createClient();
 const {data:{user}}=await supabase.auth.getUser();
 if(!user) throw new Error("Sign in required");
 const {data:profile}=await supabase.from("users")
  .select("school_id,role,is_active,deleted_at").eq("id",user.id).maybeSingle();
 if(!profile?.is_active||profile.deleted_at||
  !["admin","head","teacher"].includes(profile.role)) throw new Error("Not authorised");

 const studentId=String(formData.get("student_id")??"");
 const classId=String(formData.get("class_id")??"");
 const date=String(formData.get("date")??"");
 const status=String(formData.get("status")??"");
 const uuid=/^[0-9a-f-]{36}$/i;
 if(!allowed.has(status)||!isCalendarDate(date)||!uuid.test(studentId)||!uuid.test(classId))
  throw new Error("Invalid attendance data");

 const {data:learner}=await supabase.from("student_profiles")
  .select("user_id,class_id").eq("user_id",studentId).eq("class_id",classId)
  .is("deleted_at",null).maybeSingle();
 const {data:schoolUser}=await supabase.from("users").select("id").eq("id",studentId)
  .eq("school_id",profile.school_id).eq("role","student").eq("is_active",true)
  .is("deleted_at",null).maybeSingle();
 const {data:klass}=await supabase.from("classes").select("id,teacher_id")
  .eq("id",classId).eq("school_id",profile.school_id).is("deleted_at",null).maybeSingle();
 if(!learner||!schoolUser||!klass) throw new Error("Learner not in this school/class");
 if(profile.role==="teacher"&&klass.teacher_id!==user.id){
  const {data:assignment}=await supabase.from("class_subject_teacher").select("id")
   .eq("class_id",classId).eq("teacher_id",user.id).is("deleted_at",null)
   .limit(1).maybeSingle();
  if(!assignment) throw new Error("Teacher not assigned to this class");
 }
 const {data:existing}=await supabase.from("attendance").select("id")
  .eq("school_id",profile.school_id).eq("student_user_id",studentId)
  .eq("class_id",classId).eq("date",date).is("deleted_at",null).maybeSingle();
 const payload={school_id:profile.school_id,class_id:classId,student_user_id:studentId,
  date,status,marked_by:user.id};
 const result=existing
  ?await supabase.from("attendance").update(payload).eq("id",existing.id)
    .eq("school_id",profile.school_id)
  :await supabase.from("attendance").insert(payload);
 if(result.error)throw new Error("Unable to save attendance: "+result.error.message);
 revalidatePath("/dashboard/attendance");
 redirect("/dashboard/attendance?date="+encodeURIComponent(date)+"&saved=1");
}
