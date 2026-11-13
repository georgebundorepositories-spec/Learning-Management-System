"use client";
import Link from "next/link";
export default function DashboardError({reset}:{error:Error & {digest?:string};reset:()=>void}){
 return <main className="simple-page"><section className="simple-card" role="alert">
 <p className="eyebrow">SCHOOL WORKSPACE</p><h1>We couldn’t load this page</h1>
 <p>Check your connection and try again. If the problem continues, contact your school administrator.</p>
 <div className="workspace-tabs"><button className="outline-button" onClick={reset}>Try again</button>
 <Link href="/dashboard">Back to dashboard</Link></div>
 </section></main>;
}
