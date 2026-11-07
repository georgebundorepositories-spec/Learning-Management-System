// P0-T11: one-time, operator-run bootstrap; no secrets or personal data committed.
// Usage: load the ignored environment file and run: node scripts/create-admin.mjs
import { createClient } from "@supabase/supabase-js";

function env(name) {
  const value = process.env[name]?.trim();
  if (!value) throw new Error("Missing required environment variable: " + name);
  return value;
}
const url = process.env.SUPABASE_URL || env("NEXT_PUBLIC_SUPABASE_URL");
const secret = env("SUPABASE_SECRET_KEY");
if (!secret.startsWith("sb_secret_") && !secret.startsWith("eyJ")) {
  throw new Error("Use a private Supabase secret key, not a publishable key");
}
const schoolName = env("ZIMLMS_SCHOOL_NAME");
const fullName = env("ZIMLMS_ADMIN_FULL_NAME");
const phone = env("ZIMLMS_ADMIN_PHONE");
const email = process.env.ZIMLMS_ADMIN_EMAIL?.trim() || undefined;
const password = env("ZIMLMS_ADMIN_PASSWORD");
const emisNumber = process.env.ZIMLMS_EMIS_NUMBER?.trim() || undefined;
if (!/^\+2637[1-8]\d{7}$/.test(phone)) throw new Error("Admin phone must use +2637... E.164 format");
if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) throw new Error("Invalid admin email");
if (password.length < 12) throw new Error("Use an administrator password of at least 12 characters");
if (schoolName.length < 3 || fullName.length < 3) throw new Error("School and admin names are required");

const supabase = createClient(url, secret, {
  auth: { autoRefreshToken: false, persistSession: false }
});
let createdSchoolId = null;
let createdAuthId = null;
try {
  if (emisNumber) {
    const { data: duplicate, error: findError } = await supabase.from("schools")
      .select("id").eq("emis_number", emisNumber).maybeSingle();
    if (findError) throw findError;
    if (duplicate) throw new Error("EMIS number already exists; refusing duplicate school");
  }
  const { data: school, error: schoolError } = await supabase.from("schools")
    .insert({ name: schoolName, emis_number: emisNumber ?? null, locale_default: "en",
              timezone: "Africa/Harare", currency: "USD" })
    .select("id").single();
  if (schoolError || !school) throw schoolError || new Error("School creation failed");
  createdSchoolId = school.id;

  const { data: account, error: userError } = await supabase.auth.admin.createUser({
    phone, email, password, phone_confirm: true, email_confirm: Boolean(email), user_metadata: { display_name: fullName }
  });
  if (userError || !account.user) throw userError || new Error("Auth creation failed");
  createdAuthId = account.user.id;

  const { error: profileError } = await supabase.from("users").insert({
    id: createdAuthId, school_id: createdSchoolId, phone, email: email ?? null, full_name: fullName,
    role: "admin", preferred_lang: "en", is_active: true
  });
  if (profileError) throw profileError;

  console.log("Admin bootstrap complete.");
  console.log("School ID:", createdSchoolId);
  console.log("Admin identity provisioned; no password printed.");
} catch (error) {
  // Best-effort compensation. A failed cleanup is reported explicitly.
  const cleanupErrors = [];
  if (createdAuthId) {
    const { error: authError } = await supabase.auth.admin.deleteUser(createdAuthId);
    if (authError) cleanupErrors.push("Auth cleanup failed");
  }
  if (createdSchoolId) {
    const { error: schoolError } = await supabase.from("schools").delete().eq("id", createdSchoolId);
    if (schoolError) cleanupErrors.push("School cleanup failed");
  }
  console.error("Bootstrap failed:", error instanceof Error ? error.message : "unknown error");
  if (cleanupErrors.length) console.error(cleanupErrors.join("; "));
  process.exitCode = 1;
}
