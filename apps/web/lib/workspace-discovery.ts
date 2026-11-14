/** Fast, deterministic client-side discovery of server-authorised modules.
 * Filtering never expands a role's permissions: it only narrows modules
 * already selected by the authenticated dashboard on the server.
 */
export type WorkspaceCategory="Learning"|"People"|"Teaching"|"Communication"|"Finance"|"Management";
export type WorkspaceModule={href:string;title:string;description:string};
export const workspaceCategories:readonly WorkspaceCategory[]=[
 "Learning","People","Teaching","Communication","Finance","Management"
];
const pathCategories:Record<string,WorkspaceCategory>={
 planning:"Teaching",users:"People",coursework:"Teaching",progress:"Learning",
 admin:"Management",reports:"Management",notices:"Communication",
 fees:"Finance",assessments:"Teaching","my-learning":"Learning",
 calendar:"Communication",learners:"People",teachers:"People",parents:"People",
 attendance:"Teaching",learning:"Learning",curriculum:"Learning"
};
export function categoryForModule(href:string):WorkspaceCategory{
 const slug=href.split("/").filter(Boolean).pop()??"";
 return pathCategories[slug]??"Management";
}
export function matchesWorkspaceModule(module:WorkspaceModule,query:string):boolean{
 const words=query.normalize("NFKD").trim().toLocaleLowerCase("en").split(/\s+/).filter(Boolean);
 if(!words.length)return true;
 const haystack=[module.title,module.description,categoryForModule(module.href)]
  .join(" ").normalize("NFKD").toLocaleLowerCase("en");
 return words.every(word=>haystack.includes(word));
}
export function findWorkspaceModules<T extends WorkspaceModule>(
 modules:readonly T[],query:string,category:"All"|WorkspaceCategory="All"
):T[]{
 return modules.filter(module=>
  (category==="All"||categoryForModule(module.href)===category)&&
  matchesWorkspaceModule(module,query)
 );
}
