"use server";

import { revalidatePath } from "next/cache";
import { createClient } from "@/lib/supabase/server";

const uuid = /^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i;
const schoolPath = "/dashboard/coursework";

/** Creates a class assignment as staff. Postgres RLS is the final authority. */
export async function createCoursework(form: FormData) {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error("Sign in required");
  const { data: actor } = await supabase.from("users")
    .select("role,school_id,is_active").eq("id", user.id).maybeSingle();
  if (!actor?.is_active || !["admin", "head", "teacher"].includes(actor.role))
    throw new Error("Not authorised to publish assignments");

  const class_id = String(form.get("class_id") ?? "");
  const subject_id = String(form.get("subject_id") ?? "");
  const title = String(form.get("title") ?? "").trim();
  const instructions = String(form.get("instructions") ?? "").trim();
  const due_on = String(form.get("due_on") ?? "").trim();
  const max_score = String(form.get("max_score") ?? "100").trim();
  if (!uuid.test(class_id) || !uuid.test(subject_id)
    || title.length < 3 || title.length > 180
    || instructions.length < 3 || instructions.length > 5000
    || (due_on !== "" && !/^\d{4}-\d{2}-\d{2}$/.test(due_on))
    || !/^(?:0|[1-9]\d{0,4})(?:\.\d{1,2})?$/.test(max_score)
    || Number(max_score) <= 0 || Number(max_score) > 10000)
    throw new Error("Invalid assignment details");

  const { data: klass } = await supabase.from("classes").select("id,teacher_id")
    .eq("id", class_id).eq("school_id", actor.school_id)
    .is("deleted_at", null).maybeSingle();
  const { data: subject } = await supabase.from("subjects").select("id")
    .eq("id", subject_id).eq("school_id", actor.school_id)
    .is("deleted_at", null).maybeSingle();
  if (!klass || !subject) throw new Error("Class or subject is unavailable");

  if (actor.role === "teacher" && klass.teacher_id !== user.id) {
    const { data: assignment } = await supabase.from("class_subject_teacher")
      .select("id").eq("class_id", class_id).eq("subject_id", subject_id)
      .eq("teacher_id", user.id).is("deleted_at", null)
      .limit(1).maybeSingle();
    if (!assignment) throw new Error("Not assigned to this class and subject");
  }

  const { error } = await supabase.from("school_assignments").insert({
    school_id: actor.school_id, class_id, subject_id,
    title, instructions, due_on: due_on || null,
    max_score: Number(max_score), created_by: user.id
  });
  if (error) throw new Error("Unable to publish assignment");
  revalidatePath(schoolPath);
}

/** Students can create or revise their own answers; parents cannot submit. */
export async function submitCoursework(form: FormData) {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error("Sign in required");
  const { data: actor } = await supabase.from("users")
    .select("role,school_id,is_active").eq("id", user.id).maybeSingle();
  if (!actor?.is_active || actor.role !== "student")
    throw new Error("Only the learner can submit an answer");

  const assignment_id = String(form.get("assignment_id") ?? "");
  const text_answer = String(form.get("text_answer") ?? "").trim();
  if (!uuid.test(assignment_id) || text_answer.length < 1 || text_answer.length > 5000)
    throw new Error("Answer must contain 1–5,000 characters");

  const { data: assignment } = await supabase.from("school_assignments")
    .select("id,class_id").eq("id", assignment_id)
    .eq("school_id", actor.school_id).is("deleted_at", null).maybeSingle();
  if (!assignment) throw new Error("Assignment not available to this account");

  const { data: enrolment } = await supabase.from("student_profiles")
    .select("id").eq("user_id", user.id).eq("class_id", assignment.class_id)
    .is("deleted_at", null).maybeSingle();
  if (!enrolment) throw new Error("Not enrolled in this class");

  const { data: existing } = await supabase.from("assignment_submissions")
    .select("id").eq("assignment_id", assignment_id)
    .eq("student_user_id", user.id).maybeSingle();
  if (existing) {
    const { data, error } = await supabase.from("assignment_submissions")
      .update({ text_answer, updated_at: new Date().toISOString() })
      .eq("id", existing.id).eq("student_user_id", user.id)
      .select("id").maybeSingle();
    if (error || !data) throw new Error("Unable to revise answer");
  } else {
    const { error } = await supabase.from("assignment_submissions")
      .insert({ school_id: actor.school_id, assignment_id,
        student_user_id: user.id, text_answer });
    if (error) throw new Error("Unable to submit answer");
  }
  revalidatePath(schoolPath);
}

/** Staff mark only submissions from their authorised class and subject.
 * Grade ownership and maximum marks are independently guarded by RLS.
 */
export async function reviewCoursework(form: FormData) {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error("Sign in required");
  const { data: actor } = await supabase.from("users")
    .select("role,school_id,is_active").eq("id",user.id).maybeSingle();
  if (!actor?.is_active || !["admin","head","teacher"].includes(actor.role))
    throw new Error("Not authorised to mark coursework");

  const submission_id = String(form.get("submission_id") ?? "");
  const scoreInput = String(form.get("score") ?? "").trim();
  const feedback = String(form.get("feedback") ?? "").trim();
  if (!uuid.test(submission_id) || feedback.length > 2000 ||
    !/^(?:0|[1-9]\d{0,4})(?:\.\d{1,2})?$/.test(scoreInput))
    throw new Error("Invalid mark or feedback");

  const score = Number(scoreInput);
  const { data: submission } = await supabase.from("assignment_submissions")
    .select("id,assignment_id").eq("id",submission_id)
    .eq("school_id",actor.school_id).maybeSingle();
  if (!submission) throw new Error("Submission unavailable to your account");
  const { data: assignment } = await supabase.from("school_assignments")
    .select("id,class_id,subject_id,max_score").eq("id",submission.assignment_id)
    .eq("school_id",actor.school_id).is("deleted_at",null).maybeSingle();
  if (!assignment || score > Number(assignment.max_score))
    throw new Error("Score exceeds maximum or assignment unavailable");

  if (actor.role === "teacher") {
    const { data: klass } = await supabase.from("classes").select("teacher_id")
      .eq("id",assignment.class_id).eq("school_id",actor.school_id)
      .is("deleted_at",null).maybeSingle();
    const { data: teachingAssignment } = await supabase.from("class_subject_teacher")
      .select("id").eq("class_id",assignment.class_id)
      .eq("subject_id",assignment.subject_id).eq("teacher_id",user.id)
      .is("deleted_at",null).limit(1).maybeSingle();
    if (klass?.teacher_id !== user.id && !teachingAssignment)
      throw new Error("You are not assigned to this subject");
  }

  const { data: existing } = await supabase.from("assignment_reviews")
    .select("id").eq("submission_id",submission_id).maybeSingle();
  const now = new Date().toISOString();
  if (existing) {
    const { data, error } = await supabase.from("assignment_reviews")
      .update({score,feedback,reviewed_by:user.id,reviewed_at:now})
      .eq("id",existing.id).select("id").maybeSingle();
    if (error || !data) throw new Error("Unable to revise feedback");
  } else {
    const { error } = await supabase.from("assignment_reviews")
      .insert({school_id:actor.school_id,submission_id,score,feedback,
        reviewed_by:user.id});
    if (error) throw new Error("Unable to save feedback");
  }
  revalidatePath(schoolPath);
}
