import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import {curriculumUnits,curriculumGrades,curriculumSubjects,curriculumVersion} from "@/lib/curriculum/catalog";

export const dynamic="force-dynamic";

export default async function CurriculumCatalogue({
 searchParams
}:{searchParams:Promise<{level?:string;subject?:string;q?:string}>}){
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();
 if(!user)redirect("/login");
 const {data:profile}=await supabase.from("users")
  .select("id,role,school_id,is_active").eq("id",user.id).maybeSingle();
 if(!profile?.is_active || !["admin","head","teacher","student","parent"].includes(profile.role))
  notFound();
 const params=await searchParams;
 const level=(params.level??"").slice(0,40);
 const subject=(params.subject??"").slice(0,60);
 const q=(params.q??"").trim().slice(0,100);
 const matches=curriculumUnits.filter(u=>
  (!level || u.level===level)&&(!subject || u.subject===subject)&&
  (!q || [u.title,u.topic,u.level,u.subject,u.objective].join(" ")
     .toLowerCase().includes(q.toLowerCase()))
 );
 return <main className="dashboard-page">
 <header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard/learning">← Learning</Link></header>
 <div className="dash-wrap">
  <p className="eyebrow">2024–2030 HERITAGE-BASED CURRICULUM</p>
  <h1>Lessons, exercises &amp; assignments</h1>
  <p className="subhead">{curriculumUnits.length} original learning units for ECD A to Grade 7, organised using the supplied MoPSE syllabi. Each includes a lesson, worked example, exercises and an assignment template.</p>
  <section className="summary-card" aria-label="Filter curriculum">
    <h2>Find learning material</h2>
    <form method="get" className="curriculum-filters">
      <label>Grade
        <select name="level" defaultValue={level}><option value="">All levels</option>{curriculumGrades.map(g=><option key={g}>{g}</option>)}</select>
      </label>
      <label>Subject
        <select name="subject" defaultValue={subject}><option value="">All subjects</option>{curriculumSubjects.map(s=><option key={s}>{s}</option>)}</select>
      </label>
      <label>Search
        <input type="search" name="q" defaultValue={q} maxLength={100} placeholder="Try fractions or water"/>
      </label>
      <button type="submit" className="outline-button">Apply filters</button>
    </form>
  </section>
  <p className="privacy-note">{matches.length} matching units · Catalogue version {curriculumVersion}</p>
  {!matches.length?<p className="notice-panel">No lessons match those filters. <Link href="/dashboard/curriculum">Clear filters</Link></p>:
  <div className="summary-cards">
   {matches.map(unit=><Link key={unit.id} href={`/dashboard/curriculum/${encodeURIComponent(unit.id)}`} className="summary-card" style={{color:"inherit",textDecoration:"none"}}>
      <span>{unit.level} · {unit.subject}</span>
      <strong>{unit.title}</strong>
      <span>{unit.topic} · {unit.durationMinutes} min</span>
      <p>{unit.objective}</p>
      <span style={{fontWeight:700,color:"var(--green)"}}>Open lesson and exercises →</span>
    </Link>)}
  </div>}
  <p className="privacy-note">Explanations and exercises are original curriculum-aligned material. School staff must review before using them for formal assessment. No commercial textbook pages or artwork are reproduced.</p>
 </div></main>;
}
