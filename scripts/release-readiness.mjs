#!/usr/bin/env node
import {appendFileSync,mkdirSync,writeFileSync} from "node:fs";
import {inspectEnvironment,evidenceRequirements,fullFeatureRequirements} from "./release-readiness-core.mjs";

const env={ZIMSCHOOL_WEB_ORIGIN:process.env.ZIMSCHOOL_WEB_ORIGIN};
for(const k of ["VERCEL_TOKEN","VERCEL_ORG_ID","VERCEL_PROJECT_ID",
 "ANDROID_KEYSTORE_BASE64","ANDROID_STORE_PASSWORD","ANDROID_KEY_PASSWORD",
 "ANDROID_KEY_ALIAS"]){
 env[k]=process.env["GATE_HAS_"+k]==="true"?"configured":"absent";
}
for(const [id] of [...evidenceRequirements,...fullFeatureRequirements])
 env["ZIMSCHOOL_EVIDENCE_"+id]=process.env["ZIMSCHOOL_EVIDENCE_"+id];

const report=inspectEnvironment(env,{full:process.env.RELEASE_TARGET!=="limited-pilot"});
const publicTests=[];
if(report.origin){
 for(const [route,expected] of [
  ["/demo",/fictional demonstration/i],
  ["/login",/sign in|log in|login/i],
  ["/accept-invite",/Accept your school invitation/i]]){
  try{
   const url=report.origin+route;
   const response=await fetch(url,{signal:AbortSignal.timeout(18000),
    headers:{"Cache-Control":"no-cache"},redirect:"follow"});
   const body=(await response.text()).slice(0,300000);
   publicTests.push({route,passed:response.ok&&expected.test(body),
    httpStatus:response.status});
  }catch{
   publicTests.push({route,passed:false,httpStatus:null});
  }
 }
}
const passed=report.technicalGatesPresent&&publicTests.length===3&&
 publicTests.every(t=>t.passed);
const output={
 candidate:process.env.GITHUB_SHA||"local-uncommitted",
 target:report.target,checked_at:new Date().toISOString(),
 state:passed?"EVIDENCE_SUBMITTED_REQUIRES_HUMAN_APPROVAL":"BLOCKED",
 checks:report.checks,public_routes:publicTests,
 // A link with a checkbox is NOT itself proof. The school owner must audit.
 caveat:"This audit validates prerequisite fields and public pages only. It does not certify legal consent, real user onboarding, physical QA, provider settlements, independent security review or production go-live."
};
mkdirSync("release-evidence",{recursive:true});
writeFileSync("release-evidence/readiness.json",JSON.stringify(output,null,2)+"\n");
let summary="## ZimSchool final release gate — "+output.state+"\n\n";
summary+="**Target:** "+report.target+" · **Revision:** "+output.candidate+"\n\n";
summary+="| Gate | Audit | Required proof |\n|---|---|---|\n";
for(const c of report.checks)
 summary+="| "+c.id+" | "+(c.passed?"Evidence/setting present":"BLOCKED")+
  " | "+c.message+" |\n";
for(const t of publicTests)
 summary+="\n- Public "+t.route+": "+(t.passed?"reachable":"FAILED")+" (HTTP "+(t.httpStatus??"n/a")+")";
summary+="\n\n**Important:** Evidence links need independent human approval. An automated check is not a production-release authorisation.\n";
if(process.env.GITHUB_STEP_SUMMARY)appendFileSync(process.env.GITHUB_STEP_SUMMARY,summary);
console.log(summary);
if(!passed){
 console.error("Release blocked: "+report.blocked.join(", ")+
  (publicTests.some(x=>!x.passed)?" or invalid public deployment":""));
 process.exitCode=1;
}
