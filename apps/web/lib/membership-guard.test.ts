import {describe,expect,it} from "vitest";
import {confirmedGuardianAction} from "./membership-guard";
describe("guardian relationship confirmation",()=>{
 it("requires the actual checked form field, not an inert visual label",()=>{
  const blank=new FormData();
  expect(confirmedGuardianAction(blank,"link")).toBe(false);
  expect(confirmedGuardianAction(blank,"revoke")).toBe(false);
  blank.set("guardian_verified","yes");
  expect(confirmedGuardianAction(blank,"link")).toBe(true);
  expect(confirmedGuardianAction(blank,"revoke")).toBe(false);
  blank.set("revocation_confirmed","true");
  expect(confirmedGuardianAction(blank,"revoke")).toBe(false);
  blank.set("revocation_confirmed","yes");
  expect(confirmedGuardianAction(blank,"revoke")).toBe(true);
 });
});
