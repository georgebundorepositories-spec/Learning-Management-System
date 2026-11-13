"use client";
import {useFormStatus} from "react-dom";

/** The visual pending state is tied to the actual server form submission. */
export default function AttendanceSaveButton({previouslyMarked}:{previouslyMarked:boolean}){
 const {pending}=useFormStatus();
 return <button type="submit" className="outline-button" disabled={pending}
  aria-disabled={pending} aria-live="polite">
  {pending?"Saving…":previouslyMarked?"Update mark":"Save mark"}
 </button>;
}
