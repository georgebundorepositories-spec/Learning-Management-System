import rawCatalog from "../../../../packages/curriculum/catalog.json";

export type CurriculumExercise={question:string;answer:string};
export type CurriculumUnit={
 id:string;level:string;subject:string;topic:string;title:string;objective:string;
 durationMinutes:number;explanation:string[];workedExample:string;
 question:string;choices:string[];correctIndex:number;feedback:string;
 exercises:CurriculumExercise[];
 assignment:{title:string;instructions:string;maxScore:number;rubric:string[]};
 syllabusSource:string;mediaActivity:string;
};
export const curriculumUnits:CurriculumUnit[]=rawCatalog.units as CurriculumUnit[];
export const curriculumVersion=rawCatalog.catalogVersion;
export const curriculumNotice=rawCatalog.licenseNotice;
export const curriculumGrades=[...new Set(curriculumUnits.map(unit=>unit.level))];
export const curriculumSubjects=[...new Set(curriculumUnits.map(unit=>unit.subject))];

export function getCurriculumUnit(id:string):CurriculumUnit|undefined{
 return curriculumUnits.find(unit=>unit.id===id);
}
export function canonicalGrade(value:string):string{
 const low=value.trim().toLowerCase();
 const ecd=low.match(/ecd\s*([ab])/);
 if(ecd)return "ECD "+ecd[1].toUpperCase();
 const grade=low.match(/(?:grade\s*|^)([1-7])(?:\b|$)/);
 return grade?"Grade "+grade[1]:"";
}
export function gradeMatches(unitLevel:string,classGrade:string,className:string):boolean{
 const detected=canonicalGrade(classGrade)||canonicalGrade(className);
 return !!detected&&detected===unitLevel;
}
export function subjectMatches(unitSubject:string,schoolSubject:string):boolean{
 const unit=unitSubject.trim().toLowerCase();
 const school=schoolSubject.trim().toLowerCase();
 if(unit===school)return true;
 const aliases:Record<string,string[]>={
  "english language":["english","english language"],
  "science and technology":["science","science and technology","science & technology"],
  "mathematics":["maths","mathematics","math"],
  "indigenous languages":["indigenous languages","shona","ndebele","kalanga","tonga","venda","nambya","sotho","xhosa","chewa","tswana","chibarwe","khoisan","sign language"]
 };
 return (aliases[unit]??[]).includes(school);
}
export function curriculumAssignmentInstructions(unit:CurriculumUnit):string{
 return [
  "Curriculum topic: "+unit.topic,
  "Learning objective: "+unit.objective,
  "",
  "Practice:",
  ...unit.exercises.map((item,index)=> (index+1)+". "+item.question),
  "",
  "Class assignment:",
  unit.assignment.instructions,
  "",
  "Please answer in your own words. Do not include private family or contact information."
 ].join("\n");
}
