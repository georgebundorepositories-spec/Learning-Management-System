import {describe,expect,it} from "vitest";
import {
 curriculumUnits,curriculumGrades,curriculumSubjects,
 getCurriculumUnit,gradeMatches,subjectMatches,curriculumAssignmentInstructions
} from "./catalog";

describe("Heritage-Based Curriculum content",()=>{
 it("provides 47 complete original units across nine levels and four subjects",()=>{
  expect(curriculumUnits).toHaveLength(47);
  expect(new Set(curriculumUnits.map(x=>x.id)).size).toBe(47);
  expect(curriculumGrades).toEqual(["ECD A","ECD B","Grade 1","Grade 2","Grade 3","Grade 4","Grade 5","Grade 6","Grade 7"]);
  expect(new Set(curriculumSubjects)).toEqual(new Set([
   "English Language","Science and Technology","Indigenous Languages","Mathematics"
  ]));
  for(const unit of curriculumUnits) {
    expect(unit.objective.length).toBeGreaterThan(15);
    expect(unit.explanation).toHaveLength(2);
    expect(unit.explanation.every(p=>p.length>60)).toBe(true);
    expect(unit.exercises).toHaveLength(3);
    expect(unit.exercises.every(e=>e.question&&e.answer)).toBe(true);
    expect(unit.choices).toHaveLength(3);
    expect(unit.correctIndex).toBeGreaterThanOrEqual(0);
    expect(unit.correctIndex).toBeLessThan(3);
    expect(unit.assignment.maxScore).toBe(10);
    expect(unit.assignment.rubric).toHaveLength(3);
    expect(unit.syllabusSource).toContain("MoPSE");
  }
 });
 it("does not allow a curriculum template to be assigned to the wrong grade",()=>{
  expect(gradeMatches("Grade 5","Grade 5","5A")).toBe(true);
  expect(gradeMatches("Grade 5","","Grade 5 A")).toBe(true);
  expect(gradeMatches("Grade 5","Grade 4","Grade 5 A")).toBe(false);
  expect(gradeMatches("ECD B","","ECD B Yellow")).toBe(true);
  expect(gradeMatches("Grade 3","","Primary")).toBe(false);
 });
 it("matches only eligible school subjects and recognised aliases",()=>{
  expect(subjectMatches("English Language","English")).toBe(true);
  expect(subjectMatches("Science and Technology","Science & Technology")).toBe(true);
  expect(subjectMatches("Mathematics","Maths")).toBe(true);
  expect(subjectMatches("Indigenous Languages","Shona")).toBe(true);
  expect(subjectMatches("Mathematics","English")).toBe(false);
 });
 it("publishes only learner-visible instructions and exercises, not answer keys",()=>{
  const unit=getCurriculumUnit("grade-6-mathematics-whole-numbers-to-one-million");
  expect(unit).toBeDefined();
  const instructions=curriculumAssignmentInstructions(unit!);
  expect(instructions).toContain("Practice:");
  expect(instructions).toContain(unit!.assignment.instructions);
  expect(instructions).toContain(unit!.exercises[0].question);
  expect(instructions).not.toContain("Answer key");
  expect(instructions).not.toContain("Teacher marking guide");
 });
});
