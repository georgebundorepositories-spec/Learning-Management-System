import {describe,expect,it} from "vitest";
import {curriculumUnits,curriculumAssignmentInstructions} from "./catalog";
import {deepDiveUnits,getDeepDive,deepDiveAssignmentPrompts} from "./deep-dive";

describe("Original deeper curriculum learning activities",()=>{
  it("extends existing curriculum rather than creating unverified syllabus units",()=>{
    expect(deepDiveUnits.length).toBeGreaterThanOrEqual(12);
    const ids=new Set(curriculumUnits.map(unit=>unit.id));
    expect(new Set(deepDiveUnits.map(unit=>unit.id)).size).toBe(deepDiveUnits.length);
    for(const item of deepDiveUnits)expect(ids.has(item.id)).toBe(true);
  });
  it("contains substantive vocabulary, explanation steps, examples and practice",()=>{
    for(const unit of deepDiveUnits){
      expect(unit.starter.length).toBeGreaterThan(60);
      expect(unit.vocabulary.length).toBeGreaterThanOrEqual(4);
      expect(unit.steps.length).toBeGreaterThanOrEqual(3);
      expect(unit.steps.every(([heading,description])=>heading.length>5&&description.length>80)).toBe(true);
      expect(unit.worked.length).toBeGreaterThanOrEqual(3);
      expect(unit.guided.length).toBeGreaterThanOrEqual(2);
      expect(unit.guided.every(([prompt,hint,answer])=>prompt.length>20&&hint.length>15&&answer.length>1)).toBe(true);
      expect(unit.practice.length).toBeGreaterThanOrEqual(2);
      expect(unit.misconception.length).toBeGreaterThan(50);
      expect(unit.application.length).toBeGreaterThan(40);
      expect(unit.recap.length).toBeGreaterThanOrEqual(3);
    }
  });
  it("keeps solutions out of student coursework instructions",()=>{
    for(const lesson of deepDiveUnits){
      const unit=curriculumUnits.find(u=>u.id===lesson.id)!;
      const result=curriculumAssignmentInstructions(unit);
      for(const [question,,answer] of lesson.guided){
        expect(result).toContain(question);
        if(answer.length>35)expect(result).not.toContain(answer);
      }
      for(const [question] of lesson.practice)expect(result).toContain(question);
      expect(result).not.toContain("Teacher marking guide");
      expect(result).not.toContain("Expected response:");
    }
  });
  it("supports guided lesson retrieval without leaking answer text in assignment prompts",()=>{
    const q=deepDiveAssignmentPrompts("grade-7-mathematics-percentages-and-discounts");
    expect(q.join(" ")).toContain("Independent practice");
    expect(q.join(" ")).toContain("Community or practical application");
    expect(q.join(" ")).not.toContain("$132; discount $18.");
    expect(getDeepDive("not-real")).toBeUndefined();
  });
});
