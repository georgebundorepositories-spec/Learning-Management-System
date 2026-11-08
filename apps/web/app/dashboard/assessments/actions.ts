"use server";
import {revalidatePath} from "next/cache";
import {createClient} from "@/lib/supabase/server";
export async function createAssessment(form:FormData){
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();if(!user)throw new Error("Sign in required");
 const {data:actor}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active || !["admin","head","teacher"].includes(actor.role))throw new Error("Not authorised");
 const class_id=String(form.get("class_id")??"");
 const subject_id=String(form.get("subject_id")??"");
 const title=String(form.get("title")??"").trim();
 const assessment_date=String(form.get("assessment_date")??"");
 const max_score=Number(form.get("max_score"));
 if(title.length<3 || title.length>160 || !/^\d{4}-\d{2}-\d{2}$/.test(assessment_date) || !Number.isFinite(max_score) || max_score<=0 || max_score>10000)throw new Error("Invalid assessment");
 const {data:klass}=await supabase.from("classes").select("id,teacher_id").eq("id",class_id).eq("school_id",actor.school_id).maybeSingle();
 const {data:subject}=await supabase.from("subjects").select("id").eq("id",subject_id).eq("school_id",actor.school_id).maybeSingle();
 if(!klass || !subject)throw new Error("Invalid class or subject");
 if(actor.role==="teacher" && klass.teacher_id!==user.id){
 const {data:assignment}=await supabase.from("class_subject_teacher").select("id").eq("class_id",class_id).eq("teacher_id",user.id).eq("subject_id",subject_id).is("deleted_at",null).limit(1).maybeSingle();
 if(!assignment)throw new Error("Not assigned to this class and subject");
 }
 const {error}=await supabase.from("assessments").insert({school_id:actor.school_id,class_id,subject_id,title,max_score,assessment_date,created_by:user.id});
 if(error)throw new Error("Unable to create assessment");
 revalidatePath("/dashboard/assessments");
}
export async function recordAssessmentScore(form:FormData){
 const supabase=await createClient();const {data:{user}}=await supabase.auth.getUser();if(!user)throw new Error("Sign in required");
 const {data:actor}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active || !["admin","head","teacher"].includes(actor.role))throw new Error("Not authorised");
 const assessment_id=String(form.get("assessment_id")??"");
 const student_user_id=String(form.get("student_user_id")??"");
 const score=Number(form.get("score"));
 if(!Number.isFinite(score) || score<0)throw new Error("Invalid score");
 const {data:assessment}=await supabase.from("assessments").select("id,class_id,max_score").eq("id",assessment_id).eq("school_id",actor.school_id).is("deleted_at",null).maybeSingle();
 if(!assessment || score>assessment.max_score)throw new Error("Score exceeds assessment maximum");
 const {data:enrolled}=await supabase.from("student_profiles").select("id").eq("user_id",student_user_id).eq("class_id",assessment.class_id).is("deleted_at",null).maybeSingle();
 if(!enrolled)throw new Error("Learner not enrolled");
 const {data:existing}=await supabase.from("assessment_results").select("id").eq("assessment_id",assessment_id).eq("student_user_id",student_user_id).maybeSingle();
 const values={assessment_id,student_user_id,score,marked_by:user.id};
 const result=existing?await supabase.from("assessment_results").update(values).eq("id",existing.id):await supabase.from("assessment_results").insert(values);
 if(result.error)throw new Error("Unable to save score");
 revalidatePath("/dashboard/assessments");
}
