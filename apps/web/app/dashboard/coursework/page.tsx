import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { createCoursework, submitCoursework } from "./actions";

export const dynamic = "force-dynamic";

/** Authenticated assignment list. PostgreSQL policies restrict class visibility
 * and private answers; neither the server nor the browser uses a secret key. */
export default async function CourseworkPage() {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) redirect("/login");
  const { data: profile } = await supabase.from("users")
    .select("role,school_id,is_active")
    .eq("id", user.id).maybeSingle();
  if (!profile?.is_active) notFound();

  const canPublish = ["admin", "head", "teacher"].includes(profile.role);
  const canSubmit = profile.role === "student";

  const [classes, subjects, assignments, submissions] = await Promise.all([
    supabase.from("classes").select("id,name").eq("school_id", profile.school_id)
      .is("deleted_at", null).order("name").limit(100),
    supabase.from("subjects").select("id,name").eq("school_id", profile.school_id)
      .is("deleted_at", null).order("name").limit(100),
    supabase.from("school_assignments")
      .select("id,class_id,subject_id,title,instructions,due_on,created_at")
      .eq("school_id", profile.school_id)
      .is("deleted_at", null).order("created_at", { ascending: false }).limit(100),
    supabase.from("assignment_submissions")
      .select("id,assignment_id,student_user_id,text_answer,submitted_at,updated_at")
      .eq("school_id", profile.school_id)
      .order("submitted_at", { ascending: false }).limit(500)
  ]);
  const students = [...new Set((submissions.data ?? []).map(s => s.student_user_id))];
  const { data: names } = students.length
    ? await supabase.from("users").select("id,full_name")
      .eq("school_id", profile.school_id).in("id", students).limit(500)
    : { data: [] };

  return <main className="dashboard-page">
    <header className="topbar">
      <div className="topbrand">
        <span className="brand-mark">Z</span><strong>ZimSchool LMS</strong>
      </div>
      <Link href="/dashboard" className="outline-button">← Dashboard</Link>
    </header>
    <div className="dash-wrap">
      <p className="eyebrow">CLASSROOM WORK</p>
      <h1>Assignments</h1>
      <p className="subhead">
        Teacher instructions and private learner answers. Text submissions only;
        file attachments are not yet enabled.
      </p>

      {canPublish && <section className="summary-card">
        <h2>Publish an assignment</h2>
        <form action={createCoursework} className="resource-form">
          <label>Class
            <select name="class_id" required>
              {(classes.data ?? []).map(c =>
                <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </label>
          <label>Subject
            <select name="subject_id" required>
              {(subjects.data ?? []).map(s =>
                <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </label>
          <label>Title
            <input name="title" required minLength={3} maxLength={180}/>
          </label>
          <label>Due date (optional)
            <input type="date" name="due_on"/>
          </label>
          <label className="form-full">Instructions
            <textarea name="instructions" required
              minLength={3} maxLength={5000} rows={5}/>
          </label>
          <button className="outline-button" type="submit"
            disabled={!classes.data?.length || !subjects.data?.length}>
            Publish assignment
          </button>
        </form>
        <p className="privacy-note">
          Only your assigned classes and subjects can be published to.
          The school server verifies every submission.
        </p>
      </section>}

      <h2>Class assignments</h2>
      {assignments.error || submissions.error
        ? <p role="alert" className="notice-panel">
            Unable to load assignments or private answers. Try again.
          </p>
        : !assignments.data?.length
          ? <p className="notice-panel">
              No assignments are available for your account.
            </p>
          : <div className="summary-cards">
            {assignments.data.map(assignment => {
              const own = (submissions.data ?? []).find(s =>
                s.assignment_id === assignment.id && s.student_user_id === user.id);
              const answers = (submissions.data ?? []).filter(s =>
                s.assignment_id === assignment.id);
              return <section key={assignment.id} className="summary-card">
                <span>
                  {classes.data?.find(c => c.id === assignment.class_id)?.name ?? "Class"}
                  {" · "}
                  {subjects.data?.find(s => s.id === assignment.subject_id)?.name ?? "Subject"}
                </span>
                <strong>{assignment.title}</strong>
                <p style={{ whiteSpace: "pre-wrap" }}>{assignment.instructions}</p>
                <span>{assignment.due_on
                  ? "Due " + assignment.due_on
                  : "No due date specified"}</span>

                {canSubmit && <form action={submitCoursework}
                  className="coursework-answer-form">
                  <input type="hidden" name="assignment_id" value={assignment.id}/>
                  <label>Your answer
                    <textarea name="text_answer" rows={5}
                      minLength={1} maxLength={5000}
                      required defaultValue={own?.text_answer ?? ""}/>
                  </label>
                  <button type="submit" className="outline-button">
                    {own ? "Update my answer" : "Submit my answer"}
                  </button>
                  {own && <small>Last saved {new Date(own.updated_at)
                    .toLocaleDateString("en-ZW")}</small>}
                </form>}

                {!canSubmit && answers.length > 0 &&
                  <div className="coursework-submissions">
                    <h3>Submitted answers ({answers.length})</h3>
                    {answers.map(answer => <article key={answer.id}
                      className="coursework-submission">
                      <strong>{names?.find(n => n.id === answer.student_user_id)
                        ?.full_name ?? "Linked learner"}</strong>
                      <small>{new Date(answer.submitted_at)
                        .toLocaleDateString("en-ZW")}</small>
                      <p style={{ whiteSpace: "pre-wrap" }}>{answer.text_answer}</p>
                    </article>)}
                  </div>}
              </section>;
            })}
          </div>}
      <p className="privacy-note">
        Submissions are visible only to the learner, linked guardians, and
        authorised teaching staff. Answers are not published to other learners.
      </p>
    </div>
  </main>;
}
