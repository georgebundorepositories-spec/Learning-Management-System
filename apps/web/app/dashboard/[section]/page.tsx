import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";

export const dynamic = "force-dynamic";
const config = {
 learners: { title:"Learner register", roles:["admin","head","teacher"], table:"student_profiles", fields:"user_id,admission_no,class_id", label:"Learner ID" },
 teachers: { title:"Teaching staff", roles:["admin","head","teacher"], table:"staff_profiles", fields:"user_id,employee_no", label:"Staff ID" },
 parents: { title:"Guardian directory", roles:["admin","head"], table:"parent_profiles", fields:"user_id,relationship", label:"Guardian ID" },
 attendance: { title:"Attendance register", roles:["admin","head","teacher"], table:"attendance", fields:"student_user_id,date,status", label:"Learner ID" },
 learning: { title:"Subjects", roles:["admin","head","teacher","student","parent"], table:"subjects", fields:"id,name", label:"Subject ID" },
} as const;
type Section = keyof typeof config;
export default async function Workspace({params}:{params:Promise<{section:string}>}) {
 const {section} = await params;
 if (!(section in config)) notFound();
 const item=config[section as Section];
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();
 if(!user) redirect("/login");
 const {data:profile}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!profile?.is_active || !item.roles.some(role=>role===profile.role)) notFound();
 // Profile tables lack school_id; authorised users are filtered through the school-scoped users table.
 let rows: Record<string,unknown>[]=[];
 let errorMessage="";
 if(section==="learners" || section==="teachers" || section==="parents"){
   const role=section==="learners"?"student":section==="teachers"?"teacher":"parent";
   const {data:members,error}=await supabase.from("users").select("id,full_name,role").eq("school_id",profile.school_id).eq("role",role).eq("is_active",true).limit(100);
   rows=(members??[]) as Record<string,unknown>[];
   if(error) errorMessage="Unable to load the directory.";
 } else {
   const query=supabase.from(item.table).select(item.fields).limit(100);
   const {data,error}=section==="attendance" ? await query.eq("school_id",profile.school_id) : await query.eq("school_id",profile.school_id);
   rows=(data??[]) as Record<string,unknown>[];
   if(error) errorMessage="Unable to load records.";
 }
 return <main className="dashboard-page"><header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard" className="outline-button">← Dashboard</Link></header>
 <div className="dash-wrap"><p className="eyebrow">SCHOOL WORKSPACE</p><h1>{item.title}</h1><p className="subhead">{rows.length} records displayed · Restricted school access</p>
 {errorMessage?<section role="alert" className="notice-panel">{errorMessage}</section>:rows.length===0?<section className="notice-panel">No records available for your account yet.</section>:
 <div className="summary-cards">{rows.map((record,index)=><article className="summary-card" key={String(record.id??record.user_id??record.student_user_id??index)}><span>{item.label}</span><strong>{String(record.full_name??record.name??record.admission_no??record.employee_no??record.user_id??record.student_user_id??"Record")}</strong>{record.status?<span>{String(record.status)}</span>:null}{record.date?<span>{String(record.date)}</span>:null}</article>)}</div>}
 <p className="privacy-note">Read-only foundation. Editing, attendance submission and learning uploads are not enabled yet.</p></div></main>;
}
