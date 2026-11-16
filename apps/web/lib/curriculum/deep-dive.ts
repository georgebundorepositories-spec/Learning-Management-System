import deepDiveCatalog from "../../../../packages/curriculum/deep-dive.json";

export type DeepDiveItem = {question:string;answer:string};
export type CurriculumDeepDive = {
  id:string;
  starter:string;
  vocabulary:[string,string][];
  steps:[string,string][];
  worked:string[];
  guided:[string,string,string][];
  practice:[string,string][];
  misconception:string;
  application:string;
  recap:string[];
};
export const deepDiveVersion=deepDiveCatalog.version;
export const deepDiveCount=deepDiveCatalog.units.length;
export const deepDiveUnits:CurriculumDeepDive[]=deepDiveCatalog.units as CurriculumDeepDive[];
const byId=new Map<string,CurriculumDeepDive>(deepDiveUnits.map(item=>[item.id,item]));
export function getDeepDive(id:string):CurriculumDeepDive|undefined {
  return byId.get(id);
}
export function deepDiveAssignmentPrompts(id:string):string[] {
  const lesson=getDeepDive(id);
  if (!lesson) return [];
  return [
    "Independent practice (show all steps or explanations):",
    ...lesson.practice.map(([question],i)=>`${i+1}. ${question}`),
    "",
    "Community or practical application:",
    lesson.application
  ];
}
