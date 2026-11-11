"use client";
import Link from "next/link";
import {useEffect,useMemo,useRef,useState} from "react";
import {categoryForModule,findWorkspaceModules,workspaceCategories,
 type WorkspaceCategory,type WorkspaceModule} from "@/lib/workspace-discovery";

export default function WorkspaceLauncher({modules}:{modules:readonly WorkspaceModule[]}){
 const [query,setQuery]=useState("");
 const [category,setCategory]=useState<"All"|WorkspaceCategory>("All");
 const input=useRef<HTMLInputElement>(null);
 const categories=useMemo(()=>workspaceCategories.filter(
  cat=>modules.some(module=>categoryForModule(module.href)===cat)),[modules]);
 const shown=useMemo(()=>findWorkspaceModules(modules,query,category),
  [modules,query,category]);
 useEffect(()=>{
  const onKey=(event:KeyboardEvent)=>{
   const target=event.target;
   const typing=target instanceof HTMLElement&&(
    target.matches("input,textarea,select")||target.isContentEditable
   );
   if(event.key==="/"&&!event.altKey&&!event.ctrlKey&&!event.metaKey&&!typing){
    event.preventDefault();
    input.current?.focus();
   }
   if(event.key==="Escape"&&document.activeElement===input.current){
    setQuery("");
    input.current?.blur();
   }
  };
  document.addEventListener("keydown",onKey);
  return ()=>document.removeEventListener("keydown",onKey);
 },[]);
 return <section className="workspace-launcher" aria-labelledby="workspace-title">
  <div className="workspace-launcher-header">
   <div><p className="eyebrow">YOUR TOOLS</p>
    <h2 id="workspace-title">Choose a workspace</h2>
    <p className="workspace-help">These links open your authorised school tools. Search or filter to find a task.</p>
   </div>
   <span className="workspace-count">{modules.length} available</span>
  </div>
  <div className="workspace-search">
   <label htmlFor="workspace-module-search">Find a tool</label>
   <div className="workspace-search-control">
    <input ref={input} id="workspace-module-search" type="search" value={query}
     placeholder="Search attendance, lessons, messages…" autoComplete="off"
     onChange={event=>setQuery(event.target.value)} maxLength={90}/>
    {query?<button type="button" onClick={()=>{setQuery("");input.current?.focus()}}
      aria-label="Clear tool search">Clear</button>:<span aria-hidden="true" className="workspace-search-key">/</span>}
   </div>
  </div>
  <div className="workspace-filter" role="group" aria-label="Filter tools by category">
   {(["All",...categories] as ("All"|WorkspaceCategory)[]).map(option=>
    <button key={option} type="button" aria-pressed={category===option}
     className={category===option?"workspace-filter-chip active":"workspace-filter-chip"}
     onClick={()=>setCategory(option)}>{option}</button>)}
  </div>
  <p className="workspace-results" role="status" aria-live="polite">
   {shown.length} {shown.length===1?"workspace":"workspaces"} shown
  </p>
  {shown.length?<div className="workspace-tool-grid">
    {shown.map(module=><Link key={module.href} href={module.href} prefetch={false}
     className="workspace-tool-card" aria-label={"Open "+module.title}>
      <span className="workspace-tool-category">{categoryForModule(module.href)}</span>
      <strong>{module.title}</strong>
      <span className="workspace-tool-description">{module.description}</span>
      <span className="workspace-tool-action">Open workspace <span aria-hidden="true">→</span></span>
     </Link>)}
   </div>:<div className="workspace-empty" role="status">
    <h3>No matching school tools</h3>
    <p>Try another subject or clear the search. Your school permissions have not changed.</p>
    <button type="button" className="outline-button" onClick={()=>{
      setQuery("");setCategory("All");input.current?.focus();
    }}>Show all available tools</button>
   </div>}
 </section>;
}
