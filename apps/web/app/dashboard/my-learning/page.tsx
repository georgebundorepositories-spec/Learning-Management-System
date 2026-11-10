import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
export const dynamic="force-dynamic";
export default async function MyLearning(){
 const supabase=await createClient();const {data:{user}}=await supabase.auth.getUser();if(!user)redirect("/login");
 const {data:profile}=await supabase.from("users").select("school_id,role,is_active,full_name").eq("id",user.id).maybeSingle();
 if(!profile?.is_active || !["parent","student"].includes(profile.role))notFound();
 let studentIds:string[]=[];
 if(profile.role==="student")studentIds=[user.id];
 else {const {data:links}=await supabase.from("student_guardian_link").select("student_user_id").eq("guardian_user_id",user.id).is("deleted_at",null).limit(30);studentIds=(links??[]).map(l=>l.student_user_id);}
 const {data:learners}=studentIds.length?await supabase.from("users").select("id,full_name").eq("school_id",profile.school_id).eq("role","student").in("id",studentIds).limit(30):{data:[]};
 const permitted=(learners??[]).map(l=>l.id);
 const {data:attendance}=permitted.length?await supabase.from("attendance").select("id,student_user_id,date,status").eq("school_id",profile.school_id).in("student_user_id",permitted).is("deleted_at",null).order("date",{ascending:false}).limit(100):{data:[]};
 return <main className="dashboard-page"><header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard">← Dashboard</Link></header><div className="dash-wrap"><p className="eyebrow">{profile.role==="parent"?"PARENT PORTAL":"LEARNER PORTAL"}</p><h1>{profile.role==="parent"?"My children":"My learning"}</h1><p className="subhead">Private attendance records for linked learners</p>{learners?.length?<div className="summary-cards">{learners.map(learner=><section className="summary-card" key={learner.id}><strong>{learner.full_name}</strong>{(attendance??[]).filter(a=>a.student_user_id===learner.id).map(a=><span key={a.id}>{a.date}: {a.status}</span>)}</section>)}</div>:<p className="notice-panel">No linked learner profile is available yet.</p>}<p className="privacy-note">Visit <Link href="/dashboard/progress">Learning progress</Link> for private coursework, marks and attendance, or <Link href="/dashboard/coursework">Assignments</Link> to view class tasks, <Link href="/dashboard/assessments">Assessments</Link> for private marks, or <Link href="/dashboard/learning">Learning materials</Link> for teacher-shared resources.</p></div></main>;
}
