import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
export const dynamic="force-dynamic";

/** Read school activities only; do not invent a pupil's club membership.
 * Discussion requires verified safeguarding, moderation and guardian consent.
 */
export default async function SchoolLife(){
 const db=await createClient();
 const {data:{user}}=await db.auth.getUser();
 if(!user)redirect("/login");
 const {data:actor}=await db.from("users")
  .select("school_id,role,is_active,deleted_at").eq("id",user.id).maybeSingle();
 if(!actor?.is_active||actor.deleted_at||actor.role!=="student")notFound();
 const {data:events,error}=await db.from("calendar_events")
  .select("id,title,start_at,location,description,category")
  .eq("school_id",actor.school_id).eq("is_published",true)
  .eq("approval_status","approved").is("deleted_at",null)
  .in("category",["sports","cultural","meeting"])
  .gte("start_at",new Date().toISOString()).order("start_at",{ascending:true}).limit(15);
 return <main className="dashboard-page">
  <header className="topbar"><Link href="/dashboard" className="outline-button">← Home</Link></header>
  <div className="dash-wrap simple-dashboard">
   <p className="eyebrow">LEARNER PORTAL</p><h1>My School Life</h1>
   <p className="subhead">Find activities your school has announced.</p>
   <section className="simple-life-section">
    <h2>School activities</h2>
    {error?<p role="alert">School events could not be loaded. Try again later.</p>:
     !events?.length?<p>No school activities have been published yet.</p>:
      <div className="simple-option-list">{events.map(event=><article
       key={event.id} className="simple-life-event">
       <strong>{event.title}</strong>
       <span>{new Date(event.start_at).toLocaleString("en-ZW",
         {timeZone:"Africa/Harare",dateStyle:"medium",timeStyle:"short"})}</span>
       {event.location?<span>{event.location}</span>:null}
       {event.description?<p>{event.description}</p>:null}
      </article>)}</div>}
   </section>
   <section className="simple-life-section">
    <h2>My clubs</h2>
    <p>Your school has not yet set up a verified club-membership register here.
      Activities above do not mean you are enrolled in a club.</p>
   </section>
   <section className="simple-life-section">
    <h2>Talk with other learners</h2>
    <p>Peer discussion is not active yet. Before learners can post, your school must
      appoint staff moderators and approve child-safety and reporting rules.
      Use your teacher or school office for help in the meantime.</p>
   </section>
  </div>
 </main>;
}
