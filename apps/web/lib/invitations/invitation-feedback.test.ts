import {describe,expect,it} from "vitest";
import {invitationFailureMessage} from "./invitation-feedback";

describe("school invitation error feedback",()=>{
 it("explains a duplicate member instead of blaming email settings",()=>{
   const text=invitationFailureMessage(409,{ok:false,message:"An account with this email or phone already exists"});
   expect(text).toContain("already registered");
   expect(text).toContain("No invitation was sent");
   expect(text).not.toContain("email settings");
 });
 it("explains verified account and unverified account assignment separately",()=>{
   expect(invitationFailureMessage(409,{message:"This email already has an unverified login; confirm the email before joining a school"})).toContain("must be verified");
   expect(invitationFailureMessage(409,{message:"This login already belongs to a school; contact the school administrator"})).toContain("already linked");
   expect(invitationFailureMessage(503,{message:"Existing account lookup is unavailable"})).toContain("could not verify");
 });
 it("directs session and role failures to the correct fix",()=>{
   expect(invitationFailureMessage(401,{})).toContain("Sign in again");
   expect(invitationFailureMessage(403,{})).toContain("not authorised");
 });
 it("preserves only known safe validation guidance",()=>{
   expect(invitationFailureMessage(400,{message:"Class not available to this school"})).toContain("Class not available");
   expect(invitationFailureMessage(400,{message:"secret internal stack: example@example.com"})).not.toContain("example@example.com");
 });
 it("distinguishes failed email delivery from a duplicate",()=>{
   const text=invitationFailureMessage(400,{message:"Invitation could not be sent; check the email address and school mail settings"});
   expect(text).toContain("SMTP");
   expect(text).not.toContain("already registered");
 });
 it("identifies missing approved HTTPS invite redirect configuration",()=>{
   expect(invitationFailureMessage(503,{message:"School invitations require an approved HTTPS web URL. Ask the deployment administrator to configure it."})).toContain("HTTPS");
 });
 it("explains other server, rate-limit and deployment failures safely",()=>{
   expect(invitationFailureMessage(429,null)).toContain("Too many");
   expect(invitationFailureMessage(500,{})).toContain("function logs");
   expect(invitationFailureMessage(404,{})).toContain("not deployed");
   expect(invitationFailureMessage(520,{message:"internal-secret"})).not.toContain("internal-secret");
 });
});
