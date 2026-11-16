import {describe,expect,it} from "vitest";
import guidedA from "../../../../packages/curriculum/source-guided-a.json";
import guidedB from "../../../../packages/curriculum/source-guided-b.json";
import {curriculumUnits,curriculumAssignmentInstructions,getCurriculumUnit} from "./catalog";

const units=[...guidedA.units,...guidedB.units];

describe("source-guided original learning packs",()=>{
  it("includes 25 new unique source-mapped ECD and junior lessons",()=>{
    expect(units).toHaveLength(25);
    expect(new Set(units.map(x=>x.id)).size).toBe(units.length);
    expect(curriculumUnits).toHaveLength(88);
    for(const unit of units){
      expect(getCurriculumUnit(unit.id)).toBeDefined();
      expect(unit.syllabusSource).toMatch(/^MoPSE /);
      expect(unit.textbookReference.length).toBeGreaterThan(35);
    }
  });
  it("provides substantial explanation, quizzes, extended practice and applied marking",()=>{
    for(const unit of units){
      expect(unit.explanation.length).toBeGreaterThanOrEqual(4);
      expect(unit.explanation.every(p=>p.length>110)).toBe(true);
      expect(unit.workedExample.length).toBeGreaterThan(90);
      expect(unit.exercises.length).toBeGreaterThanOrEqual(5);
      expect(unit.exercises.every(e=>e.question.length>9&&e.answer.length>0)).toBe(true);
      expect(unit.choices).toHaveLength(3);
      expect(unit.correctIndex).toBeGreaterThanOrEqual(0);
      expect(unit.correctIndex).toBeLessThan(3);
      expect(unit.assignment.maxScore).toBe(20);
      expect(unit.assignment.rubric).toHaveLength(4);
      expect(unit.assignment.instructions.length).toBeGreaterThan(120);
    }
  });
  it("keeps marking keys out of published class assignment tasks",()=>{
    for(const item of units){
      const unit=getCurriculumUnit(item.id)!;
      const publicInstructions=curriculumAssignmentInstructions(unit);
      for(const e of item.exercises){
        expect(publicInstructions).toContain(e.question);
        if(e.answer.length>48)expect(publicInstructions).not.toContain(e.answer);
      }
      expect(publicInstructions).not.toContain("Teacher marking guide");
      expect(publicInstructions).not.toContain("Expected response:");
    }
  });
  it("covers all supplied textbook levels and strands with original examples",()=>{
    for(const [level,subject] of [
      ["ECD A","Science and Technology"],
      ["Grade 1","English Language"],
      ["Grade 4","English Language"],
      ["Grade 5","English Language"],
      ["Grade 5","Mathematics"],
      ["Grade 5","Indigenous Languages"],
    ]){
      expect(units.some(u=>u.level===level&&u.subject===subject)).toBe(true);
    }
  });
});
