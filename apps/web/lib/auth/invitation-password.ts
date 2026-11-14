/** A school member needs a strong unique password, but never receives
 * a password chosen or stored by a school administrator.
 */
export function invitationPasswordError(password:string):string|null{
 if(password.length<12||password.length>128)
   return "Choose a password between 12 and 128 characters.";
 if(!/[a-z]/.test(password)||!/[A-Z]/.test(password)||!/[0-9]/.test(password))
   return "Include uppercase and lowercase letters plus a number.";
 return null;
}
