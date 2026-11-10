"use server";
import {createClient} from "@/lib/supabase/server";
import {getCurriculumUnit,gradeMatches,subjectMatches,curriculumAssignmentInstructions} from "@/lib/curriculum/catalog";
import {createCoursework} from "../coursework/actions";

const uuid=/^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i;

/** A teacher/admin explicitly chooses a matching school class and subject.
 * Original templates are read from server-owned catalogue, never from submitted text.
 * Existing school coursework action + RLS remains the final authorisation layer.
 */
export async function publishCurriculumAssignment(form:FormData):Promise<void>{
 const unitId=String(form.get("unit_id")??"");
 const classId=String(form.get("class_id")??"");
 const subjectId=String(form.get("subject_id")??"");
 const unit=getCurriculumUnit(unitId);
 if(!unit || !uuid.test(classId)||!uuid.test(subjectId))
   throw new Error("Choose a valid curriculum unit, class and subject");
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();
 if(!user)throw new Error("Sign in required");
 const {data:actor}=await supabase.from("users")
   .select("role,school_id,is_active").eq("id",user.id).maybeSingle();
 if(!actor?.is_active||!["admin","head","teacher"].includes(actor.role))
   throw new Error("Only authorised school staff can publish coursework");
 const [{data:klass},{data:subject}]=await Promise.all([
  supabase.from("classes").select("id,name,grade_level").eq("id",classId)
    .eq("school_id",actor.school_id).is("deleted_at",null).maybeSingle(),
  supabase.from("subjects").select("id,name").eq("id",subjectId)
    .eq("school_id",actor.school_id).is("deleted_at",null).maybeSingle()
 ]);
 if(!klass || !subject || !gradeMatches(unit.level,klass.grade_level??"",klass.name)
   || !subjectMatches(unit.subject,subject.name))
   throw new Error("The chosen class and subject must match the lesson grade and curriculum subject");
 const assignment=new FormData();
 assignment.set("class_id",klass.id);
 assignment.set("subject_id",subject.id);
 assignment.set("title",unit.assignment.title.slice(0,180));
 assignment.set("instructions",curriculumAssignmentInstructions(unit));
 assignment.set("max_score",String(unit.assignment.maxScore));
 await createCoursework(assignment);
}
