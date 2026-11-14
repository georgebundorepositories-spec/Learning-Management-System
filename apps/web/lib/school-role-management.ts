/** The school's LMS designation is public.users.role, never auth.users.role or
 * client-editable JWT user_metadata. Admin/head provisioning is separate.
 */
export const assignableSchoolRoles=[
 {value:"teacher",label:"Teacher"},
 {value:"student",label:"Learner"},
 {value:"parent",label:"Parent / guardian"},
 {value:"bursar",label:"Bursar / school finance"}
] as const;
export type AssignableSchoolRole=(typeof assignableSchoolRoles)[number]["value"];
export function isAssignableSchoolRole(value:string):value is AssignableSchoolRole{
 return assignableSchoolRoles.some(option=>option.value===value);
}
export function canSchoolAdminChangeRole(
 actorRole:string,oldRole:string,nextRole:string,isSelf:boolean
):boolean{
 return actorRole==="admin"&&!isSelf &&
  !["admin","head"].includes(oldRole) &&
  oldRole!==nextRole && isAssignableSchoolRole(nextRole);
}
export function schoolRoleLabel(role:string):string{
 return assignableSchoolRoles.find(option=>option.value===role)?.label ??
  (role==="admin"?"Administrator":role==="head"?"School head":role);
}
