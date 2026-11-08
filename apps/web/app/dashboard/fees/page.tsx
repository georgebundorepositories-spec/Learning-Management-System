import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import {issueInvoice} from "./actions";
export const dynamic="force-dynamic";

export default async function FeesPage(){
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();if(!user)redirect("/login");
 const {data:actor}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active || !["admin","head","bursar","parent","student"].includes(actor.role))notFound();
 const canIssue=["admin","head"].includes(actor.role);
 const {data:invoices,error}=await supabase.from("fee_invoices")
 .select("id,student_user_id,description,amount,currency,due_on,status,created_at")
 .eq("school_id",actor.school_id).order("created_at",{ascending:false}).limit(100);
 const {data:learnerOptions}=canIssue?await supabase.from("users").select("id,full_name")
 .eq("school_id",actor.school_id).eq("role","student").eq("is_active",true).limit(200):{data:[]};
 const ids=[...new Set((invoices??[]).map(i=>i.student_user_id))];
 const {data:learnerNames}=ids.length?await supabase.from("users").select("id,full_name")
 .eq("school_id",actor.school_id).in("id",ids).limit(100):{data:[]};
 return <main className="dashboard-page">
 <header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard" className="outline-button">← Dashboard</Link></header>
 <div className="dash-wrap"><p className="eyebrow">SCHOOL FINANCE</p><h1>Fee invoices</h1>
 <p className="subhead">School-issued charges only. Online payment, USSD, transaction confirmation and receipts are not enabled.</p>
 {canIssue?<section className="summary-card"><h2>Issue an invoice</h2>
 <form action={issueInvoice} className="resource-form">
 <label>Learner<select name="student_user_id" required>{(learnerOptions??[]).map(s=><option key={s.id} value={s.id}>{s.full_name}</option>)}</select></label>
 <label>Description<input name="description" required minLength={3} maxLength={300} placeholder="School fees — Term 1"/></label>
 <label>Amount<input name="amount" type="number" min="0.01" max="100000000" step="0.01" required/></label>
 <label>Currency<select name="currency" defaultValue="USD"><option value="USD">USD</option><option value="ZWG">ZWG (ZiG)</option></select></label>
 <label>Due date<input name="due_on" type="date"/></label>
 <button type="submit" className="outline-button" disabled={!learnerOptions?.length}>Issue invoice</button>
 </form></section>:null}
 <h2>Invoices</h2>
 {error?<p className="notice-panel" role="alert">Unable to read invoices.</p>:!invoices?.length?<p className="notice-panel">No invoices available.</p>:
 <div className="summary-cards">{invoices.map(invoice=><article className="summary-card" key={invoice.id}>
 <span>{learnerNames?.find(s=>s.id===invoice.student_user_id)?.full_name??"Learner"}</span>
 <strong>{invoice.currency} {Number(invoice.amount).toFixed(2)}</strong>
 <span>{invoice.description}</span><span>{invoice.due_on?"Due "+invoice.due_on:"No due date"} · {invoice.status}</span>
 <small>Reference: {invoice.id}</small></article>)}</div>}
 <p className="privacy-note">An invoice is not proof of payment. No automatic settlement or receipt generation is available.</p>
 </div></main>;
}
