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
 const {data:profile,error:membershipError}=await supabase.from("users")
  .select("full_name,role,school_id,is_active,deleted_at").eq("id",user.id).maybeSingle();
 if(membershipError)
  return <main className="dash-wrap"><h1>School membership check unavailable</h1>
   <p>The service could not verify your school access. Try refreshing this page; if the error continues, contact the system administrator.</p>
   <form action={signOut}><button type="submit">Sign out</button></form></main>;
 if(!profile)
  return <main className="dash-wrap"><h1>School membership not assigned</h1>
   <p>You have signed in successfully, but this login is not yet assigned to a school.</p>
   <p>Ask your school administrator to open <strong>School members</strong> and register this verified email with the correct designation. Existing confirmed accounts keep their current password.</p>
   <p>No private learner or staff information is available until a school administrator completes the assignment.</p>
   <form action={signOut}><button type="submit">Sign out</button></form></main>;
 if(!profile.is_active||profile.deleted_at)
  return <main className="dash-wrap"><h1>School membership inactive</h1>
   <p>Your login worked, but school access is currently disabled. Please contact your school administrator for review.</p>
   <form action={signOut}><button type="submit">Sign out</button></form></main>;
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
