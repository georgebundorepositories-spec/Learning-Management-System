import Link from "next/link";
import {redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import {signOut} from "./actions";
import {visibleModules} from "@/lib/portal-access";
import WorkspaceLauncher from "./WorkspaceLauncher";

export const dynamic="force-dynamic";

export default async function Dashboard(){
 const supabase=await createClient();
 const {data:{user},error}=await supabase.auth.getUser();
 if(error||!user)redirect("/login");
 const {data:profile}=await supabase.from("users")
  .select("full_name,role,school_id,is_active,deleted_at").eq("id",user.id).maybeSingle();
 if(!profile?.is_active||profile.deleted_at)
  return <main className="dash-wrap"><h1>Account unavailable</h1>
   <p>Your school administrator must confirm your active membership.</p>
   <form action={signOut}><button>Sign out</button></form></main>;
 const {data:school}=await supabase.from("schools")
  .select("name").eq("id",profile.school_id).maybeSingle();
 const available=visibleModules(profile.role);
 return <main className="dashboard-page" id="main-content">
  <header className="topbar">
   <Link className="topbrand topbrand-link" href="/dashboard" aria-label="ZimSchool dashboard">
    <span className="brand-mark">Z</span><strong>ZimSchool LMS</strong>
   </Link>
   <form action={signOut}><button type="submit" className="outline-button">Sign out</button></form>
  </header>
  <div className="dash-wrap dashboard-home">
   <p className="eyebrow">{profile.role==="teacher"?"TEACHER PORTAL":
     profile.role==="student"?"LEARNER PORTAL":
     profile.role==="parent"?"PARENT PORTAL":"SCHOOL WORKSPACE"}</p>
   <h1>Welcome, {profile.full_name}</h1>
   <p className="subhead">{school?.name??"Your school"} · Open a tool to continue your work</p>
   <div className="dashboard-context-grid">
    <div className="dashboard-context-card">
     <span>School</span><strong>{school?.name??"Your school"}</strong>
    </div>
    <div className="dashboard-context-card">
     <span>Your role</span><strong>{profile.role}</strong>
    </div>
    <div className="dashboard-context-card">
     <span>School access</span><strong>Verified account</strong>
     <small>Actions are confirmed against school permissions</small>
    </div>
   </div>
   <WorkspaceLauncher modules={available}/>
   <p className="privacy-note">Your tools are based on the currently authenticated school role.
    Opening a page never bypasses school data permissions. Offline library lessons and private school records have different availability.</p>
  </div>
 </main>;
}
