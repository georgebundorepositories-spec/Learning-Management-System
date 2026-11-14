/**
 * Non-secret final-launch audit helpers. A passing CI build is NOT a launch.
 * Evidence links are submitted for HUMAN verification; they do not prove
 * consent, payment settlement, signed builds or cross-device tests happened.
 */
export const evidenceRequirements = Object.freeze([
  ["AUTH_PASSWORD_PROTECTION","Leaked-password protection enabled in hosted Supabase Auth"],
  ["AUTH_INVITE_DELIVERY","Verified email invitation, one-time acceptance and self-set password"],
  ["SCHOOL_ROLES","Authorised administrator, teacher, learner and guardian account acceptance"],
  ["CHILD_DATA_CONSENT","Approved school privacy/guardian consent and retention assessment"],
  ["BACKUP_RESTORE","Successful full backup/restore recovery drill"],
  ["OFFLINE_72H","Two-device 72-hour offline/reconnect and revocation tests"],
  ["PHYSICAL_ANDROID","Physical Android 8+ device acceptance and 2G performance test"],
  ["SIGNED_ANDROID","Persistent owner-signed Android APK and verified signing fingerprint"],
  ["ACCESSIBILITY","Web and Android accessibility, Shona/Ndebele/English user review"],
  ["OWNER_APPROVAL","Documented owner review of the final release candidate"]
]);

export const fullFeatureRequirements = Object.freeze([
  ["PAYMENT_PROVIDER","Live provider webhook signatures, idempotent ledger, refund/receipt tests"],
  ["MESSAGING_PROVIDER","SMS/push consent, opt-out, delivery and retry evidence"],
  ["USSD_PROVIDER","Approved USSD provider test, security and reconciliation evidence"]
]);

export function parseOrigin(raw){
 if(!raw||typeof raw!=="string")return null;
 try{
  const url=new URL(raw);
  if(url.protocol!=="https:"||url.username||url.password||url.search||url.hash||
     url.pathname!=="/"||url.port||url.hostname==="localhost"||
     url.hostname.endsWith(".localhost")||
     /^(?:127\.|10\.|192\.168\.|169\.254\.|0\.)/.test(url.hostname)||
     url.hostname==="::1"||url.hostname.startsWith("[")||
     url.hostname.includes(".."))return null;
  return url.origin;
 }catch{return null}
}

export function looksLikeEvidence(raw){
 if(typeof raw!=="string"||raw.length>2048)return false;
 try{
  const url=new URL(raw);
  return url.protocol==="https:"&&!url.username&&!url.password&&
   !url.hash&&url.hostname!=="localhost"&&
   url.pathname!=="/"&&url.pathname.length>4;
 }catch{return false}
}

export function inspectEnvironment(env,{full=true}={}){
 const checks=[];
 const push=(id,ok,message)=>checks.push({id,passed:Boolean(ok),message});
 const origin=parseOrigin(env.ZIMSCHOOL_WEB_ORIGIN);
 push("WEB_ORIGIN",origin!==null,
  "A real, publicly accessible HTTPS web origin must serve /demo, /login and /accept-invite.");
 push("VERCEL_CONFIGURATION",
  ["VERCEL_TOKEN","VERCEL_ORG_ID","VERCEL_PROJECT_ID"].every(k=>env[k]==="configured"),
  "Vercel deployment token, organisation ID and project ID are required (values stay secret).");
 push("OWNER_SIGNING_CONFIGURATION",
  ["ANDROID_KEYSTORE_BASE64","ANDROID_STORE_PASSWORD","ANDROID_KEY_PASSWORD","ANDROID_KEY_ALIAS"]
   .every(k=>env[k]==="configured"),
  "Persistent Android owner keystore and signing credentials are required (not CI debug signing).");
 for(const [id,message] of evidenceRequirements){
  push("EVIDENCE_"+id,looksLikeEvidence(env["ZIMSCHOOL_EVIDENCE_"+id]),
   message+"; provide an HTTPS evidence link for independent human review.");
 }
 if(full){
  for(const [id,message] of fullFeatureRequirements){
   push("EVIDENCE_"+id,looksLikeEvidence(env["ZIMSCHOOL_EVIDENCE_"+id]),
    message+"; full launch cannot advertise a provider as working without tests.");
  }
 }
 return {target:full?"full":"limited-pilot",origin,checks,
  blocked:checks.filter(c=>!c.passed).map(c=>c.id),
  technicalGatesPresent:checks.every(c=>c.passed)};
}
