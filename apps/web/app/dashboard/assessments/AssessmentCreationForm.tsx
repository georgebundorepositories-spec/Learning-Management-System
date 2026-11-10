"use client";

import { useState } from "react";
import { createAssessment } from "./actions";

type ClassRecord = { id: string; name: string; teacher_id: string | null };
type SubjectRecord = { id: string; name: string };
type TeachingAssignment = { class_id: string; subject_id: string };

export default function AssessmentCreationForm({
  classes, subjects, assignments, teacherId, isTeacher,
}: {
  classes: ClassRecord[];
  subjects: SubjectRecord[];
  assignments: TeachingAssignment[];
  teacherId: string;
  isTeacher: boolean;
}) {
  const [classId, setClassId] = useState(classes[0]?.id ?? "");
  const chosen = classes.find(c => c.id === classId);
  const availableSubjects = !isTeacher || chosen?.teacher_id === teacherId
    ? subjects
    : subjects.filter(s => assignments.some(a =>
        a.class_id === classId && a.subject_id === s.id));

  return <form action={createAssessment} className="resource-form">
    <label>Class
      <select name="class_id" value={classId}
        onChange={event => setClassId(event.target.value)} required>
        {classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
      </select>
    </label>
    <label>Subject
      <select name="subject_id" key={classId} required
        disabled={availableSubjects.length === 0}>
        {availableSubjects.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
      </select>
    </label>
    <label>Assessment title
      <input name="title" minLength={3} maxLength={160} required />
    </label>
    <label>Date
      <input type="date" name="assessment_date" required />
    </label>
    <label>Maximum score
      <input name="max_score" type="number" min="0.01" max="10000"
        step="0.01" required />
    </label>
    <button type="submit" className="outline-button"
      disabled={!classId || availableSubjects.length === 0}>
      Create assessment
    </button>
    {classId && availableSubjects.length === 0 &&
      <p role="status" className="privacy-note">
        No subjects assigned to you in this class. Ask your school administrator.
      </p>}
  </form>;
}
