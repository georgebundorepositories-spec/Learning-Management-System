export type LoginIdentifier = { type: "email" | "phone"; value: string };

/** Normalizes common Zimbabwean mobile-number formats to E.164. */
export function parseLoginIdentifier(input: string): LoginIdentifier {
  const value = input.trim();
  if (/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return { type: "email", value };
  const compact = value.replace(/[\s()-]/g, "");
  const normalized = /^07[1-8]\d{7}$/.test(compact)
    ? `+263${compact.slice(1)}`
    : /^2637[1-8]\d{7}$/.test(compact)
      ? `+${compact}`
      : compact;
  if (!/^\+2637[1-8]\d{7}$/.test(normalized)) {
    throw new Error("Enter a valid Zimbabwe mobile number or email address");
  }
  return { type: "phone", value: normalized };
}
