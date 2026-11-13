"use server";

import { revalidatePath } from "next/cache";
import { createClient } from "@/lib/supabase/server";

const uuid = /^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i;
const decimal = /^(?:0|[1-9]\d{0,4})(?:\.\d{1,2})?$/;

export async function createAssessment(form: FormData) {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error("Sign in required");

  const { data: actor } = await supabase.from("users")
    .select("school_id,role,is_active").eq("id",user.id).maybeSingle();
  if (!actor?.is_active || !["admin","head","teacher"].includes(actor.role))
    throw new Error("Not authorised to create assessments");

  const class_id = String(form.get("class_id") ?? "");
  const subject_id = String(form.get("subject_id") ?? "");
  const title = String(form.get("title") ?? "").trim();
  const assessment_date = String(form.get("assessment_date") ?? "");
  const maxText = String(form.get("max_score") ?? "");

  if (!uuid.test(class_id) || !uuid.test(subject_id)
      || title.length < 3 || title.length > 160
      || !/^\d{4}-\d{2}-\d{2}$/.test(assessment_date)
      || !decimal.test(maxText) || Number(maxText) <= 0
      || Number(maxText) > 10000)
    throw new Error("Invalid assessment details");

  const { data: klass } = await supabase.from("classes")
    .select("id,teacher_id").eq("id",class_id)
    .eq("school_id",actor.school_id).is("deleted_at",null).maybeSingle();
  const { data: subject } = await supabase.from("subjects")
    .select("id").eq("id",subject_id).eq("school_id",actor.school_id)
    .is("deleted_at",null).maybeSingle();
  if (!klass || !subject) throw new Error("Class or subject is unavailable");

  // The database independently enforces the same subject-specific rule for
  // all callers, including direct Data API requests outside Next.js.
  if (actor.role === "teacher" && klass.teacher_id !== user.id) {
    const { data: assigned } = await supabase.from("class_subject_teacher")
      .select("id").eq("class_id",class_id).eq("subject_id",subject_id)
      .eq("teacher_id",user.id).is("deleted_at",null).limit(1).maybeSingle();
    if (!assigned) throw new Error("Not assigned to this subject and class");
  }

  const { error } = await supabase.from("assessments").insert({
    school_id:actor.school_id,class_id,subject_id,title,
    max_score:Number(maxText),assessment_date,created_by:user.id
  });
  if (error) throw new Error("Unable to create assessment");
  revalidatePath("/dashboard/assessments");
}

export async function recordAssessmentScore(form: FormData) {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error("Sign in required");
  const { data: actor } = await supabase.from("users")
    .select("school_id,role,is_active").eq("id",user.id).maybeSingle();
  if (!actor?.is_active || !["admin","head","teacher"].includes(actor.role))
    throw new Error("Not authorised to record grades");

  const assessment_id = String(form.get("assessment_id") ?? "");
  const student_user_id = String(form.get("student_user_id") ?? "");
  const scoreText = String(form.get("score") ?? "").trim();
  if (!uuid.test(assessment_id) || !uuid.test(student_user_id)
      || !decimal.test(scoreText))
    throw new Error("Invalid score or learner");
  const score = Number(scoreText);

  const { data: assessment } = await supabase.from("assessments")
    .select("id,class_id,max_score").eq("id",assessment_id)
    .eq("school_id",actor.school_id).is("deleted_at",null).maybeSingle();
  if (!assessment || score > Number(assessment.max_score))
    throw new Error("Score exceeds the maximum or assessment is unavailable");

  const { data: enrolled } = await supabase.from("student_profiles")
    .select("id").eq("user_id",student_user_id)
    .eq("class_id",assessment.class_id).is("deleted_at",null).maybeSingle();
  const { data: learner } = await supabase.from("users")
    .select("id").eq("id",student_user_id)
    .eq("school_id",actor.school_id).eq("role","student")
    .eq("is_active",true).maybeSingle();
  if (!enrolled || !learner) throw new Error("Learner is not enrolled");

  const { data: existing } = await supabase.from("assessment_results")
    .select("id").eq("assessment_id",assessment_id)
    .eq("student_user_id",student_user_id).maybeSingle();

  if (existing) {
    // Column privileges forbid changes to student_user_id or assessment_id.
    const { data, error } = await supabase.from("assessment_results")
      .update({score,marked_by:user.id,updated_at:new Date().toISOString()})
      .eq("id",existing.id).eq("student_user_id",student_user_id)
      .select("id").maybeSingle();
    if (error || !data) throw new Error("Unable to update learner's score");
  } else {
    const { error } = await supabase.from("assessment_results")
      .insert({assessment_id,student_user_id,score,marked_by:user.id});
    if (error) throw new Error("Unable to save learner's score");
  }
  revalidatePath("/dashboard/assessments");
}
