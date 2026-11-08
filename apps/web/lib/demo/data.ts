/**
 * Synthetic presentation fixtures. All values are fictitious.
 * The matching structural schools/classes/subjects are seeded with
 * scripts/seed-fictional-schools.sql. No real person or enrolment data.
 */
export type DemoSchool = {
  id: string;
  name: string;
  type: "Primary" | "Secondary";
  district: string;
  province: string;
  language: string;
  classes: string[];
  subjects: string[];
  events: Array<{ date: string; title: string; category: string; venue: string }>;
};
export const demoSchools: DemoSchool[] = [
  {
    id: "10000000-0000-4000-8000-000000000001",
    name: "Mkoba Learning Primary",
    type: "Primary",
    province: "Midlands", district: "Gweru",
    language: "ChiShona / English",
    classes: ["ECD A", "Grade 3A", "Grade 5A", "Grade 7A"],
    subjects: [
      "Mathematics", "English Language", "ChiShona",
      "Science and Technology", "Heritage Studies", "Physical Education and Arts"
    ],
    events: [
      { date: "10 Nov", title: "Teaching plans review", category: "Academic", venue: "Staff room" },
      { date: "13 Nov", title: "Grade 7 project exhibition", category: "Cultural", venue: "Assembly area" },
      { date: "20 Nov", title: "Inter-house athletics", category: "Sports", venue: "Sports field" },
      { date: "04 Dec", title: "End of Term 3", category: "Calendar", venue: "School-wide" }
    ],
  },
  {
    id: "10000000-0000-4000-8000-000000000002",
    name: "Gweru Learning Secondary",
    type: "Secondary",
    province: "Midlands", district: "Gweru",
    language: "English / ChiShona",
    classes: ["Form 1A", "Form 2A", "Form 3A", "Form 4A"],
    subjects: [
      "Mathematics", "English Language", "Integrated Science", "Biology",
      "Geography", "History", "Computer Science"
    ],
    events: [
      { date: "11 Nov", title: "Form 4 revision planning", category: "Academic", venue: "Library" },
      { date: "18 Nov", title: "Science club demonstration", category: "Academic", venue: "Science lab" },
      { date: "24 Nov", title: "Inter-form football fixtures", category: "Sports", venue: "Sports grounds" },
      { date: "04 Dec", title: "End of Term 3", category: "Calendar", venue: "School-wide" }
    ]
  }
] as const;

export const demoTotals = {
  schools: demoSchools.length,
  academicYears: 4,
  terms: 12,
  classes: demoSchools.reduce((sum, school) => sum + school.classes.length, 0),
  subjects: demoSchools.reduce((sum, school) => sum + school.subjects.length, 0),
};
