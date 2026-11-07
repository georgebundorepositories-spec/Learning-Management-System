// Phase 0: local-only phone/password Auth acceptance test.
// The temporary identity is deleted even if the test fails.
// Never use this script with production or non-local URLs.
import { createClient } from "@supabase/supabase-js";
import { randomBytes } from "node:crypto";
import assert from "node:assert/strict";

const url = process.env.SUPABASE_URL;
const secret = process.env.SUPABASE_SECRET_KEY;
const publishable = process.env.SUPABASE_PUBLISHABLE_KEY;
const safeLocal = /^http:\/\/(localhost|127\.0\.0\.1):\d+\/?$/.test(url ?? "");
if (!safeLocal) throw new Error("Refusing non-local Supabase Auth integration test");
if (!secret || !publishable) throw new Error("Local API keys are required");

const server = createClient(url, secret, {
  auth: { autoRefreshToken: false, persistSession: false }
});
const publicClient = createClient(url, publishable, {
  auth: { autoRefreshToken: false, persistSession: false }
});
const phone = "+263771000987";
const password = "Ci!A9" + randomBytes(16).toString("hex");
let schoolId = null;
let userId = null;
async function result(promise, label) {
  const { data, error } = await promise;
  if (error) throw new Error(label + ": " + error.message);
  return data;
}

try {
  const school = await result(
    server.from("schools").insert({ name: "CI Synthetic Phone Auth School" }).select("id").single(),
    "Create synthetic school"
  );
  schoolId = school.id;

  const created = await result(
    server.auth.admin.createUser({ phone, password, phone_confirm: true }),
    "Provision synthetic phone account"
  );
  assert.ok(created.user?.id, "Supabase Auth created a user");
  userId = created.user.id;

  await result(server.from("users").insert({
    id: userId, phone, school_id: schoolId,
    full_name: "CI Synthetic Phone User", role: "teacher"
  }), "Provision tenant membership");

  const session = await result(
    publicClient.auth.signInWithPassword({ phone, password }),
    "Phone/password login"
  );
  assert.equal(session.user?.id, userId, "Phone password login resolves correct user");

  const own = await result(
    publicClient.from("users").select("id,school_id,role").eq("id", userId).single(),
    "Tenant-bound profile read"
  );
  assert.equal(own.school_id, schoolId);
  assert.equal(own.role, "teacher");
  console.log("PASS: real local Supabase phone+password Auth, tenant profile read");
} finally {
  await publicClient.auth.signOut();
  if (userId) {
    const { error } = await server.auth.admin.deleteUser(userId);
    if (error) console.error("Synthetic phone identity cleanup failed");
  }
  if (schoolId) {
    const { error } = await server.from("schools").delete().eq("id", schoolId);
    if (error) console.error("Synthetic phone school cleanup failed");
  }
}
