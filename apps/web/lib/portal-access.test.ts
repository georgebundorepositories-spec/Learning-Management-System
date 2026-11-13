import {it,expect,describe} from "vitest";
import {visibleModules,canOpenDashboardModule} from "./portal-access";
const links=(role:string)=>visibleModules(role).map(m=>m.href);
describe("dashboard role access",()=>{
 it("teacher receives lesson planning but no admin/parent directory",()=>{
  expect(links("teacher")).toContain("/dashboard/planning");
  expect(links("teacher")).not.toContain("/dashboard/users");
  expect(links("teacher")).not.toContain("/dashboard/parents");
 });
 it("student sees learning and private coursework, never planning",()=>{
  for(const href of ["/dashboard/curriculum","/dashboard/learning","/dashboard/progress","/dashboard/my-learning"])
   expect(links("student")).toContain(href);
  for(const href of ["/dashboard/planning","/dashboard/attendance","/dashboard/users","/dashboard/teachers"])
   expect(links("student")).not.toContain(href);
 });
 it("parent sees family progress but no staff controls",()=>{
  expect(links("parent")).toContain("/dashboard/my-learning");
  expect(links("parent")).not.toContain("/dashboard/planning");
  expect(links("parent")).not.toContain("/dashboard/learners");
 });
 it("only admin receives invite portal and unknown roles see nothing",()=>{
  expect(canOpenDashboardModule("admin","/dashboard/users")).toBe(true);
  expect(canOpenDashboardModule("teacher","/dashboard/users")).toBe(false);
  expect(links("unknown")).toEqual([]);
 });
});
