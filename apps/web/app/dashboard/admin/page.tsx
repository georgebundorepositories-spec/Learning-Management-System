import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
export const dynamic="force-dynamic";

export default async function AdminPage(){
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();if(!user)redirect("/login");
 const {data:actor}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active || !["admin","head"].includes(actor.role))notFound();
 const [{data:school},users,flags,events]=await Promise.all([
 supabase.from("schools").select("name,type,province,district,currency,locale_default").eq("id",actor.school_id).maybeSingle(),
 supabase.from("users").select("id,role,is_active").eq("school_id",actor.school_id).limit(500),
 supabase.from("feature_flags").select("id,key,value").eq("school_id",actor.school_id).is("deleted_at",null).limit(50),
 supabase.from("audit_log").select("id,action,entity,created_at").eq("school_id",actor.school_id).order("created_at",{ascending:false}).limit(20)
 ]);
 const staff=(users.data??[]).filter(u=>["admin","head","teacher","bursar"].includes(u.role)).length;
 return <main className="dashboard-page"><header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard" className="outline-button">← Dashboard</Link></header>
 <div className="dash-wrap"><p className="eyebrow">SCHOOL ADMINISTRATION</p><h1>{school?.name??"School settings"}</h1><p className="subhead">Read-only operational overview. Account activation and policy configuration require the authorised provisioning workflow.</p>
 <div className="summary-cards">
 <article className="summary-card"><span>School</span><strong>{school?.name??"Unavailable"}</strong><span>{school?.type??"School"} · {school?.district??"District"}, {school?.province??"Province"}</span></article>
 <article className="summary-card"><span>Staff profiles</span><strong>{staff}</strong></article>
 <article className="summary-card"><span>Inactive accounts (first 500)</span><strong>{(users.data??[]).filter(u=>!u.is_active).length}</strong></article>
 <article className="summary-card"><span>Default currency</span><strong>{school?.currency??"—"}</strong><span>Language: {school?.locale_default??"—"}</span></article>
 </div>
 <h2>School feature flags</h2>{flags.error?<p role="alert" className="notice-panel">Unable to load feature flags.</p>:!flags.data?.length?<p className="notice-panel">No feature flags configured.</p>:<div className="summary-cards">{flags.data.map(f=><article className="summary-card" key={f.id}><strong>{f.key}</strong><code>{JSON.stringify(f.value)}</code></article>)}</div>}
 <h2>Recent audit activity</h2>{events.error?<p role="alert" className="notice-panel">Unable to read audit activity.</p>:!events.data?.length?<p className="notice-panel">No visible audit entries.</p>:<div className="summary-cards">{events.data.map(e=><article className="summary-card" key={e.id}><span>{new Date(e.created_at).toLocaleString("en-ZW",{timeZone:"Africa/Harare"})}</span><strong>{e.action}</strong><span>{e.entity}</span></article>)}</div>}
 <p className="privacy-note">No privileged API keys are needed in the browser. School membership and role changes are intentionally unavailable from this page.</p>
 </div></main>;
}