import {describe,expect,it} from "vitest";
import {homeHub,homeHubs} from "./role-home";
import {canOpenDashboardModule} from "./portal-access";
describe("simple role-based school home",()=>{
 it("has no more than five plain-language choices per role",()=>{
  for(const role of ["student","teacher","parent","admin","head","bursar","clerk"]){
   const groups=homeHubs(role);
   expect(groups.length).toBeGreaterThan(0);
   expect(groups.length).toBeLessThanOrEqual(5);
   expect(new Set(groups.map(g=>g.slug)).size).toBe(groups.length);
   expect(groups.every(g=>g.links.length>0)).toBe(true);
  }
 });
 it("keeps student learning, results, news, school life and future visible",()=>{
  expect(homeHubs("student").map(g=>g.title)).toEqual([
   "My Learning","My Results","School News & Events","My School Life","My Future"]);
  const routes=homeHubs("student").flatMap(g=>g.links.map(l=>l.href));
  expect(routes).toContain("/dashboard/careers");
  expect(routes).not.toContain("/dashboard/planning");
  expect(routes).not.toContain("/dashboard/users");
 });
 it("prevents opening another designation's hub through direct URL",()=>{
  expect(homeHub("student","people")).toBeUndefined();
  expect(homeHub("parent","teaching")).toBeUndefined();
  expect(homeHub("teacher","my-child")).toBeUndefined();
  expect(homeHub("head","people")?.links.some(x=>x.href==="/dashboard/users")).toBe(false);
  expect(homeHubs("unexpected")).toEqual([]);
 });
 it("every real hub link is also authorised in the current role",()=>{
  for(const role of ["admin","head","teacher","student","parent","bursar","clerk"]){
   for(const group of homeHubs(role)){
    for(const {href} of group.links){
     if(href.startsWith("/dashboard/school-life")||href==="/dashboard/careers"){
      expect(role).toBe("student");
     }else{
      expect(canOpenDashboardModule(role,href)).toBe(true);
     }
    }
   }
  }
 });
});
