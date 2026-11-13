import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import {homeHub} from "@/lib/role-home";
export const dynamic="force-dynamic";
export default async function RoleHubPage({params}:{params:Promise<{slug:string}>}){
 const {slug}=await params;
 const db=await createClient();
 const {data:{user}}=await db.auth.getUser();
 if(!user)redirect("/login");
 const {data:actor}=await db.from("users")
  .select("role,is_active,deleted_at").eq("id",user.id).maybeSingle();
 if(!actor?.is_active||actor.deleted_at)notFound();
 const chosen=homeHub(actor.role,slug);
 if(!chosen)notFound();
 return <main className="dashboard-page">
  <header className="topbar"><Link href="/dashboard" className="outline-button">← Home</Link></header>
  <div className="dash-wrap simple-dashboard">
   <p className="eyebrow">SCHOOL WORKSPACE</p>
   <h1>{chosen.title}</h1>
   <p className="subhead">{chosen.description}</p>
   <div className="simple-option-list">
    {chosen.links.map(link=><Link key={link.href} href={link.href} prefetch={false}
     className="simple-home-link">
     <span><strong>{link.title}</strong><small>{link.detail}</small></span>
     <span aria-hidden="true">→</span>
    </Link>)}
   </div>
  </div>
 </main>;
}
