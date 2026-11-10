"use client";
import {useState} from "react";

export default function Quiz({
 question,choices,correctIndex,feedback
}:{question:string;choices:string[];correctIndex:number;feedback:string}){
 const [selected,setSelected]=useState<number|null>(null);
 const [checked,setChecked]=useState(false);
 return <section className="summary-card" aria-label="Self-check question">
   <h2>Check your understanding</h2>
   <p>{question}</p>
   <fieldset style={{border:0,padding:0,margin:0}}>
    <legend className="privacy-note">Choose one answer, then check it.</legend>
    {choices.map((choice,i)=><label key={choice} style={{display:"flex",alignItems:"center",gap:10,padding:"10px 0"}}>
      <input type="radio" name="quiz" value={i} checked={selected===i}
        onChange={()=>{setSelected(i);setChecked(false);}}/>
      <span>{choice}</span>
     </label>)}
   </fieldset>
   <button type="button" className="outline-button" disabled={selected===null} onClick={()=>setChecked(true)}>Check answer</button>
   {checked?<p role="status" className="notice-panel">
     <strong>{selected===correctIndex?"Correct. Well done.":"Not quite. Review the lesson and try again."}</strong>
     {selected===correctIndex?<span> {feedback}</span>:null}
   </p>:null}
 </section>;
}
