"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { createClient } from "@/lib/supabase/client";
import { languages, messages, type Language } from "@/lib/i18n";
import { parseLoginIdentifier } from "@/lib/validation";

export default function LoginPage() {
  const [language, setLanguage] = useState<Language>("en");
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [pending, setPending] = useState(false);
  const router = useRouter();
  const words = messages[language];

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending) return;
    setPending(true);
    setErrorMessage("");
    try {
      const identifierValue = parseLoginIdentifier(identifier);
      const supabase = createClient();
      const { error } = await supabase.auth.signInWithPassword(
        identifierValue.type === "phone"
          ? { phone: identifierValue.value, password }
          : { email: identifierValue.value, password }
      );
      if (error) throw error;
      router.replace("/dashboard");
      router.refresh();
    } catch {
      setErrorMessage(words.invalid);
    } finally {
      setPending(false);
    }
  }

  return <main className="auth-page">
    <div className="brand-band" aria-hidden="true"><div className="brand-mark">Z</div><span>ZimSchool LMS</span></div>
    <section className="auth-panel" aria-labelledby="login-title">
      <div className="lang-control"><label htmlFor="language">{words.language}</label>
        <select id="language" value={language} onChange={e => setLanguage(e.target.value as Language)}>
          {languages.map(lang => <option key={lang} value={lang}>{lang === "en" ? "English" : lang === "sn" ? "ChiShona" : "isiNdebele"}</option>)}
        </select>
      </div>
      <div className="eyebrow">ZIMSCHOOL • SECURE ACCESS</div>
      <h1 id="login-title">{words.welcome}</h1><p className="subhead">{words.subtitle}</p>
      <form onSubmit={onSubmit}>
        <label htmlFor="identifier">{words.identifier}</label>
        <input id="identifier" name="identifier" autoComplete="username" inputMode="text"
          required value={identifier} onChange={e => setIdentifier(e.target.value)} placeholder="077 123 4567" />
        <label htmlFor="password">{words.password}</label>
        <input id="password" type="password" name="password" autoComplete="current-password"
          required minLength={6} value={password} onChange={e => setPassword(e.target.value)} />
        {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}
        <button type="submit" disabled={pending}>{pending ? words.signingIn : words.signIn}</button>
      </form>
      <div className="auth-note"><p>{words.forgot}</p><p><Link href="/register">{words.register}</Link></p></div>
      <p className="privacy-note">{words.privacy}</p>
    </section>
  </main>;
}
