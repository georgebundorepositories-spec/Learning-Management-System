import { redirect } from "next/navigation";
import Link from "next/link";
import { createClient } from "@/lib/supabase/server";
import { signOut } from "./actions";
import { messages, languages, type Language } from "@/lib/i18n";

export const dynamic = "force-dynamic";
export default async function Dashboard() {
  const supabase = await createClient();
  const { data: { user }, error } = await supabase.auth.getUser();
  if (error || !user) redirect("/login");

  const { data: profile } = await supabase.from("users")
    .select("full_name,preferred_lang,role,school_id,is_active")
    .eq("id", user.id).maybeSingle();
  const lang: Language = profile && languages.includes(profile.preferred_lang as Language)
    ? (profile.preferred_lang as Language) : "en";
  const words = messages[lang];
  const { data: school } = profile?.is_active
    ? await supabase.from("schools").select("name").eq("id", profile.school_id).maybeSingle()
    : { data: null };

  return <main className="dashboard-page">
    <header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div>
      <form action={signOut}><button type="submit" className="outline-button">{words.signOut}</button></form>
    </header>
    <div className="dash-wrap"><p className="eyebrow">ZIMSCHOOL LMS • PRIVATE WORKSPACE</p>
      <h1>{words.dashboard}</h1>
      {profile?.is_active ? <>
        <p className="subhead">{profile.full_name}</p>
        <div className="summary-cards">
          <article className="summary-card"><span>{words.school}</span><strong>{school?.name ?? "—"}</strong></article>
          <article className="summary-card"><span>{words.role}</span><strong>{profile.role}</strong></article>
        </div>
        <div className="dashboard-shortcuts"><Link href="/calendar" className="outline-button">Open offline calendar ↗</Link><Link href="/demo" className="outline-button">Fictional school preview ↗</Link></div>
        <p className="privacy-note">{words.privacy}</p>
      </> : <section role="status" className="notice-panel">{words.pending}</section>}
    </div>
  </main>;
}
