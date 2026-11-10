import Link from "next/link";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { signOut } from "./actions";
import { visibleModules } from "@/lib/portal-access";

export const dynamic = "force-dynamic";



export default async function Dashboard() {
  const supabase = await createClient();
  const { data: { user }, error } = await supabase.auth.getUser();
  if (error || !user) redirect("/login");
  const { data: profile } = await supabase.from("users")
    .select("full_name,role,school_id,is_active").eq("id", user.id).maybeSingle();
  if (!profile?.is_active) return <main className="dash-wrap"><h1>Account pending</h1><p>Your school administrator must activate your account.</p><form action={signOut}><button>Sign out</button></form></main>;
  const { data: school } = await supabase.from("schools").select("name").eq("id",profile.school_id).maybeSingle();
  const available = visibleModules(profile.role);
  return <main className="dashboard-page">
    <header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><form action={signOut}><button type="submit" className="outline-button">Sign out</button></form></header>
    <div className="dash-wrap"><p className="eyebrow">{profile.role==="teacher"?"TEACHER PORTAL":profile.role==="student"?"LEARNER PORTAL":profile.role==="parent"?"PARENT PORTAL":"SCHOOL WORKSPACE"} · {profile.role.toUpperCase()}</p>
      <h1>Welcome, {profile.full_name}</h1><p className="subhead">{school?.name ?? "Your school"} · Choose a workspace</p>
      <div className="summary-cards"><article className="summary-card"><span>School</span><strong>{school?.name ?? "—"}</strong></article><article className="summary-card"><span>Access</span><strong>{profile.role}</strong></article></div>
      <div className="summary-cards">{available.map(module => <Link key={module.href} href={module.href} className="summary-card" style={{textDecoration:"none",display:"block",color:"inherit"}}><span>WORKSPACE ↗</span><strong style={{display:"block",margin:"10px 0"}}>{module.title}</strong><span>{module.description}</span></Link>)}</div>
      <p className="privacy-note">School information is restricted to your authorised account. Features under development are identified inside each workspace.</p>
    </div>
  </main>;
}
