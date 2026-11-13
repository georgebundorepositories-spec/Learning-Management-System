import {describe,expect,it} from "vitest";
import {categoryForModule,findWorkspaceModules,matchesWorkspaceModule,workspaceCategories}
 from "./workspace-discovery";
import {visibleModules} from "./portal-access";
describe("authorised workspace discovery",()=>{
 it("never creates or exposes modules outside role permissions",()=>{
  for(const role of ["admin","head","teacher","student","parent","bursar","clerk","unknown"]){
   const allowed=visibleModules(role);
   for(const cat of ["All",...workspaceCategories] as const){
    for(const term of ["","marks","learners","admin","family","attendance"]){
     const result=findWorkspaceModules(allowed,term,cat);
     expect(result.every(item=>allowed.some(x=>x.href===item.href))).toBe(true);
    }
   }
  }
  expect(findWorkspaceModules(visibleModules("parent"),"teacher")).toEqual([]);
  expect(findWorkspaceModules(visibleModules("unknown"),"")).toEqual([]);
 });
 it("supports title, description, multiple search words and categories",()=>{
  const modules=visibleModules("teacher");
  expect(findWorkspaceModules(modules,"assessment")).toEqual(
   modules.filter(x=>x.href==="/dashboard/assessments"));
  expect(findWorkspaceModules(modules,"class attendance").map(x=>x.href))
   .toContain("/dashboard/attendance");
  expect(findWorkspaceModules(modules,"notices","Teaching")).toEqual([]);
  expect(findWorkspaceModules(modules,"notices","Communication").length).toBeGreaterThan(0);
 });
 it("category mappings cover every real server-side dashboard module",()=>{
  for(const module of visibleModules("admin")){
   expect(workspaceCategories).toContain(categoryForModule(module.href));
   expect(matchesWorkspaceModule(module,"")).toBe(true);
  }
 });
});
