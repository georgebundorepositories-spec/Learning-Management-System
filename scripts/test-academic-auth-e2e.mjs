// End-to-end LOCAL Supabase Data API test of real auth sessions, coursework and grading.
// Intentionally refuses any remotely hosted Supabase URL. No production accounts or data.
// Test records are synthetic, uniquely named, and removed in a finally block.
import { createClient } from "@supabase/supabase-js";
import { randomBytes } from "node:crypto";
import assert from "node:assert/strict";

const url = process.env.SUPABASE_URL ?? "";
const serviceKey = process.env.SUPABASE_SECRET_KEY ?? "";
const publishable = process.env.SUPABASE_PUBLISHABLE_KEY ?? "";
if (!/^http:\/\/(localhost|127\.0\.0\.1):\d+\/?$/.test(url)) {
  throw new Error("Refusing to run academic Auth tests outside a local Supabase instance");
}
if (!serviceKey || !publishable) throw new Error("Local Supabase credentials required");

const server = createClient(url, serviceKey, {
  auth: {autoRefreshToken:false, persistSession:false, detectSessionInUrl:false}
});
const unique = randomBytes(6).toString("hex");
const password = "CI!" + randomBytes(22).toString("hex");
const cleanup = {
  schools: [], authUsers: [], users: [], academicYears: [], terms: [],
  classes: [], subjects: [], classAssignments: [], guardians: [], profiles: [],
  assignments: [], submissions: [], reviews: [], plans: [],
  assessments: [], assessmentResults: [], completionUsers: []
};
const clients = [];

async function ok(promise, label) {
  const {data, error} = await promise;
  if (error) throw new Error(label + ": " + error.message);
  return data;
}
async function makeRow(table, row, label) {
  const data = await ok(server.from(table).insert(row).select("id").single(),label);
  return data.id;
}
async function createPerson(name, role, schoolId, sequence) {
  const email = "ci-academic-" + unique + "-" + sequence + "@example.invalid";
  const authUser = await ok(server.auth.admin.createUser({
    email, password, email_confirm:true
  }), "Create " + name + " synthetic Auth identity");
  assert.ok(authUser.user?.id);
  cleanup.authUsers.push(authUser.user.id);
  const id = authUser.user.id;
  await ok(server.from("users").insert({
    id, school_id:schoolId, full_name:name, role, email,
    phone:"+26377" + String(1000000 + sequence * 101 + Number.parseInt(unique.slice(0,4),16)),
    is_active:true
  }), "Create " + name + " school membership");
  cleanup.users.push(id);
  const client = createClient(url, publishable, {
    auth:{autoRefreshToken:false,persistSession:false,detectSessionInUrl:false}
  });
  const signed = await ok(client.auth.signInWithPassword({email,password}),
    "Sign in " + name);
  assert.equal(signed.user.id, id, "JWT resolves correct " + role);
  clients.push(client);
  return {id, client, role};
}
async function readIds(client, table, filterColumn, filterId) {
  let q = client.from(table).select("id");
  if (filterColumn) q = q.eq(filterColumn,filterId);
  const rows = await ok(q, "Read " + table);
  return new Set(rows.map(row=>row.id));
}
async function clearRecords() {
  let errors = [];
  const tableGroups = [
    ["assessment_results","assessmentResults"],
    ["assessments","assessments"],
    ["teacher_lesson_plans","plans"],
    ["assignment_reviews","reviews"],
    ["assignment_submissions","submissions"],
    ["school_assignments","assignments"],
    ["student_guardian_link","guardians"],
    ["student_profiles","profiles"],
    ["class_subject_teacher","classAssignments"],
    ["terms","terms"],
    ["classes","classes"],
    ["subjects","subjects"],
    ["academic_years","academicYears"],
    ["users","users"],
    ["schools","schools"]
  ];
  for (const client of clients) {
    try {await client.auth.signOut();} catch { /* local test server may be stopped */ }
  }
  if (cleanup.completionUsers.length) {
    const {error}=await server.from("lesson_completions")
      .delete().in("student_user_id",cleanup.completionUsers);
    if(error)errors.push("lesson_completions: "+error.message);
  }
  for (const [table, key] of tableGroups) {
    if (!cleanup[key].length) continue;
    const {error} = await server.from(table).delete().in(
      table === "users" ? "id" : table === "student_profiles" ? "user_id" : "id",
      cleanup[key]
    );
    if(error)errors.push(table + ": " + error.message);
  }
  for (const id of cleanup.authUsers.reverse()) {
    const {error} = await server.auth.admin.deleteUser(id);
    if(error)errors.push("Synthetic Auth identity: " + error.message);
  }
  if(errors.length)throw new Error("Local synthetic fixture cleanup failed: "+errors.join("; "));
}

