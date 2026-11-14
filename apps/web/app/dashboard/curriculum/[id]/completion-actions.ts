"use server";
import {createClient} from "@/lib/supabase/server";
import {getCurriculumUnit} from "@/lib/curriculum/catalog";
import {revalidatePath} from "next/cache";

/** Learner self-check only. This is NOT a teacher mark or exam score.
 * The browser never supplies school, learner identity or a mark.
 */
export async function recordLessonCompletion(lessonId:string):
 Promise<{saved:boolean;message:string}>{
 const unit=typeof lessonId==="string" && lessonId.length<=128
  ?getCurriculumUnit(lessonId):undefined;
 if(!unit)return {saved:false,message:"Unknown curriculum lesson"};
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();
 if(!user)return {saved:false,message:"Sign in to save learning progress"};
 const {data:actor,error:actorError}=await supabase.from("users")
  .select("school_id,role,is_active").eq("id",user.id).maybeSingle();
 if(actorError||!actor?.is_active||actor.role!=="student")
  return {saved:false,message:"Only an enrolled learner may save self-check completion"};
 const {error}=await supabase.from("lesson_completions").insert({
  school_id:actor.school_id,student_user_id:user.id,lesson_id:unit.id
 });
 if(error && error.code!=="23505")
  return {saved:false,message:"Could not save progress. Check your connection and try again."};
 revalidatePath("/dashboard/progress");
 revalidatePath("/dashboard/curriculum/"+unit.id);
 return {saved:true,message:"Self-check completion saved to your school account"};
}
