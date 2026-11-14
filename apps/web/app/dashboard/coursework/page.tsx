import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { submitCoursework, reviewCoursework } from "./actions";
import CourseworkCreationForm from "./CourseworkCreationForm";

export const dynamic = "force-dynamic";

export default async function CourseworkPage() {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) redirect("/login");
  const { data: profile } = await supabase.from("users")
    .select("role,school_id,is_active").eq("id",user.id).maybeSingle();
  if (!profile?.is_active ||
    !["admin","head","teacher","student","parent"].includes(profile.role))
    notFound();

  const canPublish = ["admin","head","teacher"].includes(profile.role);
  const canSubmit = profile.role === "student";
  const isTeacher = profile.role === "teacher";
  const school = profile.school_id;

  const [classes,subjects,teachingAssignments,assignments,submissions,reviews] =
    await Promise.all([
      supabase.from("classes").select("id,name,teacher_id")
        .eq("school_id",school).is("deleted_at",null).order("name").limit(100),
      supabase.from("subjects").select("id,name").eq("school_id",school)
        .is("deleted_at",null).order("name").limit(100),
      isTeacher ? supabase.from("class_subject_teacher")
        .select("class_id,subject_id").eq("teacher_id",user.id)
        .is("deleted_at",null).limit(500)
        : Promise.resolve({data:[] as {class_id:string;subject_id:string}[]}),
      supabase.from("school_assignments")
        .select("id,class_id,subject_id,title,instructions,due_on,max_score,created_at")
        .eq("school_id",school).is("deleted_at",null)
        .order("created_at",{ascending:false}).limit(100),
      supabase.from("assignment_submissions")
        .select("id,assignment_id,student_user_id,text_answer,submitted_at,updated_at")
        .eq("school_id",school).order("submitted_at",{ascending:false}).limit(500),
      supabase.from("assignment_reviews")
        .select("id,submission_id,score,feedback,reviewed_at")
        .eq("school_id",school).order("reviewed_at",{ascending:false}).limit(500)
    ]);

  const classesList = classes.data ?? [];
  const allowedClasses = isTeacher ? classesList.filter(c =>
    c.teacher_id === user.id ||
    (teachingAssignments.data ?? []).some(a => a.class_id === c.id))
    : classesList;

  const studentIds = [...new Set((submissions.data ?? []).map(s => s.student_user_id))];
  const { data: names } = studentIds.length
    ? await supabase.from("users").select("id,full_name")
      .eq("school_id",school).in("id",studentIds).limit(500)
    : {data:[] as {id:string;full_name:string}[]};

  const available = assignments.data ?? [];
  const answers = submissions.data ?? [];
  const graded = reviews.data ?? [];

  return <main className="dashboard-page">
    <header className="topbar">
      <div className="topbrand">
        <span className="brand-mark">Z</span><strong>ZimSchool LMS</strong>
      </div>
      <Link href="/dashboard" className="outline-button">← Dashboard</Link>
    </header>
    <div className="dash-wrap">
      <p className="eyebrow">CLASSROOM WORK</p>
      <h1>Coursework &amp; feedback</h1>
      <p className="subhead">
        Class assignments, protected learner submissions and teacher grading.
        Text-only answers; file attachments are not yet enabled.
      </p>

      {canPublish && <section className="summary-card">
        <h2>Publish an assignment</h2>
        <CourseworkCreationForm
          classes={allowedClasses} subjects={subjects.data ?? []}
          assignments={teachingAssignments.data ?? []}
          teacherId={user.id} isTeacher={isTeacher}/>
      </section>}

      <h2>Assignments</h2>
      {assignments.error || submissions.error || reviews.error
        ? <p role="alert" className="notice-panel">
            School records could not be loaded. Check your account's permissions.
          </p>
        : !available.length
          ? <p className="notice-panel">
              No assignments are available for your account yet.
            </p>
          : <div className="summary-cards">
            {available.map(assignment => {
              const related = answers.filter(s => s.assignment_id === assignment.id);
              const own = related.find(s => s.student_user_id === user.id);
              const ownReview = graded.find(g => g.submission_id === own?.id);
              return <section className="summary-card" key={assignment.id}>
                <span>
                  {classesList.find(c => c.id === assignment.class_id)?.name ?? "Class"}
                  {" · "}
                  {subjects.data?.find(s => s.id === assignment.subject_id)?.name ?? "Subject"}
                </span>
                <strong>{assignment.title}</strong>
                <p style={{whiteSpace:"pre-wrap"}}>{assignment.instructions}</p>
                <span>
                  {assignment.due_on ? "Due " + assignment.due_on : "No due date"}
                  {" · Maximum: "}{assignment.max_score}
                </span>

                {canSubmit && <>
                  {ownReview && <div className="coursework-review">
                    <strong>Marked: {ownReview.score} / {assignment.max_score}</strong>
                    <p style={{whiteSpace:"pre-wrap"}}>
                      {ownReview.feedback || "No written feedback recorded."}
                    </p>
                    <small>Reviewed {new Date(ownReview.reviewed_at)
                      .toLocaleDateString("en-ZW")}</small>
                    <p className="privacy-note">
                      Your reviewed answer is locked against further edits.
                    </p>
                  </div>}
                  {!ownReview && <form action={submitCoursework}
                    className="coursework-answer-form">
                    <input name="assignment_id" type="hidden" value={assignment.id}/>
                    <label>Your answer
                      <textarea name="text_answer" rows={5} minLength={1}
                        maxLength={5000} required
                        defaultValue={own?.text_answer ?? ""}/>
                    </label>
                    <button type="submit" className="outline-button">
                      {own ? "Update answer" : "Submit answer"}
                    </button>
                    {own && <small>Saved {new Date(own.updated_at)
                      .toLocaleDateString("en-ZW")}</small>}
                  </form>}
                </>}

                {!canSubmit && related.length > 0 &&
                  <div className="coursework-submissions">
                    <h3>Submitted answers ({related.length})</h3>
                    {related.map(answer => {
                      const review = graded.find(g => g.submission_id === answer.id);
                      return <article key={answer.id} className="coursework-submission">
                        <strong>{names?.find(n => n.id === answer.student_user_id)
                          ?.full_name ?? "Linked learner"}</strong>
                        <small>Submitted {new Date(answer.submitted_at)
                          .toLocaleDateString("en-ZW")}</small>
                        <p style={{whiteSpace:"pre-wrap"}}>{answer.text_answer}</p>
                        {canPublish ? <form action={reviewCoursework}
                          className="coursework-review-form">
                          <input type="hidden" name="submission_id" value={answer.id}/>
                          <label>Score (out of {assignment.max_score})
                            <input type="number" name="score" required min="0"
                              max={assignment.max_score} step="0.01"
                              defaultValue={review?.score ?? ""}/>
                          </label>
                          <label>Teacher feedback
                            <textarea name="feedback" rows={3}
                              maxLength={2000}
                              defaultValue={review?.feedback ?? ""}/>
                          </label>
                          <button className="outline-button" type="submit">
                            {review ? "Update grade and feedback" : "Record grade"}
                          </button>
                        </form> : review ? <div className="coursework-review">
                          <strong>Mark: {review.score} / {assignment.max_score}</strong>
                          <p style={{whiteSpace:"pre-wrap"}}>
                            {review.feedback || "No written feedback recorded."}
                          </p>
                        </div> : <p className="privacy-note">Not yet graded.</p>}
                      </article>;
                    })}
                  </div>}
              </section>;
            })}
          </div>}
      <p className="privacy-note">
        The school database restricts teaching access by class and subject.
        Grades are visible only to authorised teachers, learners and linked guardians.
        Once graded, a learner must request a new assignment to submit a revision.
      </p>
    </div>
  </main>;
}
