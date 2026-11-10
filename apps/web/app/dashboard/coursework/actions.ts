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
  if (!uuid.test(class_id) || !uuid.test(subject_id)
    || title.length < 3 || title.length > 180
    || instructions.length < 3 || instructions.length > 5000
    || (due_on !== "" && !/^\d{4}-\d{2}-\d{2}$/.test(due_on)))
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
    title, instructions, due_on: due_on || null, created_by: user.id
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
