import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
export const dynamic="force-dynamic";

export default async function ReportsPage(){
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();if(!user)redirect("/login");
 const {data:actor}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active || !["admin","head"].includes(actor.role))notFound();

 const [learners,classes,attendance,assessments,resources]=await Promise.all([
 supabase.from("users").select("id",{count:"exact",head:true}).eq("school_id",actor.school_id).eq("role","student").eq("is_active",true),
 supabase.from("classes").select("id",{count:"exact",head:true}).eq("school_id",actor.school_id).is("deleted_at",null),
 supabase.from("attendance").select("status,date").eq("school_id",actor.school_id).is("deleted_at",null).order("date",{ascending:false}).limit(1000),
 supabase.from("assessments").select("id",{count:"exact",head:true}).eq("school_id",actor.school_id).is("deleted_at",null),
 supabase.from("learning_resources").select("id",{count:"exact",head:true}).eq("school_id",actor.school_id).is("deleted_at",null),
 ]);
 const countBy=(status:string)=>(attendance.data??[]).filter(a=>a.status===status).length;
 const total=attendance.data?.length??0;
 const stats=[
 {name:"Active learners",value:learners.count,error:learners.error},
 {name:"Classes",value:classes.count,error:classes.error},
 {name:"Assessments",value:assessments.count,error:assessments.error},
 {name:"Learning materials",value:resources.count,error:resources.error},
 ];
 return <main className="dashboard-page"><header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard" className="outline-button">← Dashboard</Link></header>
 <div className="dash-wrap"><p className="eyebrow">SCHOOL REPORTS</p><h1>School overview</h1><p className="subhead">Live summary of school records visible to your administrator account.</p>
 <div className="summary-cards">{stats.map(s=><article className="summary-card" key={s.name}><span>{s.name}</span><strong>{s.error?"Unavailable":s.value??"—"}</strong></article>)}</div>
 <p className="privacy-note"><Link href="/dashboard/progress">Open individual learning progress →</Link> (read-only, authorised roster)</p>
 <h2>Attendance snapshot</h2><p className="privacy-note">Based on the most recent {total} available attendance entries (maximum 1,000), not a complete-term attendance rate.</p>
 {attendance.error?<p className="notice-panel" role="alert">Attendance summary unavailable.</p>:<div className="summary-cards">{["present","absent","late","excused"].map(status=><article className="summary-card" key={status}><span>{status}</span><strong>{countBy(status)}</strong></article>)}</div>}
 <p className="privacy-note">For complete term totals and legally releasable reports, a separately tested export and reconciliation workflow is required.</p>
 </div></main>;
}