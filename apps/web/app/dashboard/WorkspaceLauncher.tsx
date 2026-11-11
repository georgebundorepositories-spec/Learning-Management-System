"use client";
import Link from "next/link";
import {useState} from "react";
import type {RoleHub} from "@/lib/role-home";

/** Progressive disclosure: 4–5 clear choices, not the underlying table/menu list. */
export default function WorkspaceLauncher({modules}:{modules:readonly RoleHub[]}){
 const [expanded,setExpanded]=useState<string|null>(null);
 return <section className="simple-home" aria-labelledby="home-tasks">
  <h2 id="home-tasks">What would you like to do?</h2>
  <p className="simple-home-intro">Choose one option. You will always have a way back home.</p>
  <div className="simple-home-grid">
   {modules.map((group,index)=><article key={group.slug}
     className="simple-home-card">
    <div className="simple-home-number" aria-hidden="true">{index+1}</div>
    <div className="simple-home-copy">
     <h3>{group.title}</h3>
     <p>{group.description}</p>
     <button type="button" className="simple-home-open"
      aria-controls={"hub-"+group.slug}
      aria-expanded={expanded===group.slug}
      onClick={()=>setExpanded(current=>current===group.slug?null:group.slug)}>
      {expanded===group.slug?"Close options":"Open"} <span aria-hidden="true">{expanded===group.slug?"−":"→"}</span>
     </button>
    </div>
    {expanded===group.slug?<div id={"hub-"+group.slug}
      className="simple-home-links">
      {group.links.map(link=><Link href={link.href} key={link.href} prefetch={false}
       className="simple-home-link">
       <span><strong>{link.title}</strong><small>{link.detail}</small></span>
       <span aria-hidden="true">→</span>
      </Link>)}
     </div>:null}
   </article>)}
  </div>
 </section>;
}
