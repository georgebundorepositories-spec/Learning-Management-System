"use server";
import {revalidatePath} from "next/cache";
import {createClient} from "@/lib/supabase/server";

export async function publishNotice(form:FormData){
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();if(!user)throw new Error("Sign-in required");
 const {data:actor}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active || !["admin","head"].includes(actor.role))throw new Error("Only school administrators can publish notices");
 const title=String(form.get("title")??"").trim();
 const body=String(form.get("body")??"").trim();
 const audience_role=String(form.get("audience_role")??"all");
 if(title.length<3 || title.length>160 || body.length<3 || body.length>4000 ||
 !["all","admin","head","teacher","student","parent","bursar"].includes(audience_role))
 throw new Error("Invalid notice details");
 const {error}=await supabase.from("school_notices").insert({
 school_id:actor.school_id,title,body,audience_role,published_by:user.id
 });
 if(error)throw new Error("Unable to publish notice");
 revalidatePath("/dashboard/notices");
}
