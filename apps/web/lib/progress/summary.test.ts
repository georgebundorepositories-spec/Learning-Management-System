import {describe,expect,it} from "vitest";
import {percentOfMarked,progressSnapshot} from "./summary";

describe("read-only, permission-scoped learning progress",()=>{
 it("does not treat a missing grade as zero",()=>{
  expect(percentOfMarked([])).toBe(null);
  expect(percentOfMarked([{score:8,maxScore:10},{score:18,maxScore:20}])).toBe(87);
  expect(percentOfMarked([{score:10,maxScore:0}])).toBe(null);
 });
 it("isolates class assignments and learner submissions without cross-school mixing",()=>{
  const row=progressSnapshot(
   "learner-a","class-a",
   [{id:"task-1",class_id:"class-a",max_score:20},
    {id:"task-2",class_id:"class-a",max_score:10},
    {id:"task-foreign",class_id:"class-b",max_score:10}],
   [{id:"answer-1",assignment_id:"task-1",student_user_id:"learner-a"},
    {id:"answer-stranger",assignment_id:"task-2",student_user_id:"learner-b"}],
   [{submission_id:"answer-1",score:15},
    {submission_id:"answer-stranger",score:10}],
   [{id:"exam-a",class_id:"class-a",max_score:50},
    {id:"exam-b",class_id:"class-b",max_score:100}],
   [{assessment_id:"exam-a",student_user_id:"learner-a",score:40},
    {assessment_id:"exam-b",student_user_id:"learner-a",score:90},
    {assessment_id:"exam-a",student_user_id:"learner-b",score:10}],
   [{student_user_id:"learner-a",status:"present"},
    {student_user_id:"learner-a",status:"absent"},
    {student_user_id:"learner-b",status:"present"}]
  );
  expect(row).toEqual({
   learnerId:"learner-a",classId:"class-a",
   assigned:2,submitted:1,reviewed:1,pending:1,courseworkPercent:75,
   assessmentCount:1,markedAssessments:1,assessmentPercent:80,
   attendancePresent:1,attendanceRecorded:2
  });
 });
 it("shows no average before the first reviewed piece of work",()=>{
  const row=progressSnapshot("learner","grade-3",
   [{id:"a",class_id:"grade-3",max_score:10}],[],[],
   [{id:"test",class_id:"grade-3",max_score:100}],[],[]);
  expect(row.courseworkPercent).toBeNull();
  expect(row.assessmentPercent).toBeNull();
  expect(row.pending).toBe(1);
  expect(row.attendanceRecorded).toBe(0);
 });
});
