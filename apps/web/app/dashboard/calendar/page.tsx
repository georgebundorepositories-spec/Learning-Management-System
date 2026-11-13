import Link from "next/link";
import {redirect,notFound} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
export const dynamic="force-dynamic";
export default async function CalendarPage({searchParams}:{searchParams:Promise<{category?:string}>}){
 const supabase=await createClient(); const {data:{user}}=await supabase.auth.getUser();if(!user)redirect("/login");
 const {data:profile}=await supabase.from("users").select("school_id,role,is_active").eq("id",user.id).maybeSingle();if(!profile?.is_active)notFound();
 const {category}=await searchParams;
 const filter=category==="sports"?"sports":null;
 let query=supabase.from("calendar_events").select("id,title,category,start_at,end_at,location,description").eq("school_id",profile.school_id).eq("is_published",true).eq("approval_status","approved").is("deleted_at",null).order("start_at",{ascending:true}).limit(100);
 if(filter)query=query.eq("category",filter);
 const {data:events,error}=await query;
 return <main className="dashboard-page"><header className="topbar"><div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div><Link href="/dashboard">← Dashboard</Link></header><div className="dash-wrap"><p className="eyebrow">SCHOOL PROGRAMME</p><h1>{filter?"Sports & fixtures":"School calendar"}</h1><p className="subhead">Approved, published school events</p><nav className="workspace-tabs"><Link href="/dashboard/calendar">All events</Link><Link href="/dashboard/calendar?category=sports">Sports</Link></nav>{error?<p role="alert" className="notice-panel">Unable to load school events.</p>:!events?.length?<p className="notice-panel">No published events available.</p>:<div className="summary-cards">{events.map(event=><article className="summary-card" key={event.id}><span>{event.category}</span><strong>{event.title}</strong><span>{new Date(event.start_at).toLocaleString("en-ZW",{timeZone:"Africa/Harare",dateStyle:"medium",timeStyle:"short"})}</span>{event.location?<span>{event.location}</span>:null}{event.description?<p>{event.description}</p>:null}</article>)}</div>}</div></main>;
}
