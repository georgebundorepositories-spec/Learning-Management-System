import {describe,it,expect} from "vitest";
import {invitationPasswordError} from "./invitation-password";

describe("new member password setup",()=>{
 it("rejects weak and too-short passwords",()=>{
  expect(invitationPasswordError("password")).not.toBeNull();
  expect(invitationPasswordError("justlowercase1234")).not.toBeNull();
  expect(invitationPasswordError("ONLYUPPERCASE1234")).not.toBeNull();
  expect(invitationPasswordError("JustAlphabeticVeryLong")).not.toBeNull();
 });
 it("accepts an adequately long varied password",()=>{
  expect(invitationPasswordError("MyNewSchoolPass2026!")).toBeNull();
 });
 it("rejects overlong inputs",()=>{
  expect(invitationPasswordError("A"+("a".repeat(127))+"2")).not.toBeNull();
 });
});
