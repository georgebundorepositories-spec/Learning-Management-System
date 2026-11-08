"use server";
import {revalidatePath} from "next/cache";
import {createClient} from "@/lib/supabase/server";

export async function publishNotice(form:FormData){
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();
 if(!user)throw new Error("Sign-in required");
 const {data:actor}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active || !["admin","head"].includes(actor.role))
 throw new Error("Only school administrators can publish notices");
 const title=String(form.get("title")??"").trim();
 const body=String(form.get("body")??"").trim();
 const audience_role=String(form.get("audience_role")??"all");
 const request_key=String(form.get("request_key")??"");
 if(title.length<3 || title.length>160 || body.length<3 || body.length>4000 ||
 !/^[0-9a-f-]{36}$/i.test(request_key) ||
 !["all","admin","head","teacher","student","parent","bursar"].includes(audience_role))
 throw new Error("Invalid notice details");

 const {error}=await supabase.from("school_notices").insert({
 school_id:actor.school_id,title,body,audience_role,published_by:user.id,request_key
 });
 if(error){
  if(error.code!=="23505")throw new Error("Unable to publish notice");
  const {data:original}=await supabase.from("school_notices")
    .select("title,body,audience_role,published_by")
    .eq("school_id",actor.school_id).eq("request_key",request_key).maybeSingle();
  if(!original || original.title!==title || original.body!==body ||
   original.audience_role!==audience_role || original.published_by!==user.id)
   throw new Error("Request identifier already used for another notice");
 }
 revalidatePath("/dashboard/notices");
}
