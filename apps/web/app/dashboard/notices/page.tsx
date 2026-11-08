import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import {publishNotice} from "./actions";
export const dynamic="force-dynamic";

export default async function NoticesPage(){
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();if(!user)redirect("/login");
 const {data:actor}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active)notFound();
 const canPublish=["admin","head"].includes(actor.role);
 const {data:notices,error}=await supabase.from("school_notices")
 .select("id,title,body,audience_role,created_at")
 .eq("school_id",actor.school_id).order("created_at",{ascending:false}).limit(100);
 return <main className="dashboard-page">
 <header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard" className="outline-button">← Dashboard</Link></header>
 <div className="dash-wrap"><p className="eyebrow">COMMUNICATIONS</p><h1>School notices</h1><p className="subhead">In-app announcements for your school. SMS and push delivery are not enabled.</p>
 {canPublish?<section className="summary-card"><h2>Publish a notice</h2><form action={publishNotice} className="resource-form">
 <label>Title<input name="title" required minLength={3} maxLength={160}/></label>
 <label>Audience<select name="audience_role" defaultValue="all"><option value="all">Everyone at this school</option><option value="teacher">Teachers</option><option value="student">Learners</option><option value="parent">Parents</option><option value="bursar">Bursar</option><option value="admin">Administrators</option><option value="head">Head</option></select></label>
 <label className="form-full">Message<textarea name="body" required minLength={3} maxLength={4000} rows={5}/></label>
 <button type="submit" className="outline-button">Publish in app</button></form></section>:null}
 {error?<p className="notice-panel" role="alert">Unable to load notices.</p>:!notices?.length?<p className="notice-panel">No current notices for your account.</p>:
 <div className="summary-cards">{notices.map(notice=><article className="summary-card" key={notice.id}><span>{notice.audience_role==="all"?"School-wide":notice.audience_role} · {new Date(notice.created_at).toLocaleDateString("en-ZW")}</span><strong>{notice.title}</strong><p style={{whiteSpace:"pre-wrap"}}>{notice.body}</p></article>)}</div>}
 </div></main>;
}