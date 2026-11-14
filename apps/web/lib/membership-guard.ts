/** A separate confirmation step is mandatory for changes which expose or
 * revoke confidential learner records. UI flags alone are not authorisation;
 * the school administrator role and RLS are checked separately.
 */
export function confirmedGuardianAction(data:FormData,type:"link"|"revoke"):boolean {
 const field=type==="link"?"guardian_verified":"revocation_confirmed";
 return data.get(field)==="yes";
}
