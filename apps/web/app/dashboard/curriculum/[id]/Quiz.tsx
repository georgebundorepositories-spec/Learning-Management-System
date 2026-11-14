"use client";
import {useState} from "react";
import {recordLessonCompletion} from "./completion-actions";

export default function Quiz({
 lessonId,question,choices,correctIndex,feedback,trackProgress,initialCompleted
}:{lessonId:string;question:string;choices:string[];correctIndex:number;
 feedback:string;trackProgress:boolean;initialCompleted:boolean}){
 const [selected,setSelected]=useState<number|null>(null);
 const [checked,setChecked]=useState(false);
 const [busy,setBusy]=useState(false);
 const [saved,setSaved]=useState(initialCompleted);
 const [message,setMessage]=useState(initialCompleted?
  "Your earlier self-check completion is saved.":"");
 async function checkAnswer(){
  if(selected===null||busy)return;
  setChecked(true);
  if(selected!==correctIndex||!trackProgress||saved)return;
  setBusy(true);
  try{
   const result=await recordLessonCompletion(lessonId);
   setSaved(result.saved);setMessage(result.message);
  }catch{
   setMessage("Progress was not saved. Reconnect and retry the correct answer.");
  }finally{setBusy(false)}
 }
 return <section className="summary-card" aria-label="Self-check question">
   <h2>Check your understanding</h2>
   <p>{question}</p>
   <fieldset style={{border:0,padding:0,margin:0}}>
    <legend className="privacy-note">Choose one answer, then check it.</legend>
    {choices.map((choice,i)=><label key={choice}
      style={{display:"flex",alignItems:"center",gap:10,padding:"10px 0"}}>
      <input type="radio" name="quiz" value={i} checked={selected===i}
        onChange={()=>{setSelected(i);setChecked(false);}}/>
      <span>{choice}</span>
     </label>)}
   </fieldset>
   <button type="button" className="outline-button"
     disabled={selected===null||busy} onClick={checkAnswer}>
     {busy?"Saving completion…":"Check answer"}
   </button>
   {checked?<p role="status" className="notice-panel">
     <strong>{selected===correctIndex?"Correct. Well done.":"Not quite. Review the lesson and try again."}</strong>
     {selected===correctIndex?<span> {feedback}</span>:null}
   </p>:null}
   {trackProgress?<p role="status" className="privacy-note">
    {saved?"Self-check completion synchronised to your school account.":
      message||"Answer correctly to record this formative lesson."}
   </p>:<p className="privacy-note">
     Practice only; staff and guardian previews are not learner completions.
   </p>}
   <p className="privacy-note">
     Self-check completion is not a teacher-awarded grade or formal assessment.
   </p>
 </section>;
}
