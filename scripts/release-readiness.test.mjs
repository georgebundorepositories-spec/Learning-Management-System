import {test} from "node:test";
import assert from "node:assert/strict";
import {parseOrigin,looksLikeEvidence,inspectEnvironment,
 evidenceRequirements,fullFeatureRequirements} from "./release-readiness-core.mjs";

test("rejects untrusted, local or credential-bearing invitation origins",()=>{
 for(const url of ["","http://school.example","https://localhost",
  "https://127.0.0.1","https://site.example/some/path",
  "https://user:pass@site.example","https://site.example/?next=evil",
  "https://site.example/#fragment","https://site.example:8080"]) {
  assert.equal(parseOrigin(url),null,url);
 }
 assert.equal(parseOrigin("https://school.example/"),"https://school.example");
});
test("evidence requires an HTTPS document, not an arbitrary yes flag",()=>{
 for(const value of [undefined,null,"yes","true","https://some-site.example",
  "javascript:alert(1)","http://not-secure.test/evidence",
  "https://user:password@site.example/fake"])
  assert.equal(looksLikeEvidence(value),false,String(value));
 assert.equal(looksLikeEvidence("https://github.com/org/repo/issues/18"),true);
});
test("new repository always fails closed without external release prerequisites",()=>{
 const {technicalGatesPresent,blocked,target}=inspectEnvironment({}, {full:true});
 assert.equal(target,"full");
 assert.equal(technicalGatesPresent,false);
 assert.ok(blocked.includes("WEB_ORIGIN"));
 assert.ok(blocked.includes("VERCEL_CONFIGURATION"));
 assert.ok(blocked.includes("OWNER_SIGNING_CONFIGURATION"));
 assert.ok(blocked.includes("EVIDENCE_CHILD_DATA_CONSENT"));
 assert.ok(blocked.includes("EVIDENCE_PAYMENT_PROVIDER"));
});
test("limited pilot never accidentally claims payment, SMS or USSD certification",()=>{
 const report=inspectEnvironment({}, {full:false});
 assert.equal(report.target,"limited-pilot");
 assert.equal(report.blocked.some(x=>x.includes("PROVIDER")),false);
 assert.ok(report.blocked.includes("EVIDENCE_SCHOOL_ROLES"));
});
test("complete evidence remains a human-review checklist, not an automated launch",()=>{
 const env={ZIMSCHOOL_WEB_ORIGIN:"https://school.example",
  VERCEL_TOKEN:"configured",VERCEL_ORG_ID:"configured",
  VERCEL_PROJECT_ID:"configured",ANDROID_KEYSTORE_BASE64:"configured",
  ANDROID_STORE_PASSWORD:"configured",ANDROID_KEY_PASSWORD:"configured",
  ANDROID_KEY_ALIAS:"configured"};
 for(const [id] of [...evidenceRequirements,...fullFeatureRequirements])
  env["ZIMSCHOOL_EVIDENCE_"+id]="https://github.com/org/repo/issues/"+id;
 const report=inspectEnvironment(env,{full:true});
 assert.equal(report.technicalGatesPresent,true);
 assert.deepEqual(report.blocked,[]);
 assert.equal(report.origin,"https://school.example");
});
