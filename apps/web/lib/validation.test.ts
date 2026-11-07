import { describe, expect, it } from "vitest";
import { parseLoginIdentifier } from "./validation";

describe("parseLoginIdentifier", () => {
  it("accepts E.164 Zimbabwe mobile numbers", () => {
    expect(parseLoginIdentifier("+263771234567")).toEqual({type:"phone",value:"+263771234567"});
  });
  it("normalizes local numbers", () => {
    expect(parseLoginIdentifier("077 123 4567").value).toBe("+263771234567");
  });
  it("accepts email sign-in for provisioned users", () => {
    expect(parseLoginIdentifier(" parent@example.org ")).toEqual({type:"email",value:"parent@example.org"});
  });
  it("rejects invalid mobile numbers", () => {
    expect(() => parseLoginIdentifier("1234")).toThrow();
  });
});
