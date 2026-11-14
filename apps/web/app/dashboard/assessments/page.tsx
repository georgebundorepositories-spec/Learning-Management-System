import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { recordAssessmentScore } from "./actions";
import AssessmentCreationForm from "./AssessmentCreationForm";

export const dynamic = "force-dynamic";

export default async function Assessments() {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) redirect("/login");

  const { data: actor } = await supabase.from("users")
    .select("school_id,role,is_active").eq("id",user.id).maybeSingle();
  if (!actor?.is_active || !["admin","head","teacher","student","parent"]
    .includes(actor.role)) notFound();

  const isTeacher = actor.role === "teacher";
  const canMark = ["admin","head","teacher"].includes(actor.role);
  const school = actor.school_id;

  const [classReply,subjectReply,assignmentReply,assessmentReply,marksReply] =
    await Promise.all([
      canMark ? supabase.from("classes").select("id,name,teacher_id")
        .eq("school_id",school).is("deleted_at",null).order("name").limit(100)
        : Promise.resolve({data: [] as {id:string;name:string;teacher_id:string|null}[]}),
      supabase.from("subjects").select("id,name").eq("school_id",school)
        .is("deleted_at",null).order("name").limit(100),
      isTeacher ? supabase.from("class_subject_teacher")
        .select("class_id,subject_id").eq("teacher_id",user.id)
        .is("deleted_at",null).limit(500)
        : Promise.resolve({data: [] as {class_id:string;subject_id:string}[]}),
      supabase.from("assessments")
        .select("id,class_id,subject_id,title,max_score,assessment_date")
        .eq("school_id",school).is("deleted_at",null)
        .order("assessment_date",{ascending:false}).limit(100),
      supabase.from("assessment_results")
        .select("id,assessment_id,student_user_id,score")
        .order("created_at",{ascending:false}).limit(500)
    ]);

  const allClasses = classReply.data ?? [];
  const subjects = subjectReply.data ?? [];
  const assignments = assignmentReply.data ?? [];
  const availableClasses = isTeacher ? allClasses.filter(c =>
    c.teacher_id === user.id || assignments.some(a => a.class_id === c.id))
    : allClasses;

  const allowedClassIds = new Set(availableClasses.map(c => c.id));
  const { data: enrolments } = canMark && allowedClassIds.size
    ? await supabase.from("student_profiles")
      .select("user_id,class_id").in("class_id",[...allowedClassIds])
      .is("deleted_at",null).limit(500)
    : { data: [] as {user_id:string;class_id:string}[] };

  const marks = marksReply.data ?? [];
  const learnerIds = [...new Set([
    ...(enrolments ?? []).map(e => e.user_id),
    ...marks.map(m => m.student_user_id)
  ])];
  const { data: learners } = learnerIds.length
    ? await supabase.from("users").select("id,full_name")
      .eq("school_id",school).eq("role","student").in("id",learnerIds)
      .limit(500)
    : { data: [] as {id:string;full_name:string}[] };

  return <main className="dashboard-page">
    <header className="topbar">
      <div className="topbrand">
        <span className="brand-mark">Z</span>
        <strong>ZimSchool LMS</strong>
      </div>
      <Link href="/dashboard" className="outline-button">← Dashboard</Link>
    </header>
    <div className="dash-wrap">
      <p className="eyebrow">ACADEMIC PERFORMANCE</p>
      <h1>Assessments &amp; results</h1>
      <p className="subhead">
        {canMark
          ? "Create assigned-subject assessments and record learner marks."
          : "Private marks for you or learners linked to your account."}
      </p>

      {canMark && <section className="summary-card">
        <h2>Create assessment</h2>
        <AssessmentCreationForm
          classes={availableClasses} subjects={subjects}
          assignments={assignments} teacherId={user.id}
          isTeacher={isTeacher}/>
      </section>}

      <h2>Assessment register</h2>
      {assessmentReply.error || marksReply.error
        ? <p role="alert" className="notice-panel">
            Assessment records could not be loaded. Check your school permissions.
          </p>
        : !assessmentReply.data?.length
          ? <p className="notice-panel">
              No assessments are available to your account yet.
            </p>
          : <div className="summary-cards">
            {assessmentReply.data.map(a => {
              const associatedMarks = marks.filter(r => r.assessment_id === a.id);
              const roster = (enrolments ?? []).filter(e => e.class_id === a.class_id);
              return <section className="summary-card" key={a.id}>
                <span>
                  {subjects.find(s => s.id === a.subject_id)?.name ?? "Subject"}
                  {" · "}{a.assessment_date}
                </span>
                <strong>{a.title}</strong>
                <span>Maximum: {a.max_score}</span>
                {canMark ? <>
                  {roster.length === 0 &&
                    <p className="privacy-note">No enrolled learners in this class.</p>}
                  {roster.map(e => {
                    const learner = learners?.find(l => l.id === e.user_id);
                    if (!learner) return null;
                    const mark = associatedMarks.find(r => r.student_user_id === learner.id);
                    return <form className="assessment-score-form"
                      action={recordAssessmentScore} key={learner.id}>
                      <input type="hidden" name="assessment_id" value={a.id}/>
                      <input type="hidden" name="student_user_id" value={learner.id}/>
                      <label>{learner.full_name}
                        <input type="number" name="score" min="0"
                          max={a.max_score} step="0.01" required
                          defaultValue={mark?.score ?? ""}/>
                      </label>
                      <button type="submit" className="outline-button">
                        {mark ? "Update mark" : "Save mark"}
                      </button>
                    </form>;
                  })}
                </> : associatedMarks.length
                  ? associatedMarks.map(r => <p key={r.id}>
                      <strong>{learners?.find(l => l.id === r.student_user_id)
                        ?.full_name ?? "Learner"}:</strong>{" "}
                      {r.score} / {a.max_score}
                    </p>)
                  : <p className="privacy-note">
                      No result recorded for your linked learner.
                    </p>}
              </section>;
            })}
          </div>}
      <p className="privacy-note">
        Subjects and grades are restricted by school role and teaching assignment.
        Private learner marks are protected in the school database.
      </p>
    </div>
  </main>;
}
