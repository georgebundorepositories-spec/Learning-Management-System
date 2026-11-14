import {describe,expect,it} from "vitest";
import {assignableSchoolRoles,canSchoolAdminChangeRole,isAssignableSchoolRole,schoolRoleLabel}
 from "./school-role-management";

describe("LMS role assignment",()=>{
 it("only shows supported ordinary school designations",()=>{
  expect(assignableSchoolRoles.map(r=>r.value)).toEqual(["teacher","student","parent","bursar"]);
  expect(isAssignableSchoolRole("head")).toBe(false);
  expect(isAssignableSchoolRole("admin")).toBe(false);
  expect(isAssignableSchoolRole("master_admin")).toBe(false);
  expect(schoolRoleLabel("student")).toBe("Learner");
 });
 it("school administrator can change an eligible member's designation",()=>{
  expect(canSchoolAdminChangeRole("admin","student","teacher",false)).toBe(true);
  expect(canSchoolAdminChangeRole("admin","teacher","parent",false)).toBe(true);
  expect(canSchoolAdminChangeRole("admin","parent","parent",false)).toBe(false);
 });
 it("blocks self-change, head elevation, unrecognised roles, other actors",()=>{
  for(const actor of ["head","teacher","student","parent","bursar",""]){
   expect(canSchoolAdminChangeRole(actor,"student","teacher",false)).toBe(false);
  }
  expect(canSchoolAdminChangeRole("admin","admin","teacher",false)).toBe(false);
  expect(canSchoolAdminChangeRole("admin","head","teacher",false)).toBe(false);
  expect(canSchoolAdminChangeRole("admin","teacher","admin",false)).toBe(false);
  expect(canSchoolAdminChangeRole("admin","teacher","head",false)).toBe(false);
  expect(canSchoolAdminChangeRole("admin","student","teacher",true)).toBe(false);
 });
});
