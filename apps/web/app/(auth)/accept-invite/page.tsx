"use client";

import {useEffect,useState,type FormEvent} from "react";
import Link from "next/link";
import {useRouter} from "next/navigation";
import {createClient} from "@/lib/supabase/client";
import {invitationPasswordError} from "@/lib/auth/invitation-password";

/**
 * A legitimate Supabase invitation confirms an email and returns an auth
 * session through an URL fragment, PKCE code or token_hash. Never accept
 * a caller-supplied user ID, school or role, and never render private records.
 */
export default function AcceptSchoolInvitation(){
 const [phase,setPhase]=useState<"checking"|"ready"|"error"|"saving"|"done">("checking");
 const [message,setMessage]=useState("");
 const [email,setEmail]=useState("");
 const [password,setPassword]=useState("");
 const [confirm,setConfirm]=useState("");
 const router=useRouter();

 useEffect(()=>{
  let active=true;
  const db=createClient();
  async function checkInvite(){
   try{
    const query=new URLSearchParams(window.location.search);
    const hash=new URLSearchParams(window.location.hash.replace(/^#/,""));
    // Consume any credential-bearing URL values immediately so the token
    // is not left in browser history when the form is displayed.
    const code=query.get("code");
    const tokenHash=query.get("token_hash");
    const type=query.get("type");
    const access=hash.get("access_token");
    const refresh=hash.get("refresh_token");
    const inviteError=hash.has("error")||query.has("error");
    if(code||tokenHash||access||inviteError)
      window.history.replaceState(null,"",window.location.pathname);
    if(inviteError)throw new Error("The invitation link is invalid or expired.");
    if(access&&refresh){
     const {error}=await db.auth.setSession({access_token:access,refresh_token:refresh});
     if(error)throw error;
    }else if(code){
     const {error}=await db.auth.exchangeCodeForSession(code);
     if(error)throw error;
    }else if(tokenHash&&type==="invite"){
     const {error}=await db.auth.verifyOtp({token_hash:tokenHash,type:"invite"});
     if(error)throw error;
    }else if(tokenHash){
     throw new Error("Unsupported invitation type");
    }
    const {data:{user},error}=await db.auth.getUser();
    if(error||!user)throw new Error("No valid school invitation session.");
    const {data:profile,error:profileError}=await db.from("users")
     .select("role,is_active,deleted_at").eq("id",user.id).maybeSingle();
    if(profileError||!profile?.is_active||profile.deleted_at||
      !["teacher","student","parent"].includes(profile.role))
     throw new Error("School membership has not been activated for this invitation.");
    if(active){
     setEmail(user.email??"your invited address");
     setPhase("ready");
    }
   }catch{
    if(active){
     setMessage("This invitation could not be verified. Ask the school administrator to check your account and resend the invitation.");
     setPhase("error");
    }
   }
  }
  void checkInvite();
  return ()=>{active=false};
 },[]);

 async function savePassword(event:FormEvent<HTMLFormElement>){
  event.preventDefault();
  if(phase!=="ready")return;
  const warning=invitationPasswordError(password);
  if(warning){setMessage(warning);return}
  if(password!==confirm){setMessage("The two passwords do not match.");return}
  setMessage("");
  setPhase("saving");
  try{
   const db=createClient();
   const {data:{user},error:authError}=await db.auth.getUser();
   if(authError||!user)throw new Error("Your invitation session expired");
   const {data:member}=await db.from("users")
    .select("id,role,is_active,deleted_at").eq("id",user.id).maybeSingle();
   if(!member?.is_active||member.deleted_at||
     !["teacher","student","parent"].includes(member.role))
    throw new Error("School invitation was revoked");
   const {error}=await db.auth.updateUser({password});
   if(error)throw error;
   setPassword("");setConfirm("");
   setPhase("done");
   router.replace("/dashboard");
   router.refresh();
  }catch{
   setPhase("ready");
   setMessage("Could not set your password. Confirm the invitation is still valid and try again.");
  }
 }
 return <main className="auth-page">
  <div className="brand-band" aria-hidden="true">
   <div className="brand-mark">Z</div><span>ZimSchool LMS</span>
  </div>
  <section className="auth-panel" aria-labelledby="invite-title">
   <p className="eyebrow">SCHOOL-APPROVED ACCOUNT</p>
   <h1 id="invite-title">Accept your school invitation</h1>
   {phase==="checking"?<p role="status">Verifying your emailed invitation…</p>:
    phase==="error"?<div>
     <p role="alert" className="form-error">{message}</p>
     <p><Link href="/login">Return to sign in</Link></p>
    </div>:
    phase==="done"?<p role="status">Password saved. Opening your school portal…</p>:
    <div>
     <p className="subhead">Finish setting up the invited account for <strong>{email}</strong>. The school determines which teacher, learner or parent features you can access.</p>
     <form onSubmit={savePassword}>
      <label htmlFor="new-password">Choose a new password</label>
      <input id="new-password" type="password" autoComplete="new-password"
       minLength={12} maxLength={128} required value={password}
       onChange={event=>setPassword(event.target.value)}/>
      <label htmlFor="confirm-password">Confirm your password</label>
      <input id="confirm-password" type="password" autoComplete="new-password"
       minLength={12} maxLength={128} required value={confirm}
       onChange={event=>setConfirm(event.target.value)}/>
      <p className="privacy-note">Use at least 12 characters including uppercase and lowercase letters and a number. Do not share your password with your school administrator.</p>
      {message?<p role="alert" className="form-error">{message}</p>:null}
      <button type="submit" disabled={phase==="saving"}>
       {phase==="saving"?"Saving your password…":"Set password and open my portal"}
      </button>
     </form>
    </div>}
   <p className="privacy-note">Only users invited by a school administrator can access a school account. This page does not allow public registration.</p>
  </section>
 </main>;
}
