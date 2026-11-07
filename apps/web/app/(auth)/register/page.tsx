import Link from "next/link";
export default function Registration() {
  return <main className="simple-page"><article className="simple-card">
    <div className="eyebrow">ACCOUNT PROTECTION</div>
    <h1>Registration by school invitation</h1>
    <p>For student safety, ZimSchool accounts must be provisioned by an authorised school administrator. Public self-registration is disabled.</p>
    <p>Ask your school to send an invitation or provision your account.</p>
    <Link href="/login">Return to sign in</Link>
  </article></main>;
}
