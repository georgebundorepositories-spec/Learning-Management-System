"use server";

import {revalidatePath} from "next/cache";
import {createClient} from "@/lib/supabase/server";
import {getCurriculumUnit,gradeMatches,subjectMatches} from "@/lib/curriculum/catalog";

export type PlanState={status:"idle"|"success"|"error";message:string};
const uuid=/^[a-f0-9]{8}-(?:[a-f0-9]{4}-){3}[a-f0-9]{12}$/i;
const str=(data:FormData,name:string)=>String(data.get(name)??"").trim();

/** The server and database independently check the teacher's school and assigned subject.
 * This never publishes a lesson to learners or guardians.
 */
export async function saveTeacherPlan(_previous:PlanState,form:FormData):Promise<PlanState>{
 try{
  const db=await createClient();
  const {data:{user}}=await db.auth.getUser();
  if(!user)return {status:"error",message:"Please sign in again."};
  const {data:actor}=await db.from("users").select("school_id,role,is_active,deleted_at")
   .eq("id",user.id).maybeSingle();
  if(!actor?.is_active||actor.deleted_at||actor.role!=="teacher")
   return {status:"error",message:"Only active teachers can save lesson plans."};

  const id=str(form,"plan_id");
  if(id&&!uuid.test(id))return {status:"error",message:"Invalid lesson plan identifier."};
  let classId="",subjectId="",unitId="";
  if(id){
   const {data:old,error}=await db.from("teacher_lesson_plans")
    .select("class_id,subject_id,source_lesson_id")
    .eq("id",id).eq("school_id",actor.school_id).eq("teacher_id",user.id)
    .is("deleted_at",null).maybeSingle();
   if(error||!old)return {status:"error",message:"This lesson plan is unavailable to your account."};
   classId=old.class_id;
   subjectId=old.subject_id;
   unitId=old.source_lesson_id;
  }else{
   classId=str(form,"class_id");
   subjectId=str(form,"subject_id");
   unitId=str(form,"source_lesson_id");
  }
  const unit=getCurriculumUnit(unitId);
  if(!uuid.test(classId)||!uuid.test(subjectId)||!unit)
   return {status:"error",message:"Choose a valid class, subject and curriculum lesson."};

  const [{data:schoolClass},{data:subject}]=await Promise.all([
   db.from("classes").select("id,name,grade_level,teacher_id")
    .eq("id",classId).eq("school_id",actor.school_id).is("deleted_at",null).maybeSingle(),
   db.from("subjects").select("id,name")
    .eq("id",subjectId).eq("school_id",actor.school_id).is("deleted_at",null).maybeSingle()
  ]);
  if(!schoolClass||!subject||!gradeMatches(unit.level,schoolClass.grade_level??"",schoolClass.name)||
     !subjectMatches(unit.subject,subject.name))
   return {status:"error",message:"The class and subject must match the selected curriculum lesson."};
  if(schoolClass.teacher_id!==user.id){
   const {data:assigned}=await db.from("class_subject_teacher").select("id")
    .eq("class_id",classId).eq("subject_id",subjectId)
    .eq("teacher_id",user.id).is("deleted_at",null).limit(1).maybeSingle();
   if(!assigned)return {status:"error",message:"You must be assigned to teach this class and subject."};
  }

  const lesson_title=str(form,"lesson_title");
  const objective=str(form,"objective");
  const teaching_steps=str(form,"teaching_steps");
  const resources=str(form,"resources");
  const assessment=str(form,"assessment");
  const reflection=str(form,"reflection");
  const status=str(form,"status");
  const date=str(form,"planned_on");
  const validDate=!date||/^\d{4}-\d{2}-\d{2}$/.test(date);
  if(lesson_title.length<3||lesson_title.length>180||objective.length<10||objective.length>3000||
     teaching_steps.length<20||teaching_steps.length>6000||
     resources.length>3000||assessment.length>3000||reflection.length>3000||
     !["draft","ready"].includes(status)||!validDate)
   return {status:"error",message:"Check your title, lesson objective, teaching steps and field lengths."};
  const fields={lesson_title,objective,teaching_steps,resources,assessment,reflection,
   planned_on:date||null,status,updated_at:new Date().toISOString()};
  if(id){
   const {data,error}=await db.from("teacher_lesson_plans").update(fields)
    .eq("id",id).eq("school_id",actor.school_id).eq("teacher_id",user.id)
    .is("deleted_at",null).select("id").maybeSingle();
   if(error||!data)return {status:"error",message:"Unable to revise this plan. Check your class assignment."};
  }else{
   const {error}=await db.from("teacher_lesson_plans").insert({
    school_id:actor.school_id,teacher_id:user.id,class_id:classId,subject_id:subjectId,
    source_lesson_id:unitId,...fields
   });
   if(error)return {status:"error",message:"Unable to save your plan. Check your class assignment."};
  }
  revalidatePath("/dashboard/planning");
  return {status:"success",message:id?"Updated your private lesson plan.":"Saved your private lesson plan. Only authorised school staff can view it."};
 }catch{
  return {status:"error",message:"Unable to save your plan. Please retry while connected."};
 }
}
