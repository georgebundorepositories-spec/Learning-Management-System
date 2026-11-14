"use server";

import { revalidatePath } from "next/cache";
import { createClient } from "@/lib/supabase/server";

const uuid = /^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i;

export async function addLearningResource(formData: FormData) {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error("Sign in required");
  const { data: actor } = await supabase.from("users")
    .select("school_id,role,is_active").eq("id",user.id).maybeSingle();
  if (!actor?.is_active || !["admin","head","teacher"].includes(actor.role))
    throw new Error("Not authorised to publish learning materials");

  const subject_id = String(formData.get("subject_id") ?? "");
  const title = String(formData.get("title") ?? "").trim();
  const description = String(formData.get("description") ?? "").trim();
  const rawUrl = String(formData.get("resource_url") ?? "").trim();
  let resource_url = "";
  try {
    const parsed = new URL(rawUrl);
    if (parsed.protocol !== "https:" || !parsed.hostname) throw new Error("HTTPS only");
    resource_url = parsed.toString();
  } catch {
    throw new Error("Resource link must be a valid HTTPS URL");
  }
  if (!uuid.test(subject_id) || title.length < 3 || title.length > 180
      || description.length > 3000 || resource_url.length > 2048)
    throw new Error("Invalid learning resource information");

  const { data: subject } = await supabase.from("subjects").select("id")
    .eq("id",subject_id).eq("school_id",actor.school_id)
    .is("deleted_at",null).maybeSingle();
  if (!subject) throw new Error("Subject not available to this school");

  if (actor.role === "teacher") {
    const [{ data: homeroom },{ data: assignment }] = await Promise.all([
      supabase.from("classes").select("id").eq("school_id",actor.school_id)
        .eq("teacher_id",user.id).is("deleted_at",null).limit(1).maybeSingle(),
      supabase.from("class_subject_teacher").select("id")
        .eq("teacher_id",user.id).eq("subject_id",subject_id)
        .is("deleted_at",null).limit(1).maybeSingle()
    ]);
    if (!homeroom && !assignment)
      throw new Error("You are not assigned to teach this subject");
  }

  const { error } = await supabase.from("learning_resources").insert({
    school_id:actor.school_id,subject_id,title,description,
    resource_url,created_by:user.id
  });
  if (error) throw new Error("Unable to save learning resource");
  revalidatePath("/dashboard/learning");
}
