"use client";

import { useState } from "react";
import { createCoursework } from "./actions";

type SchoolClass = { id: string; name: string; teacher_id: string | null };
type Subject = { id: string; name: string };
type AssignedSubject = { class_id: string; subject_id: string };

export default function CourseworkCreationForm({
  classes, subjects, assignments, teacherId, isTeacher,
}: {
  classes: SchoolClass[];
  subjects: Subject[];
  assignments: AssignedSubject[];
  teacherId: string;
  isTeacher: boolean;
}) {
  const [classId, setClassId] = useState(classes[0]?.id ?? "");
  const selectedClass = classes.find(c => c.id === classId);
  const allowedSubjects = !isTeacher || selectedClass?.teacher_id === teacherId
    ? subjects
    : subjects.filter(s => assignments.some(a =>
        a.class_id === classId && a.subject_id === s.id));

  return <form action={createCoursework} className="resource-form">
    <label>Class
      <select name="class_id" required value={classId}
        onChange={event => setClassId(event.target.value)}>
        {classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
      </select>
    </label>
    <label>Subject
      <select key={classId} name="subject_id" required
        disabled={!allowedSubjects.length}>
        {allowedSubjects.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
      </select>
    </label>
    <label>Title
      <input name="title" minLength={3} maxLength={180} required />
    </label>
    <label>Due date (optional)
      <input type="date" name="due_on" />
    </label>
    <label>Maximum mark
      <input type="number" min="0.01" max="10000" step="0.01"
        name="max_score" defaultValue="100" required />
    </label>
    <label className="form-full">Instructions
      <textarea name="instructions" rows={5} minLength={3}
        maxLength={5000} required />
    </label>
    <button type="submit" className="outline-button"
      disabled={!classId || !allowedSubjects.length}>
      Publish assignment
    </button>
    {!allowedSubjects.length &&
      <p className="privacy-note" role="status">
        No assigned subjects are available in this class.
      </p>}
  </form>;
}
