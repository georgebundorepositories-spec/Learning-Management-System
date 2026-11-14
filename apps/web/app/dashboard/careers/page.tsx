import Link from "next/link";
import {notFound,redirect} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
export const dynamic="force-dynamic";
const pathways=[
 {interest:"I like solving problems",areas:"Science, technology, engineering, trades",
  try:"Try a science project or repair activity. Ask a teacher which subjects build these skills."},
 {interest:"I like caring for people",areas:"Healthcare, teaching, community services",
  try:"Volunteer safely through a school-approved activity and practise explaining ideas."},
 {interest:"I like business and numbers",areas:"Accounting, entrepreneurship, agriculture, commerce",
  try:"Create a simple budget for a school project and learn how to check costs."},
 {interest:"I like making things",areas:"Arts, design, construction and practical trades",
  try:"Build a small project or portfolio and ask for constructive feedback."}
];
export default async function CareerGuidance(){
 const db=await createClient();const {data:{user}}=await db.auth.getUser();
 if(!user)redirect("/login");
 const {data:actor}=await db.from("users")
  .select("role,is_active,deleted_at").eq("id",user.id).maybeSingle();
 if(!actor?.is_active||actor.deleted_at||actor.role!=="student")notFound();
 return <main className="dashboard-page">
  <header className="topbar"><Link href="/dashboard" className="outline-button">← Home</Link></header>
  <div className="dash-wrap simple-dashboard">
   <p className="eyebrow">LEARNER PORTAL</p><h1>My Future</h1>
   <p className="subhead">You do not need to choose a career today. Start by finding what you enjoy and what you want to learn.</p>
   <h2>What interests you?</h2>
   <div className="simple-option-list">{pathways.map(path=><article className="simple-life-event" key={path.interest}>
    <h3>{path.interest}</h3><p><strong>Possible directions:</strong> {path.areas}</p>
    <p><strong>One thing to try:</strong> {path.try}</p></article>)}</div>
   <section className="simple-life-section">
    <h2>Three questions for your teacher</h2>
    <ol><li>Which subjects will help me explore my interests?</li>
     <li>Which clubs or school activities can help me practise?</li>
     <li>What further training or work experience would this field need?</li></ol>
    <p>Career options and entry requirements change. A qualified teacher or guidance counsellor can help you check current requirements.</p>
   </section>
  </div>
 </main>;
}
