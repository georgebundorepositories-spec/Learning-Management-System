import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import InviteForm from "./InviteForm";
import MembershipManager from "./MembershipManager";

export const dynamic="force-dynamic";
export default async function SchoolMemberInvitations(){
 const db=await createClient();
 const {data:{user}}=await db.auth.getUser();
 if(!user)redirect("/login");
 const {data:actor}=await db.from("users")
  .select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active||actor.role!=="admin")notFound();
 const [classes,learners,members,parents,teachers,enrolments,guardianLinks]=await Promise.all([
  db.from("classes").select("id,name,teacher_id").eq("school_id",actor.school_id).is("deleted_at",null).order("name").limit(150),
  db.from("users").select("id,full_name").eq("school_id",actor.school_id)
   .eq("role","student").eq("is_active",true).is("deleted_at",null).limit(150),
  db.from("users").select("id,role,is_active").eq("school_id",actor.school_id)
   .is("deleted_at",null).limit(500),
  db.from("users").select("id,full_name").eq("school_id",actor.school_id)
   .eq("role","parent").eq("is_active",true).is("deleted_at",null).limit(150),
  db.from("users").select("id,full_name").eq("school_id",actor.school_id)
   .eq("role","teacher").eq("is_active",true).is("deleted_at",null).limit(150),
  db.from("student_profiles").select("user_id,class_id").is("deleted_at",null).limit(200),
  db.from("student_guardian_link").select("id,student_user_id,guardian_user_id")
   .is("deleted_at",null).limit(200)
 ]);
 const counts=(members.data??[]).filter(m=>m.is_active).reduce(
  (a,m)=>({...a,[m.role]:(a[m.role]??0)+1}),{} as Record<string,number>);
 return <main className="dashboard-page">
  <header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard" className="outline-button">← Dashboard</Link></header>
  <div className="dash-wrap">
   <p className="eyebrow">ADMINISTRATION · CONTROLLED INVITATIONS</p>
   <h1>Invite two school members</h1>
   <p className="subhead">Invite a teacher, learner or parent one account at a time using their real email and unique Zimbabwe mobile number. The designation controls which portals they see after sign-in.</p>
   <div className="summary-cards">
    <article className="summary-card"><span>Active teachers</span><strong>{counts.teacher??0}</strong></article>
    <article className="summary-card"><span>Active learners</span><strong>{counts.student??0}</strong></article>
    <article className="summary-card"><span>Active parents</span><strong>{counts.parent??0}</strong></article>
   </div>
   {classes.error||learners.error||members.error?<p className="notice-panel" role="alert">School roster could not be verified; retry later.</p>:
    <section className="summary-card">
      <h2>Send a secure invitation</h2>
      <p>Complete this form twice for two additional members. Invitations are emailed; passwords are never displayed or stored in the LMS administrator's browser.</p>
      <InviteForm classes={(classes.data??[]).map(c=>({id:c.id,name:c.name}))}
       learners={(learners.data??[]).map(s=>({id:s.id,name:s.full_name}))}/>
    </section>}
   <h2>Manage enrolled members and family access</h2>
   {classes.error||learners.error||parents.error||teachers.error||enrolments.error||guardianLinks.error
    ?<p role="alert" className="notice-panel">Unable to verify class and guardian relationships.</p>
    :<MembershipManager
      classes={(classes.data??[]).map(c=>({id:c.id,name:c.name,teacher_id:c.teacher_id}))}
      students={(learners.data??[]).map(s=>({id:s.id,name:s.full_name}))}
      parents={(parents.data??[]).map(s=>({id:s.id,name:s.full_name}))}
      teachers={(teachers.data??[]).map(s=>({id:s.id,name:s.full_name}))}
      enrolments={enrolments.data??[]}
      links={guardianLinks.data??[]}/>}
   <p className="privacy-note">A learner without an assigned class or a parent without a linked learner can sign in but will not see another student's private work. School staff must complete those relationships before full functionality is available.</p>
  </div>
 </main>;
}
