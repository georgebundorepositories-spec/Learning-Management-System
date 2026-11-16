/**
 * Only administrators can reach the school invitation action. Translate the
 * trusted function's HTTP status and known safe replies into useful feedback.
 * Never echo an arbitrary server response, which might contain implementation
 * details, email addresses or personal records.
 */
export function invitationFailureMessage(status:number,body:unknown):string {
  const serverMessage=body && typeof body==="object" && "message" in body
    && typeof body.message==="string" ? body.message : "";

  if(status===409){
    return "This email address or mobile number is already registered. No invitation was sent. Review the existing account under School members; use a different address and number only when inviting a different person.";
  }
  if(status===401)return "Your school session has expired. Sign in again before inviting a member.";
  if(status===403)return "Your account is not authorised to invite members. An active school administrator must send the invitation.";
  if(status===404)return "The school invitation service is not deployed. Ask the system administrator to check the Supabase Edge Function.";
  if(status===429)return "Too many invitation attempts. Please try again later, or contact your system administrator.";
  if(status===503){
    if(serverMessage.includes("approved HTTPS web URL")){
      return "Invitations are disabled until the administrator configures the approved HTTPS password-setup website URL.";
    }
    if(serverMessage==="Could not check for duplicate accounts"){
      return "Account uniqueness could not be checked. Try again once the school database is available.";
    }
    return "The school invitation service is temporarily unavailable or not fully configured. Ask the system administrator to check its settings.";
  }
  if(status===400){
    const safelyDisplayable=new Set([
      "Provide a valid name, email, Zimbabwe mobile number and member role",
      "Invalid learner or class selection",
      "Class enrolment and learner links must match the selected role",
      "Class not available to this school",
      "Linked learner must be active in this school",
      "Invalid invitation details"
    ]);
    if(safelyDisplayable.has(serverMessage))return serverMessage+".";
    if(serverMessage.includes("Invitation could not be sent")){
      return "The email invitation could not be delivered. Check that the address is valid and ask the system administrator to check Supabase Auth email delivery and custom SMTP settings.";
    }
    return "The invitation details were rejected. Check the member's name, email, mobile number and school selections.";
  }
  if(status===500)return "The invitation service could not finish setting up the account. Ask the system administrator to check the Supabase function logs before retrying.";
  return "The school invitation could not be completed. Try again or ask the system administrator to investigate.";
}
