import {createClient} from "npm:@supabase/supabase-js@2.119.0";

type Role="teacher"|"student"|"parent";
type InviteInput={
 full_name?:unknown;email?:unknown;phone?:unknown;role?:unknown;
 class_id?:unknown;linked_student_id?:unknown;
};
const uuid=/^[a-f0-9]{8}-(?:[a-f0-9]{4}-){3}[a-f0-9]{12}$/i;
const emailFormat=/^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const phoneFormat=/^\+2637[1-8]\d{7}$/;
const roles:Role[]=["teacher","student","parent"];
function reply(status:number,message:string){
 return new Response(JSON.stringify({ok:status<300,message}),{
  status,headers:{"Content-Type":"application/json","Cache-Control":"no-store"}});
}
function value(v:unknown){return typeof v==="string"?v.trim():"";}

/**
 * Privileged identity creation must happen here, never in a browser or
 * Next.js client bundle. Require both a valid JWT and an active school admin
 * role from the database; never trust request-supplied school_id or JWT user metadata.
 */
Deno.serve(async(request:Request)=>{
 if(request.method!=="POST")return reply(405,"POST required");
 const authorization=request.headers.get("Authorization")||"";
 if(!authorization.startsWith("Bearer "))return reply(401,"Sign in required");
 const token=authorization.slice(7).trim();
 if(!token || token.length>8192)return reply(401,"Invalid session");
 const endpoint=Deno.env.get("SUPABASE_URL")||"";
 const secret=Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")||"";
 if(!endpoint || !secret)return reply(503,"Account invitations are not configured");
 const server=createClient(endpoint,secret,{
  auth:{autoRefreshToken:false,persistSession:false,detectSessionInUrl:false}
 });
 try {
  const {data:identity,error:authenticationError}=await server.auth.getUser(token);
  if(authenticationError||!identity.user)return reply(401,"Sign in required");
  const {data:caller,error:callerError}=await server.from("users")
   .select("role,school_id,is_active,deleted_at").eq("id",identity.user.id).maybeSingle();
  if(callerError||!caller||!caller.is_active||caller.deleted_at||
     caller.role!=="admin")return reply(403,"Only active school administrators can invite members");
  const length=Number(request.headers.get("content-length")||0);
  if(length>8192)return reply(413,"Invitation exceeds size limit");
  let input:InviteInput;
  try{input=await request.json() as InviteInput}
  catch{return reply(400,"Invalid invitation details")}
  const role=value(input.role) as Role;
  const fullName=value(input.full_name);
  const email=value(input.email).toLowerCase();
  const phone=value(input.phone);
  const classId=value(input.class_id);
  const linkedId=value(input.linked_student_id);
  if(!roles.includes(role)||fullName.length<3||fullName.length>120||
     email.length>254||!emailFormat.test(email)||!phoneFormat.test(phone)){
    return reply(400,"Provide a valid name, email, Zimbabwe mobile number and member role");
  }
  if((classId && !uuid.test(classId))||(linkedId && !uuid.test(linkedId)))
    return reply(400,"Invalid learner or class selection");
  if(role!=="student"&&classId||role!=="parent"&&linkedId)
    return reply(400,"Class enrolment and learner links must match the selected role");
  // Verify target relationships against the caller's school before Auth mail is sent.
  if(classId){
   const {data, error}=await server.from("classes").select("id")
    .eq("id",classId).eq("school_id",caller.school_id)
    .is("deleted_at",null).maybeSingle();
   if(error||!data)return reply(400,"Class not available to this school");
  }
  if(linkedId){
   const {data,error}=await server.from("users").select("id")
    .eq("id",linkedId).eq("school_id",caller.school_id).eq("role","student")
    .eq("is_active",true).is("deleted_at",null).maybeSingle();
   if(error||!data)return reply(400,"Linked learner must be active in this school");
  }
  const [existingEmail,existingPhone]=await Promise.all([
   server.from("users").select("id").eq("email",email).maybeSingle(),
   server.from("users").select("id").eq("phone",phone).maybeSingle()
  ]);
  if(existingEmail.error||existingPhone.error)return reply(503,"Could not check for duplicate accounts");
  if(existingEmail.data||existingPhone.data)
   return reply(409,"An account with this email or phone already exists");
  const {data:invited,error:invitationError}=await server.auth.admin.inviteUserByEmail(email);
  if(invitationError||!invited.user)return reply(400,"Invitation could not be sent; check the email address and school mail settings");
  const personId=invited.user.id;
  let created=false;
  try {
   const {error}=await server.from("users").insert({
    id:personId,school_id:caller.school_id,full_name:fullName,
    email,phone,role,is_active:true,preferred_lang:"en"
   });
   if(error)throw error;
   created=true;
   if(role==="student"){
    const {error:profileError}=await server.from("student_profiles")
     .insert({user_id:personId,class_id:classId||null});
    if(profileError)throw profileError;
   }else if(role==="parent"){
    const {error:profileError}=await server.from("parent_profiles")
      .insert({user_id:personId});
    if(profileError)throw profileError;
    if(linkedId){
     const {error:linkError}=await server.from("student_guardian_link")
      .insert({student_user_id:linkedId,guardian_user_id:personId,
       is_primary:false,can_pay_fees:false});
     if(linkError)throw linkError;
    }
   }else{
    const {error:profileError}=await server.from("staff_profiles")
     .insert({user_id:personId});
    if(profileError)throw profileError;
   }
   const {error:auditError}=await server.from("audit_log").insert({
     school_id:caller.school_id,actor_id:identity.user.id,
     action:"school_member_invited",entity:"users",entity_id:personId,
     diff:{role,has_class:!!classId,has_linked_learner:!!linkedId}
   });
   if(auditError)throw auditError;
  }catch(error){
   // Newly invited account has only just been created; remove it so an
   // incomplete school membership cannot be used as a real account.
   const age=Date.now()-Date.parse(invited.user.created_at||"");
   if(Number.isFinite(age)&&age>=0&&age<120000) {
     const {error:cleanup}=await server.auth.admin.deleteUser(personId);
     if(cleanup)return reply(500,"Invitation setup failed; administrator must review the incomplete account");
   }
   return reply(500,"Invitation setup could not be completed; no active member was confirmed");
  }
  if(!created)return reply(500,"Invitation not completed");
  return reply(200,"Invitation sent. The member must accept the email and set their own password.");
 }catch(_error){
  return reply(500,"Could not complete account invitation");
 }
});
