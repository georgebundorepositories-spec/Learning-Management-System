"use server";
import {revalidatePath} from "next/cache";
import {createClient} from "@/lib/supabase/server";

export async function issueInvoice(form:FormData){
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();if(!user)throw new Error("Sign-in required");
 const {data:actor}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active || !["admin","head"].includes(actor.role))throw new Error("Not authorised to issue invoices");
 const student_user_id=String(form.get("student_user_id")??"");
 const description=String(form.get("description")??"").trim();
 const amount=Number(form.get("amount"));
 const currency=String(form.get("currency")??"");
 const due_on=String(form.get("due_on")??"");
 if(!/^[0-9a-f-]{36}$/i.test(student_user_id) || description.length<3 || description.length>300
  || !Number.isFinite(amount) || amount<=0 || amount>100000000
  || Math.round(amount*100)!==amount*100 || !["USD","ZWG"].includes(currency)
  || (due_on!=="" && !/^\d{4}-\d{2}-\d{2}$/.test(due_on)))throw new Error("Invalid invoice details");
 const {data:learner}=await supabase.from("users").select("id").eq("id",student_user_id)
 .eq("school_id",actor.school_id).eq("role","student").eq("is_active",true).maybeSingle();
 if(!learner)throw new Error("Learner not available in this school");
 const {error}=await supabase.from("fee_invoices").insert({
  school_id:actor.school_id,student_user_id,description,amount,currency,
  due_on:due_on||null,created_by:user.id,status:"issued"
 });
 if(error)throw new Error("Could not issue invoice");
 revalidatePath("/dashboard/fees");
}
