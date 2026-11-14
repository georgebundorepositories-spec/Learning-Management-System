import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";

export const dynamic = "force-dynamic";

// Attendance and learning materials have dedicated, more capable routes.
// This catch-all handles only the three school directories.
const directories = {
  learners: { title: "Learner register", roles: ["admin", "head", "teacher"], memberRole: "student", label: "Learner" },
  teachers: { title: "Teaching staff", roles: ["admin", "head"], memberRole: "teacher", label: "Teacher" },
  parents: { title: "Guardian directory", roles: ["admin", "head"], memberRole: "parent", label: "Parent" },
} as const;

type Directory = keyof typeof directories;

export default async function Workspace({ params }: { params: Promise<{ section: string }> }) {
  const { section } = await params;
  if (!(section in directories)) notFound();

  const directory = directories[section as Directory];
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) redirect("/login");

  const { data: profile } = await supabase.from("users")
    .select("school_id,role,is_active")
    .eq("id", user.id)
    .maybeSingle();

  if (!profile?.is_active || !directory.roles.some(role => role === profile.role)) notFound();

  // The users RLS policies restrict results to the signed-in user's school
  // and their authorised subset of learner or staff records.
  const { data: members, error } = await supabase.from("users")
    .select("id,full_name,role")
    .eq("school_id", profile.school_id)
    .eq("role", directory.memberRole)
    .eq("is_active", true)
    .limit(100);

  return <main className="dashboard-page">
    <header className="topbar">
      <div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div>
      <Link href="/dashboard" className="outline-button">← Dashboard</Link>
    </header>
    <div className="dash-wrap">
      <p className="eyebrow">SCHOOL WORKSPACE</p>
      <h1>{directory.title}</h1>
      <p className="subhead">{members?.length ?? 0} records displayed · Restricted school access</p>
      {error ? <section role="alert" className="notice-panel">Unable to load the directory.</section>
        : !members?.length ? <section className="notice-panel">No records available for your account yet.</section>
        : <div className="summary-cards">{members.map(member =>
            <article className="summary-card" key={member.id}>
              <span>{directory.label}</span>
              <strong>{member.full_name ?? "Unnamed member"}</strong>
            </article>
          )}</div>}
      <p className="privacy-note">Directory visibility follows your school's access rules.</p>
    </div>
  </main>;
}
