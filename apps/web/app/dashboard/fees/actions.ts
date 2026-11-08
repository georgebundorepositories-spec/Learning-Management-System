"use server";
import {revalidatePath} from "next/cache";
import {createClient} from "@/lib/supabase/server";

export async function issueInvoice(form:FormData){
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();
 if(!user)throw new Error("Sign-in required");
 const {data:actor}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active || !["admin","head"].includes(actor.role))
 throw new Error("Not authorised to issue invoices");

 const student_user_id=String(form.get("student_user_id")??"");
 const description=String(form.get("description")??"").trim();
 const amount=String(form.get("amount")??"").trim();
 const currency=String(form.get("currency")??"");
 const due_on=String(form.get("due_on")??"");
 const request_key=String(form.get("request_key")??"");

 // Accept exact two-decimal monetary amounts without floating-point rounding.
 if(!/^[0-9a-f-]{36}$/i.test(student_user_id)
  || !/^[0-9a-f-]{36}$/i.test(request_key)
  || description.length<3 || description.length>300
  || !/^(?:0|[1-9]\d{0,8})(?:\.\d{1,2})?$/.test(amount)
  || Number(amount)<=0 || Number(amount)>100000000
  || !["USD","ZWG"].includes(currency)
  || (due_on!=="" && !/^\d{4}-\d{2}-\d{2}$/.test(due_on)))
 throw new Error("Invalid invoice details");

 const {data:learner}=await supabase.from("users").select("id").eq("id",student_user_id)
 .eq("school_id",actor.school_id).eq("role","student").eq("is_active",true).maybeSingle();
 if(!learner)throw new Error("Learner not available in this school");

 const {error}=await supabase.from("fee_invoices").insert({
  school_id:actor.school_id,student_user_id,description,amount,currency,
  due_on:due_on||null,created_by:user.id,status:"issued",request_key
 });
 if(error){
   if(error.code!=="23505")throw new Error("Could not issue invoice");
   // Retried requests succeed only if they exactly match the original charge.
   const {data:original}=await supabase.from("fee_invoices")
     .select("student_user_id,description,amount,currency,due_on,created_by")
     .eq("school_id",actor.school_id).eq("request_key",request_key).maybeSingle();
   if(!original || original.created_by!==user.id ||
      original.student_user_id!==student_user_id ||
      original.description!==description ||
      Number(original.amount)!==Number(amount) ||
      original.currency!==currency || (original.due_on??"")!==due_on)
     throw new Error("Request identifier already used for another invoice");
 }
 revalidatePath("/dashboard/fees");
}
