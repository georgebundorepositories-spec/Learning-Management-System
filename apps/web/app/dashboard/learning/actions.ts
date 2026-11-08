"use server";
import {revalidatePath} from "next/cache";
import {createClient} from "@/lib/supabase/server";
export async function addLearningResource(formData:FormData){
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();if(!user)throw new Error("Sign in required");
 const {data:actor}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active || !["admin","head","teacher"].includes(actor.role))throw new Error("Not authorised");
 const subject_id=String(formData.get("subject_id")??"");
 const title=String(formData.get("title")??"").trim();
 const description=String(formData.get("description")??"").trim();
 const resource_url=String(formData.get("resource_url")??"").trim();
 if(title.length<3 || title.length>180 || description.length>3000 || !/^https:\/\//i.test(resource_url) || resource_url.length>2048)throw new Error("Invalid resource information");
 const {data:subject}=await supabase.from("subjects").select("id").eq("id",subject_id).eq("school_id",actor.school_id).maybeSingle();
 if(!subject)throw new Error("Subject not available");
 const {error}=await supabase.from("learning_resources").insert({school_id:actor.school_id,subject_id,title,description,resource_url,created_by:user.id});
 if(error)throw new Error("Unable to save learning resource");
 revalidatePath("/dashboard/learning");
}
