import Link from "next/link";
import {redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import {signOut} from "./actions";
import {homeHubs} from "@/lib/role-home";
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
 const {data:school}=await supabase.from("schools").select("name").eq("id",profile.school_id).maybeSingle();
 const title=profile.role==="student"?"Learner home":profile.role==="teacher"?"Teacher home":
  profile.role==="parent"?"Parent home":profile.role==="admin"?"School administrator":
  profile.role==="head"?"School head":profile.role==="bursar"?"Finance home":"School home";
 return <main className="dashboard-page" id="main-content">
  <header className="topbar">
   <Link href="/dashboard" className="topbrand topbrand-link" aria-label="ZimSchool home">
    <span className="brand-mark">Z</span><strong>ZimSchool LMS</strong>
   </Link>
   <form action={signOut}><button type="submit" className="outline-button">Sign out</button></form>
  </header>
  <div className="dash-wrap simple-dashboard">
   <p className="eyebrow">{title.toUpperCase()}</p>
   <h1>Hello, {profile.full_name?.trim().split(/\s+/)[0]??"there"}</h1>
   <p className="subhead">{school?.name??"Your school"}</p>
   <WorkspaceLauncher modules={homeHubs(profile.role)}/>
   <p className="privacy-note">Only school-authorised information is available to your account.
    If something is missing, ask your school office. Some features need internet access.</p>
  </div>
 </main>;
}
