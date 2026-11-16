import { describe, expect, it } from "vitest";
import { parseLoginIdentifier,zimbabweMobilePattern } from "./validation";

describe("parseLoginIdentifier", () => {
  it("provides a valid browser-side pattern for the exact Zimbabwe E.164 number", () => {
    const browser = new RegExp(`^(?:${zimbabweMobilePattern})import { describe, expect, it } from "vitest";
import { parseLoginIdentifier,zimbabweMobilePattern } from "./validation";

);
    expect(browser.test("+263771234567")).toBe(true);
    expect(browser.test("+263781234567")).toBe(true);
    expect(browser.test("+263671234567")).toBe(false);
    expect(browser.test("+26377123456")).toBe(false);
    expect(browser.test("+2637712345678")).toBe(false);
  });
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
