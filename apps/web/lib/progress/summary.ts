/**
 * Progress is a read-only snapshot of authorised Supabase records, not
 * an examination result or an offline-device sync. Never count missing
 * marks as zero or average percentages with different denominators.
 */
export type SchoolAssignment={id:string;class_id:string;max_score:number};
export type SubmittedAnswer={id:string;assignment_id:string;student_user_id:string};
export type TeacherReview={submission_id:string;score:number};
export type SchoolAssessment={id:string;class_id:string;max_score:number};
export type SchoolMark={assessment_id:string;student_user_id:string;score:number};
export type AttendanceRecord={student_user_id:string;status:string};

export type ProgressSnapshot={
  learnerId:string;classId:string;
  assigned:number;submitted:number;reviewed:number;pending:number;
  courseworkPercent:number|null;
  assessmentCount:number;markedAssessments:number;assessmentPercent:number|null;
  attendancePresent:number;attendanceRecorded:number;
};

export function percentOfMarked(rows:{score:number;maxScore:number}[]):number|null{
  const valid=rows.filter(row=>Number.isFinite(row.score) &&
    Number.isFinite(row.maxScore) && row.maxScore>0 &&
    row.score>=0 && row.score<=row.maxScore);
  if(!valid.length)return null;
  const score=valid.reduce((n,x)=>n+x.score,0);
  const max=valid.reduce((n,x)=>n+x.maxScore,0);
  return Math.round(score/max*100);
}

export function progressSnapshot(
  learnerId:string,
  classId:string,
  assignments:SchoolAssignment[],
  submissions:SubmittedAnswer[],
  reviews:TeacherReview[],
  assessments:SchoolAssessment[],
  marks:SchoolMark[],
  attendance:AttendanceRecord[]
):ProgressSnapshot{
  const classTasks=assignments.filter(a=>a.class_id===classId);
  const taskMap=new Map(classTasks.map(a=>[a.id,a]));
  const ownSubmissions=submissions.filter(s=>
    s.student_user_id===learnerId && taskMap.has(s.assignment_id));
  const ownByAssignment=new Map(ownSubmissions.map(s=>[s.assignment_id,s]));
  const reviewBySubmission=new Map(reviews.map(r=>[r.submission_id,r]));
  const reviewedScores=classTasks.flatMap(task=>{
    const s=ownByAssignment.get(task.id);
    const r=s && reviewBySubmission.get(s.id);
    return r?[{score:Number(r.score),maxScore:Number(task.max_score)}]:[];
  });
  const classAssessments=assessments.filter(a=>a.class_id===classId);
  const assessmentMap=new Map(classAssessments.map(a=>[a.id,a]));
  const marked=marks.filter(m=>m.student_user_id===learnerId &&
    assessmentMap.has(m.assessment_id)).map(m=>({
      score:Number(m.score),
      maxScore:Number(assessmentMap.get(m.assessment_id)!.max_score)
    }));
  const ownAttendance=attendance.filter(a=>a.student_user_id===learnerId);
  return {
    learnerId,classId,assigned:classTasks.length,
    submitted:ownByAssignment.size,
    reviewed:reviewedScores.length,
    pending:Math.max(0,classTasks.length-ownByAssignment.size),
    courseworkPercent:percentOfMarked(reviewedScores),
    assessmentCount:classAssessments.length,
    markedAssessments:marked.length,
    assessmentPercent:percentOfMarked(marked),
    attendancePresent:ownAttendance.filter(a=>a.status==="present").length,
    attendanceRecorded:ownAttendance.length,
  };
}
