import Link from "next/link";
export default function DashboardNotFound(){
 return <main className="simple-page"><section className="simple-card">
 <p className="eyebrow">WORKSPACE UNAVAILABLE</p><h1>This page is not available</h1>
 <p>The address may be incorrect, or your school account may not have permission.</p>
 <Link href="/dashboard" className="outline-button">Back to dashboard</Link>
 </section></main>;
}
