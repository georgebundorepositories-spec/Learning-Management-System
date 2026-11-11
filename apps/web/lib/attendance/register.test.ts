import {describe,expect,it} from "vitest";
import {attendanceForEnrolments,attendanceKey,harareCalendarDate,isCalendarDate} from "./register";

describe("school attendance register",()=>{
 it("uses Africa/Harare rather than UTC at midnight",()=>{
  expect(harareCalendarDate(new Date("2026-11-11T21:59:00Z"))).toBe("2026-11-11");
  expect(harareCalendarDate(new Date("2026-11-11T22:00:00Z"))).toBe("2026-11-12");
 });
 it("rejects impossible dates and accepts leap-day dates",()=>{
  expect(isCalendarDate("2028-02-29")).toBe(true);
  for(const date of ["2026-02-29","2026-13-01","2026-11-31","2026-1-1","bad"])
   expect(isCalendarDate(date)).toBe(false);
 });
 it("never treats a learner as marked just because another class marked them",()=>{
  const enrolments=[{class_id:"one",user_id:"sam"},{class_id:"two",user_id:"sam"},{class_id:"one",user_id:"tari"}];
  const state=attendanceForEnrolments(enrolments,[
   {class_id:"one",student_user_id:"sam",status:"absent"},
   {class_id:"one",student_user_id:"sam",status:"absent"},
   {class_id:"foreign",student_user_id:"sam",status:"present"},
   {class_id:"two",student_user_id:"sam",status:"unknown"}
  ]);
  expect(state).toMatchObject({enrolled:3,marked:1,remaining:2});
  expect(state.recorded.get(attendanceKey("one","sam"))).toBe("absent");
  expect(state.recorded.has(attendanceKey("two","sam"))).toBe(false);
 });
});
