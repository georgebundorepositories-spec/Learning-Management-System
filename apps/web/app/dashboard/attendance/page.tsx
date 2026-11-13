import Link from "next/link";
import {redirect,notFound} from "next/navigation";
import {createClient} from "@/lib/supabase/server";
import {attendanceForEnrolments,attendanceKey,harareCalendarDate,isCalendarDate} from "@/lib/attendance/register";
import {markAttendance} from "./actions";
import AttendanceSaveButton from "./AttendanceSaveButton";

export const dynamic="force-dynamic";

export default async function AttendancePage({
 searchParams
}:{searchParams:Promise<{date?:string;saved?:string}>}){
 const supabase=await createClient();
 const {data:{user}}=await supabase.auth.getUser();
 if(!user)redirect("/login");
 const {data:profile}=await supabase.from("users")
  .select("school_id,role,is_active,deleted_at").eq("id",user.id).maybeSingle();
 if(!profile?.is_active||profile.deleted_at||!["admin","head","teacher"].includes(profile.role))
  notFound();

 const params=await searchParams;
 const selectedDate=isCalendarDate(params.date??"")?params.date!:harareCalendarDate();
 const classesResult=await supabase.from("classes")
  .select("id,name,teacher_id").eq("school_id",profile.school_id)
  .is("deleted_at",null).order("name").limit(200);
 const assignmentsResult=profile.role==="teacher"
  ?await supabase.from("class_subject_teacher").select("class_id")
   .eq("teacher_id",user.id).is("deleted_at",null).limit(500)
  :{data:[] as {class_id:string}[],error:null};
 const authorisedClasses=new Set((assignmentsResult.data??[]).map(row=>row.class_id));
 const classes=(classesResult.data??[]).filter(klass=>
  profile.role!=="teacher"||klass.teacher_id===user.id||authorisedClasses.has(klass.id));
 const classIds=classes.map(klass=>klass.id);

 const enrolledResult=classIds.length?await supabase.from("student_profiles")
  .select("user_id,class_id").in("class_id",classIds).is("deleted_at",null).limit(1000)
  :{data:[],error:null};
 const enrolments=(enrolledResult.data??[]).filter(e=>classIds.includes(e.class_id));
 const studentIds=[...new Set(enrolments.map(e=>e.user_id))];
 const [studentsResult,marksResult]=await Promise.all([
  studentIds.length?supabase.from("users").select("id,full_name")
   .eq("school_id",profile.school_id).eq("role","student").eq("is_active",true)
   .is("deleted_at",null).in("id",studentIds).limit(1000)
   :Promise.resolve({data:[],error:null}),
  classIds.length?supabase.from("attendance")
   .select("id,class_id,student_user_id,date,status").eq("school_id",profile.school_id)
   .in("class_id",classIds).eq("date",selectedDate).is("deleted_at",null).limit(1000)
   :Promise.resolve({data:[],error:null})
 ]);
 const students=new Map((studentsResult.data??[]).map(student=>[student.id,student.full_name]));
 const visibleEnrolments=enrolments.filter(e=>students.has(e.user_id));
 const attendance=attendanceForEnrolments(visibleEnrolments,marksResult.data??[]);
 const hasError=Boolean(classesResult.error||assignmentsResult.error||
  enrolledResult.error||studentsResult.error||marksResult.error);
 const truncated=(classesResult.data?.length??0)===200||
  (enrolledResult.data?.length??0)===1000||
  (studentsResult.data?.length??0)===1000||
  (marksResult.data?.length??0)===1000;
 return <main className="dashboard-page" id="main-content">
  <header className="topbar">
   <div className="topbrand"><span className="brand-mark">Z</span><strong>ZimSchool LMS</strong></div>
   <Link href="/dashboard" className="outline-button">← Dashboard</Link>
  </header>
  <div className="dash-wrap">
   <p className="eyebrow">TEACHING OPERATIONS</p><h1>Attendance register</h1>
   <p className="subhead">Choose a Zimbabwe school date, review recorded marks and save changes to the live class register.</p>
   <form action="/dashboard/attendance" method="get" className="attendance-date-picker">
    <label htmlFor="attendance-date">Register date</label>
    <input id="attendance-date" name="date" type="date" defaultValue={selectedDate}
     min="2000-01-01" max="2099-12-31" required/>
    <button type="submit" className="outline-button">View register</button>
   </form>
   {params.saved==="1"?<p className="notice-panel" role="status">Attendance saved for {selectedDate}. The register below reflects the latest school records.</p>:null}
   {hasError?<p role="alert" className="notice-panel">The register could not be loaded completely. Do not rely on its totals or submit marks until it loads successfully.</p>:null}
   {truncated?<p role="alert" className="notice-panel">The school register exceeds this page's safe 1,000-record limit. Use the school reporting tools for complete records.</p>:null}
   <div className="summary-cards">
    <article className="summary-card"><span>Available classes</span><strong>{hasError?"—":classes.length}</strong></article>
    <article className="summary-card"><span>Enrolled learners</span><strong>{hasError?"—":attendance.enrolled}</strong></article>
    <article className="summary-card"><span>Marked on {selectedDate}</span><strong>{hasError?"—":attendance.marked}</strong></article>
    <article className="summary-card"><span>Not yet marked</span><strong>{hasError?"—":attendance.remaining}</strong></article>
   </div>
   {!hasError&&classes.length===0?<p className="notice-panel">No assigned classes available. Contact your school administrator.</p>:null}
   {!hasError?<div className="attendance-class-list">{classes.map(klass=>{
    const roster=visibleEnrolments.filter(e=>e.class_id===klass.id);
    const classCount=roster.filter(e=>attendance.recorded.has(attendanceKey(klass.id,e.user_id))).length;
    return <section key={klass.id} className="summary-card attendance-class">
     <div className="attendance-class-header"><h2>{klass.name}</h2>
      <span>{classCount} of {roster.length} marked</span></div>
     {!roster.length?<p>No enrolled learners available for this class.</p>:
      roster.map(e=>{
       const previous=attendance.recorded.get(attendanceKey(klass.id,e.user_id));
       return <form key={e.user_id} action={markAttendance} className="attendance-form">
        <div className="attendance-learner"><strong>{students.get(e.user_id)}</strong>
         <small>{previous?"Recorded: "+previous:"Not yet recorded"}</small>
        </div>
        <input type="hidden" name="student_id" value={e.user_id}/>
        <input type="hidden" name="class_id" value={klass.id}/>
        <input type="hidden" name="date" value={selectedDate}/>
        <label>Status
         <select key={selectedDate} name="status" defaultValue={previous??""} required>
          <option value="" disabled>Select status</option>
          <option value="present">Present</option>
          <option value="absent">Absent</option>
          <option value="late">Late</option>
          <option value="excused">Excused</option>
         </select>
        </label>
        <AttendanceSaveButton previouslyMarked={Boolean(previous)}/>
       </form>;
      })}
    </section>;
   })}</div>:null}
   <p className="privacy-note">Only your permitted classes are displayed. A saved mark is read back from Supabase; viewing another date never changes any records. Offline attendance syncing is not available.</p>
  </div>
 </main>;
}
