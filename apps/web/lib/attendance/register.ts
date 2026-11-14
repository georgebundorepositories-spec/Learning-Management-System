/** Display and validate school attendance dates in Zimbabwe local time. */
export const attendanceStatuses=["present","absent","late","excused"] as const;
export type AttendanceStatus=(typeof attendanceStatuses)[number];
export type AttendanceRow={
 class_id:string;student_user_id:string;status:string;
};
export type Enrolment={class_id:string;user_id:string};

export function isCalendarDate(value:string):boolean{
 if(!/^\d{4}-\d{2}-\d{2}$/.test(value))return false;
 const [year,month,day]=value.split("-").map(Number);
 if(year<2000||year>2099)return false;
 const date=new Date(Date.UTC(year,month-1,day));
 return date.getUTCFullYear()===year&&date.getUTCMonth()===month-1&&date.getUTCDate()===day;
}

export function harareCalendarDate(instant:Date=new Date()):string{
 const parts=new Intl.DateTimeFormat("en-GB",{
  timeZone:"Africa/Harare",year:"numeric",month:"2-digit",day:"2-digit"
 }).formatToParts(instant);
 const part=(type:string)=>parts.find(item=>item.type===type)?.value??"";
 return [part("year"),part("month"),part("day")].join("-");
}

export function attendanceKey(classId:string,studentId:string):string{
 return classId+"\u0000"+studentId;
}

/** Never count foreign-class rows or a second record for the same enrolment. */
export function attendanceForEnrolments(enrolments:readonly Enrolment[],marks:readonly AttendanceRow[]){
 const enrolmentKeys=new Set(enrolments.map(e=>attendanceKey(e.class_id,e.user_id)));
 const recorded=new Map<string,AttendanceStatus>();
 for(const mark of marks){
  const key=attendanceKey(mark.class_id,mark.student_user_id);
  if(enrolmentKeys.has(key)&&attendanceStatuses.some(status=>status===mark.status))
   recorded.set(key,mark.status as AttendanceStatus);
 }
 return {recorded,enrolled:enrolmentKeys.size,marked:recorded.size,
  remaining:Math.max(0,enrolmentKeys.size-recorded.size)};
}
