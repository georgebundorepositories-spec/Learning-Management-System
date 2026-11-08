import Link from "next/link";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { signOut } from "./actions";

export const dynamic = "force-dynamic";

const modules = [
  { href: "/dashboard/admin", title: "Administration", description: "School information, permissions and audit overview", roles: ["admin","head"] },
  { href: "/dashboard/reports", title: "Reports", description: "School metrics and attendance summaries", roles: ["admin","head"] },
  { href: "/dashboard/notices", title: "Messages & Notices", description: "In-app school announcements", roles: ["admin","head","teacher","student","parent","bursar"] },
  { href: "/dashboard/fees", title: "Fees & Invoices", description: "Issued school invoices — payments not yet enabled", roles: ["admin","head","bursar","student","parent"] },
  { href: "/dashboard/assessments", title: "Assessments & Results", description: "Record marks and view learner performance", roles: ["admin","head","teacher","student","parent"] },
  { href: "/dashboard/my-learning", title: "My Learning / My Children", description: "Private learner and guardian attendance view", roles: ["student","parent"] },
  { href: "/dashboard/calendar", title: "Calendar & Sports", description: "Published events, sports and school programme", roles: ["admin","head","teacher","student","parent","bursar"] },
  { href: "/dashboard/learners", title: "Learners", description: "View the school learner register", roles: ["admin","head","teacher"] },
  { href: "/dashboard/teachers", title: "Teachers", description: "View teaching staff and assignments", roles: ["admin","head","teacher"] },
  { href: "/dashboard/parents", title: "Parents", description: "View linked guardians", roles: ["admin","head"] },
  { href: "/dashboard/attendance", title: "Attendance", description: "Daily class attendance register", roles: ["admin","head","teacher"] },
  { href: "/dashboard/learning", title: "Learning", description: "Subject catalogue and learning resources", roles: ["admin","head","teacher","student","parent"] },
] as const;

export default async function Dashboard() {
  const supabase = await createClient();
  const { data: { user }, error } = await supabase.auth.getUser();
  if (error || !user) redirect("/login");
  const { data: profile } = await supabase.from("users")
    .select("full_name,role,school_id,is_active").eq("id", user.id).maybeSingle();
  if (!profile?.is_active) return <main className="dash-wrap"><h1>Account pending</h1><p>Your school administrator must activate your account.</p><form action={signOut}><button>Sign out</button></form></main>;
  const { data: school } = await supabase.from("schools").select("name").eq("id",profile.school_id).maybeSingle();
  const available = modules.filter(module => (module.roles as readonly string[]).includes(profile.role));
  return <main className="dashboard-page">
    <header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><form action={signOut}><button type="submit" className="outline-button">Sign out</button></form></header>
    <div className="dash-wrap"><p className="eyebrow">SCHOOL WORKSPACE · {profile.role.toUpperCase()}</p>
      <h1>Welcome, {profile.full_name}</h1><p className="subhead">{school?.name ?? "Your school"} · Choose a workspace</p>
      <div className="summary-cards"><article className="summary-card"><span>School</span><strong>{school?.name ?? "—"}</strong></article><article className="summary-card"><span>Access</span><strong>{profile.role}</strong></article></div>
      <div className="summary-cards">{available.map(module => <Link key={module.href} href={module.href} className="summary-card" style={{textDecoration:"none",display:"block",color:"inherit"}}><span>WORKSPACE ↗</span><strong style={{display:"block",margin:"10px 0"}}>{module.title}</strong><span>{module.description}</span></Link>)}</div>
      <p className="privacy-note">School information is restricted to your authorised account. Features under development are identified inside each workspace.</p>
    </div>
  </main>;
}