let testError;
try {
  const schoolA = await makeRow("schools", {name:"CI Academic School A "+unique, is_demo:true},
    "Create synthetic school A");
  cleanup.schools.push(schoolA);
  const schoolB = await makeRow("schools", {name:"CI Academic School B "+unique, is_demo:true},
    "Create synthetic school B");
  cleanup.schools.push(schoolB);

  const teacherMath = await createPerson("CI Mathematics Teacher","teacher",schoolA,1);
  const teacherEnglish = await createPerson("CI English Teacher","teacher",schoolA,2);
  const learner = await createPerson("CI Learner","student",schoolA,3);
  const linkedGuardian = await createPerson("CI Linked Guardian","parent",schoolA,4);
  const unrelatedGuardian = await createPerson("CI Unrelated Guardian","parent",schoolA,5);
  const foreignTeacher = await createPerson("CI Other School Teacher","teacher",schoolB,6);
  const schoolAdmin = await createPerson("CI School Administrator","admin",schoolA,7);

  const math = await makeRow("subjects",{school_id:schoolA,name:"CI Mathematics "+unique},
    "Create mathematics subject"); cleanup.subjects.push(math);
  const english = await makeRow("subjects",{school_id:schoolA,name:"CI English "+unique},
    "Create English subject");cleanup.subjects.push(english);
  // No class homeroom teacher: subject permissions must come from teaching assignments.
  const klass = await makeRow("classes",{
    school_id:schoolA,name:"CI Grade Four "+unique,teacher_id:null
  },"Create assigned class");cleanup.classes.push(klass);
  const year = await makeRow("academic_years",{
    school_id:schoolA,name:"CI Academic Year "+unique
  },"Create academic year");cleanup.academicYears.push(year);
  const term = await makeRow("terms",{
    school_id:schoolA,academic_year_id:year,name:"CI Term One",term_number:1
  },"Create school term");cleanup.terms.push(term);
  const mathAssignment = await makeRow("class_subject_teacher",{
    class_id:klass,subject_id:math,teacher_id:teacherMath.id,term_id:term
  },"Assign Mathematics teacher");cleanup.classAssignments.push(mathAssignment);
  const englishAssignment = await makeRow("class_subject_teacher",{
    class_id:klass,subject_id:english,teacher_id:teacherEnglish.id,term_id:term
  },"Assign English teacher");cleanup.classAssignments.push(englishAssignment);
  const profile = await makeRow("student_profiles",{
    user_id:learner.id,class_id:klass,admission_no:"CI-"+unique
  },"Enrol synthetic learner");cleanup.profiles.push(learner.id);
  const guardianLink = await makeRow("student_guardian_link",{
    student_user_id:learner.id,guardian_user_id:linkedGuardian.id,
    is_primary:true,can_pay_fees:true
  },"Create linked guardian");cleanup.guardians.push(guardianLink);

  // Phase 15: live local JWT checks for classroom assessment creation,
  // grade recording, authorised revision and guardian/tenant isolation.
  // The mobile client must use the same RLS as the web client.
  const mathTest=await ok(teacherMath.client.from("assessments").insert({
    school_id:schoolA,class_id:klass,subject_id:math,
    title:"CI Maths Assessment "+unique,max_score:20,
    assessment_date:"2026-11-11",created_by:teacherMath.id
  }).select("id").single(),"Assigned Maths teacher creates a test");
  cleanup.assessments.push(mathTest.id);

  const rejectEnglish=await teacherEnglish.client.from("assessments").insert({
    school_id:schoolA,class_id:klass,subject_id:math,
    title:"Not My Subject "+unique,max_score:20,
    assessment_date:"2026-11-11",created_by:teacherEnglish.id
  }).select("id");
  assert.ok(rejectEnglish.error||!rejectEnglish.data?.length,
    "English teacher must not publish a Maths assessment");

  const rejectStudent=await learner.client.from("assessments").insert({
    school_id:schoolA,class_id:klass,subject_id:math,
    title:"Student Made "+unique,max_score:20,
    assessment_date:"2026-11-11",created_by:learner.id
  }).select("id");
  assert.ok(rejectStudent.error||!rejectStudent.data?.length,
    "Learner must not publish assessments");

  assert.equal((await readIds(teacherMath.client,"assessments")).has(mathTest.id),true);
  assert.equal((await readIds(teacherEnglish.client,"assessments")).has(mathTest.id),false,
    "Other-subject teacher must not see the private Maths test");

  const rejectedCrossSchool=await foreignTeacher.client.from("assessment_results").insert({
    assessment_id:mathTest.id,student_user_id:learner.id,score:5,
    feedback:"Not my tenant",marked_by:foreignTeacher.id
  }).select("id");
  assert.ok(rejectedCrossSchool.error||!rejectedCrossSchool.data?.length,
    "Foreign school staff must not write grades");

  const rejectedOverMax=await teacherMath.client.from("assessment_results").insert({
    assessment_id:mathTest.id,student_user_id:learner.id,score:21,
    feedback:"Exceeds maximum",marked_by:teacherMath.id
  }).select("id");
  assert.ok(rejectedOverMax.error||!rejectedOverMax.data?.length,
    "Assessment score above maximum must be rejected");

  const marked=await ok(teacherMath.client.from("assessment_results").insert({
    assessment_id:mathTest.id,student_user_id:learner.id,score:17,
    feedback:"Good working shown.",marked_by:teacherMath.id
  }).select("id").single(),"Assigned teacher saves assessment grade");
  cleanup.assessmentResults.push(marked.id);

  for(const person of [teacherMath,schoolAdmin,learner,linkedGuardian]){
    assert.equal((await readIds(person.client,"assessment_results")).has(marked.id),true,
      "Only authorised classroom, administrator, learner or guardian sees grade");
  }
  for(const person of [teacherEnglish,unrelatedGuardian,foreignTeacher]){
    assert.equal((await readIds(person.client,"assessment_results")).has(marked.id),false,
      "Wrong subject, unlinked guardian and foreign school cannot read grades");
  }

  const rejectedForeignEdit=await teacherEnglish.client.from("assessment_results")
    .update({score:0,marked_by:teacherEnglish.id})
    .eq("id",marked.id).select("id");
  assert.ok(rejectedForeignEdit.error||!rejectedForeignEdit.data?.length,
    "Teacher for another subject cannot edit a grade");

  const rejectedIdentityEdit=await teacherMath.client.from("assessment_results")
    .update({student_user_id:foreignTeacher.id}).eq("id",marked.id).select("id");
  assert.ok(rejectedIdentityEdit.error||!rejectedIdentityEdit.data?.length,
    "Grade update must not allow learner reassignment");

  const revisedMark=await ok(teacherMath.client.from("assessment_results")
    .update({score:18,feedback:"Excellent progress.",marked_by:teacherMath.id})
    .eq("id",marked.id).select("id,score").single(),
    "Teacher corrects own previous assessment mark");
  assert.equal(revisedMark.id,marked.id);
  assert.equal(Number(revisedMark.score),18);
  const familyMark=await ok(linkedGuardian.client.from("assessment_results")
    .select("score,feedback").eq("id",marked.id).single(),
    "Linked parent sees corrected assessment score");
  assert.equal(Number(familyMark.score),18);
  assert.equal(familyMark.feedback,"Excellent progress.");

  // Phase 10: test real JWT-backed lesson completion via PostgREST, not
  // postgres role impersonation. Only a learner may append, never reassign.
  const lessonId="grade-6-maths-ratio-scale";
  const extraLesson="grade-7-science-respiratory-system";
  const saved=await ok(learner.client.from("lesson_completions").insert({
    school_id:schoolA,student_user_id:learner.id,lesson_id:lessonId
  }).select("lesson_id").single(),"Learner saves formative completion");
  assert.equal(saved.lesson_id,lessonId);
  cleanup.completionUsers.push(learner.id);

  async function lessonIds(person) {
    return new Set((await ok(person.client.from("lesson_completions")
      .select("lesson_id"),"Read private lesson completions")).map(x=>x.lesson_id));
  }
  for(const person of [learner,linkedGuardian,teacherMath]) {
    assert.ok((await lessonIds(person)).has(lessonId),
      "Authorised student, linked parent or assigned teacher must see completion");
  }
  for(const person of [unrelatedGuardian,foreignTeacher]) {
    assert.ok(!(await lessonIds(person)).has(lessonId),
      "Unlinked guardian and other school cannot see completion");
  }
  const wrongOwner=await learner.client.from("lesson_completions").insert({
    school_id:schoolA,student_user_id:teacherMath.id,lesson_id:extraLesson
  });
  assert.ok(wrongOwner.error,"Learner may not add a completion for someone else");
  const teacherWrite=await teacherMath.client.from("lesson_completions").insert({
    school_id:schoolA,student_user_id:learner.id,lesson_id:extraLesson
  });
  assert.ok(teacherWrite.error,"Teacher cannot forge formative completion");
  const unknownId=await learner.client.from("lesson_completions").insert({
    school_id:schoolA,student_user_id:learner.id,lesson_id:"not-a-real-lesson"
  });
  assert.ok(unknownId.error,"Unknown lesson ID cannot be synced");
  const duplicate=await learner.client.from("lesson_completions").insert({
    school_id:schoolA,student_user_id:learner.id,lesson_id:lessonId
  });
  assert.equal(duplicate.error?.code,"23505",
    "Repeated completed lesson must be idempotently rejected");
  const tamper=await learner.client.from("lesson_completions")
    .update({lesson_id:extraLesson}).eq("lesson_id",lessonId).select("lesson_id");
  assert.ok(tamper.error||!tamper.data?.length,
    "Stored completion cannot be reassigned");
  assert.equal((await lessonIds(learner)).size,1,
    "Only one canonical completion remains");


  // Phase 11: real JWT-backed staff lesson planning must never leak to learners,
  // linked parents, unassigned subject teachers or unrelated schools.
  const staffPlan=await ok(teacherMath.client.from("teacher_lesson_plans").insert({
    school_id:schoolA,teacher_id:teacherMath.id,class_id:klass,subject_id:math,
    source_lesson_id:"grade-4-mathematics-four-digit-place-value",
    lesson_title:"CI Place Value "+unique,
    objective:"Identify the place value of digits in four-digit numbers.",
    teaching_steps:"Use physical counters to group thousands, hundreds, tens and ones, then practise together.",
    resources:"Counting counters and a board",assessment:"Ask learners to break apart 2,345.",
    reflection:"",status:"draft"
  }).select("id").single(),"Teacher saves private lesson plan");
  cleanup.plans.push(staffPlan.id);
  for(const person of [teacherMath]){
    assert.equal((await readIds(person.client,"teacher_lesson_plans")).has(staffPlan.id),true,
      "Authorised owner sees own private lesson plan");
  }
  for(const person of [teacherEnglish,learner,linkedGuardian,unrelatedGuardian,foreignTeacher]){
    assert.equal((await readIds(person.client,"teacher_lesson_plans")).has(staffPlan.id),false,
      "Other teacher, learner, parent and foreign school cannot read private teacher plan");
  }
  const wrongSubjectPlan=await teacherEnglish.client.from("teacher_lesson_plans").insert({
    school_id:schoolA,teacher_id:teacherEnglish.id,class_id:klass,subject_id:math,
    source_lesson_id:"grade-4-mathematics-four-digit-place-value",
    lesson_title:"Unauthorised subject",
    objective:"Incorrectly claiming an unrelated teaching subject.",
    teaching_steps:"This must be refused because teacher is not assigned to Maths.",
    status:"draft"
  }).select("id");
  assert.ok(wrongSubjectPlan.error||!wrongSubjectPlan.data?.length,
    "Teacher cannot create a lesson plan in another teacher's subject");
  const parentPlan=await linkedGuardian.client.from("teacher_lesson_plans").insert({
    school_id:schoolA,teacher_id:linkedGuardian.id,class_id:klass,subject_id:math,
    source_lesson_id:"grade-4-mathematics-four-digit-place-value",
    lesson_title:"Guardian attempted plan",
    objective:"Guardian should not write staff private lesson plans.",
    teaching_steps:"This parent does not have the classroom teacher designation.",
    status:"draft"
  });
  assert.ok(parentPlan.error,"Guardian may not forge a lesson plan");
  const hijackPlan=await teacherMath.client.from("teacher_lesson_plans")
    .update({teacher_id:teacherEnglish.id}).eq("id",staffPlan.id).select("id");
  assert.ok(hijackPlan.error||!hijackPlan.data?.length,
    "Teacher cannot transfer lesson plan ownership");
  const revisedPlan=await ok(teacherMath.client.from("teacher_lesson_plans")
    .update({reflection:"Learners understood the grouping activity.",status:"ready"})
    .eq("id",staffPlan.id).select("status,reflection").single(),
    "Teacher revises own private plan");
  assert.equal(revisedPlan.status,"ready");
  assert.match(revisedPlan.reflection,/grouping activity/);
  // A cached identity must lose access immediately when the authoritative
  // school role changes, even before the JWT has been refreshed.
  await ok(server.from("users").update({role:"parent"}).eq("id",teacherMath.id),
    "Temporarily revoke teacher designation in local fixture");
  assert.equal((await readIds(teacherMath.client,"teacher_lesson_plans")).has(staffPlan.id),false,
    "Former teacher must lose private lesson plan access without signing out");
  await ok(server.from("users").update({role:"teacher"}).eq("id",teacherMath.id),
    "Restore local teacher designation");
  assert.equal((await readIds(teacherMath.client,"teacher_lesson_plans")).has(staffPlan.id),true,
    "Restoring assigned teacher role restores plan visibility");

  const published = await ok(teacherMath.client.from("school_assignments").insert({
    school_id:schoolA,class_id:klass,subject_id:math,
    title:"CI Homework "+unique,instructions:"Find the sum of two plus two.",
    max_score:20,created_by:teacherMath.id
  }).select("id").single(), "Teacher publishes assigned maths work");
  cleanup.assignments.push(published.id);

  const englishPublished = await ok(teacherEnglish.client.from("school_assignments").insert({
    school_id:schoolA,class_id:klass,subject_id:english,
    title:"CI English Exercise "+unique,instructions:"Write one sentence.",
    max_score:15,created_by:teacherEnglish.id
  }).select("id").single(), "English teacher publishes assigned work");
  cleanup.assignments.push(englishPublished.id);

  assert.equal((await readIds(teacherMath.client,"school_assignments")).has(published.id),true);
  assert.equal((await readIds(teacherMath.client,"school_assignments")).has(englishPublished.id),false,
    "Maths teacher must not see English coursework");
  assert.equal((await readIds(teacherEnglish.client,"school_assignments")).has(published.id),false,
    "English teacher must not see Maths coursework");
  for (const person of [learner,linkedGuardian]) {
    const ids=await readIds(person.client,"school_assignments");
    assert.ok(ids.has(published.id) && ids.has(englishPublished.id),
      "Learner/family sees coursework from their enrolled class");
  }
  for (const person of [unrelatedGuardian,foreignTeacher]) {
    const ids=await readIds(person.client,"school_assignments");
    assert.ok(!ids.has(published.id) && !ids.has(englishPublished.id),
      "Unlinked or foreign-school actor cannot read coursework");
  }

  const submission = await ok(learner.client.from("assignment_submissions").insert({
    school_id:schoolA,assignment_id:published.id,
    student_user_id:learner.id,text_answer:"Two and two makes four."
  }).select("id").single(),"Learner submits Maths coursework");
  cleanup.submissions.push(submission.id);

  const revised=await ok(learner.client.from("assignment_submissions")
    .update({text_answer:"I counted 2 + 2 to get 4."})
    .eq("id",submission.id).select("id").single(),
    "Learner revises an ungraded answer");
  assert.equal(revised.id,submission.id);

  const {error:teacherWrongSubjectError,data:teacherWrongSubjectData} =
    await teacherEnglish.client.from("assignment_reviews").insert({
      school_id:schoolA,submission_id:submission.id,score:10,feedback:"Not my class",
      reviewed_by:teacherEnglish.id
    }).select("id");
  assert.ok(teacherWrongSubjectError || !teacherWrongSubjectData?.length,
    "English teacher must not mark Maths work");

  const {error:overMaxError,data:overMaxData} =
    await teacherMath.client.from("assignment_reviews").insert({
      school_id:schoolA,submission_id:submission.id,score:21,
      feedback:"Invalid mark",reviewed_by:teacherMath.id
    }).select("id");
  assert.ok(overMaxError || !overMaxData?.length,"Mark exceeding maximum must fail");

  const review = await ok(teacherMath.client.from("assignment_reviews").insert({
    school_id:schoolA,submission_id:submission.id,score:18,
    feedback:"Correct answer. Show your working.",reviewed_by:teacherMath.id
  }).select("id").single(),"Assigned teacher grades answer");
  cleanup.reviews.push(review.id);

  for (const person of [teacherMath,learner,linkedGuardian]) {
    assert.equal((await readIds(person.client,"assignment_reviews")).has(review.id),true,
      "Owner / enrolled learner / linked guardian must see feedback");
  }
  for (const person of [teacherEnglish,unrelatedGuardian,foreignTeacher]) {
    assert.equal((await readIds(person.client,"assignment_reviews")).has(review.id),false,
      "Wrong subject, unlinked parent or foreign-school user must not read feedback");
  }
  const blocked = await learner.client.from("assignment_submissions")
    .update({text_answer:"Trying to edit after grading"})
    .eq("id",submission.id).select("id");
  assert.ok(blocked.error || !blocked.data?.length,
    "Graded learner answer must be locked");
  const {data:unchanged}=await server.from("assignment_submissions")
    .select("text_answer").eq("id",submission.id).single();
  assert.equal(unchanged.text_answer,"I counted 2 + 2 to get 4.");

  const gradeEdit=await learner.client.from("assignment_reviews")
    .update({score:0}).eq("id",review.id).select("id");
  assert.ok(gradeEdit.error || !gradeEdit.data?.length,"Learner must not edit own grade");
  const gradeParentEdit=await linkedGuardian.client.from("assignment_reviews")
    .update({score:0}).eq("id",review.id).select("id");
  assert.ok(gradeParentEdit.error || !gradeParentEdit.data?.length,
    "Guardian cannot edit learner grade");

  await ok(teacherMath.client.from("assignment_reviews")
    .update({score:19,reviewed_by:teacherMath.id,feedback:"Excellent."})
    .eq("id",review.id).select("id").single(),"Assigned teacher revises mark");
  const guardianFeedback=await ok(linkedGuardian.client.from("assignment_reviews")
    .select("score,feedback").eq("id",review.id).single(),"Guardian sees revised feedback");
  assert.equal(Number(guardianFeedback.score),19);
  assert.equal(guardianFeedback.feedback,"Excellent.");

  // Phase 13: exercise the actual administrator membership UI's RLS writes,
  // not the service-role bootstrap client used to create synthetic fixtures.
  const spareClass=await makeRow("classes",{
    school_id:schoolA,name:"CI Spare Grade 3 "+unique,teacher_id:null
  },"Create extra class for live-admin permission checks");
  cleanup.classes.push(spareClass);
  const assignedClassTeacher=await ok(schoolAdmin.client.from("classes")
    .update({teacher_id:teacherMath.id}).eq("id",spareClass)
    .eq("school_id",schoolA).select("teacher_id").single(),
    "School administrator assigns homeroom teacher");
  assert.equal(assignedClassTeacher.teacher_id,teacherMath.id);
  const wrongTenantTeacher=await schoolAdmin.client.from("classes")
    .update({teacher_id:foreignTeacher.id}).eq("id",spareClass)
    .eq("school_id",schoolA).select("id");
  assert.ok(wrongTenantTeacher.error||!wrongTenantTeacher.data?.length,
    "Even administrators cannot assign a teacher from a different school");
  const blockedClassChange=await learner.client.from("classes")
    .update({teacher_id:null}).eq("id",spareClass).select("id");
  assert.ok(blockedClassChange.error||!blockedClassChange.data?.length,
    "Learner cannot update teaching assignment");
  const enrolledElsewhere=await ok(schoolAdmin.client.from("student_profiles")
    .update({class_id:spareClass}).eq("user_id",learner.id)
    .select("class_id").single(),"School admin enrols learner into selected class");
  assert.equal(enrolledElsewhere.class_id,spareClass);
  const restoredClass=await ok(schoolAdmin.client.from("student_profiles")
    .update({class_id:klass}).eq("user_id",learner.id)
    .select("class_id").single(),"School admin restores learner's initial class");
  assert.equal(restoredClass.class_id,klass);
  const illegalEnrolment=await foreignTeacher.client.from("student_profiles")
    .update({class_id:spareClass}).eq("user_id",learner.id).select("id");
  assert.ok(illegalEnrolment.error||!illegalEnrolment.data?.length,
    "Different school teacher cannot modify learner enrolment");

  // Real admin JWT inserts a same-school guardian relationship and later
  // revokes it, proving that RLS changes private visibility immediately.
  const newGuardianLink=await ok(schoolAdmin.client.from("student_guardian_link").insert({
    student_user_id:learner.id,guardian_user_id:unrelatedGuardian.id,
    is_primary:false,can_pay_fees:false
  }).select("id").single(),"School admin adds a guardian relationship");
  cleanup.guardians.push(newGuardianLink.id);
  assert.equal((await lessonIds(unrelatedGuardian)).has(lessonId),true,
    "Linked parent gains only the related learner's progress");
  const revokedNewLink=await ok(schoolAdmin.client.from("student_guardian_link")
    .delete().eq("id",newGuardianLink.id).select("id").single(),
    "School admin revokes a linked guardian");
  assert.equal(revokedNewLink.id,newGuardianLink.id);
  cleanup.guardians=cleanup.guardians.filter(id=>id!==newGuardianLink.id);
  assert.equal((await lessonIds(unrelatedGuardian)).has(lessonId),false,
    "Revoked parent loses lesson access without re-login");

  const failedCrossSchool = await foreignTeacher.client.from("assignment_submissions")
    .select("id").eq("id",submission.id);
  assert.equal(failedCrossSchool.error,null);
  assert.equal(failedCrossSchool.data.length,0);


  // A stale guardian relationship cannot grant parent privileges to a
  // user whose school role has been changed away from parent.
  await ok(server.from("users").update({role:"teacher"}).eq("id",linkedGuardian.id),
    "Temporarily revoke parent designation");
  assert.equal((await lessonIds(linkedGuardian)).has(lessonId),false,
    "Former guardian must lose linked learner progress");
  await ok(server.from("users").update({role:"parent"}).eq("id",linkedGuardian.id),
    "Restore local parent designation");
  assert.equal((await lessonIds(linkedGuardian)).has(lessonId),true,
    "Restored parent relationship still works before link removal");
  // Revocation test: a previous guardian must immediately lose visibility
  // after the school administrator removes the learner relationship.
  const revokedOriginal=await ok(schoolAdmin.client.from("student_guardian_link")
    .delete().eq("id",guardianLink).select("id").single(),
    "School administrator revokes existing parent-learner relationship");
  assert.equal(revokedOriginal.id,guardianLink);
  cleanup.guardians=cleanup.guardians.filter(id=>id!==guardianLink);
  assert.equal((await lessonIds(linkedGuardian)).has(lessonId),false,
    "Revoked guardian must lose lesson progress visibility");
  assert.equal((await readIds(linkedGuardian.client,"assignment_reviews")).has(review.id),false,
    "Revoked guardian must lose private grade feedback visibility");

  console.log("PASS: seven JWT accounts; teacher assessment and mark correction, coursework, admin school membership, parent revocation and tenant RLS; synthetic fixtures cleaned.");
} catch (error) {
  testError=error;
} finally {
  try {await clearRecords();}catch (error) {
    testError=new Error((testError?.message??"Test succeeded") +
      "; cleanup: "+error.message);
  }
}
if(testError)throw testError;
