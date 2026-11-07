// P0-T12: Local Supabase Auth + RLS integration smoke test.
// Runs only against ephemeral localhost. Never contact a production project.
import { createClient } from "@supabase/supabase-js";
import assert from "node:assert/strict";

const baseUrl = process.env.SUPABASE_URL;
const secret = process.env.SUPABASE_SECRET_KEY;
const publishable = process.env.SUPABASE_PUBLISHABLE_KEY;
if (!baseUrl?.startsWith("http://127.0.0.1:") && !baseUrl?.startsWith("http://localhost:")) {
  throw new Error("This test refuses to run against a non-local Supabase instance");
}
if (!secret || !publishable) throw new Error("Missing local API keys");
const admin = createClient(baseUrl, secret, {auth:{autoRefreshToken:false,persistSession:false}});
const createdUsers = [];
const createdSchools = [];

const users = [
  { school:"A", name:"Admin A", role:"admin", email:"admin-a@example.invalid", phone:"+263771000101", password:"CI-SchoolA-Admin!9292" },
  { school:"A", name:"Teacher A", role:"teacher", email:"teacher-a@example.invalid", phone:"+263771000102", password:"CI-SchoolA-Teacher!9292" },
  { school:"B", name:"Admin B", role:"admin", email:"admin-b@example.invalid", phone:"+263771000103", password:"CI-SchoolB-Admin!9292" }
];
const ids = {};
async function check(result, context) {
  if (result.error) throw new Error(context + ": " + result.error.message);
  return result.data;
}
try {
  for (const school of ["A","B"]) {
    const data = await check(await admin.from("schools")
      .insert({ name:"CI Synthetic School "+school, emis_number:"TEST-LOCAL-"+school })
      .select("id").single(), "Create school");
    createdSchools.push(data.id); ids[school] = data.id;
  }

  for (const u of users) {
    const response = await check(await admin.auth.admin.createUser({
      phone:u.phone, email:u.email, password:u.password, phone_confirm:true, email_confirm:true
    }), "Create test identity");
    assert.ok(response.user?.id, "Auth returns an id");
    createdUsers.push(response.user.id);
    await check(await admin.from("users").insert({
      id:response.user.id, school_id:ids[u.school], phone:u.phone, email:u.email,
      full_name:u.name, role:u.role, preferred_lang:"en"
    }), "Provision role");
    u.id = response.user.id;
  }

  for (const u of users) {
    const client = createClient(baseUrl, publishable, {
      auth:{autoRefreshToken:false,persistSession:false}
    });
    const session = await check(await client.auth.signInWithPassword({
      email:u.email,password:u.password
    }), "Sign in via email + password");
    assert.equal(session.user?.id, u.id, "Correct authenticated account");
    const schoolResult = await check(await client.from("schools").select("id,name"),
      "Tenant-scoped school read");
    assert.deepEqual(schoolResult.map(s => s.id), [ids[u.school]],
      "Authenticated users cannot read another school's profile");
    const profileResult = await check(await client.from("users").select("id,role"),
      "Tenant-scoped profiles");
    assert.ok(profileResult.some(p=>p.id===u.id), "Own user profile visible");
    assert.ok(!profileResult.some(p=>users.filter(x=>x.school!==u.school).some(x=>x.id===p.id)),
      "No cross-school users visible");

    if (u.role==="teacher") {
      await client.from("users").update({role:"admin"}).eq("id",u.id);
      const raw = await check(await admin.from("users").select("role").eq("id",u.id).single(),
        "Verify role did not change");
      assert.equal(raw.role,"teacher","Teacher cannot elevate own role");
    }
    await client.auth.signOut();
  }
  console.log("PASS: 3 real email Auth logins; school isolation; self-promotion denied");
  // Phone/password is intentionally a separate test once the SMS/phone provider is enabled.
  // Never silently substitute passing email auth for a phone-auth acceptance test.
} finally {
  // Cleanup takes place even when assertions fail.
  for (const id of createdUsers.reverse()) {
    const { error }=await admin.auth.admin.deleteUser(id);
    if (error) console.error("Local auth cleanup failed:",error.message);
  }
  for (const id of createdSchools.reverse()) {
    const { error }=await admin.from("schools").delete().eq("id",id);
    if (error) console.error("Local school cleanup failed:",error.message);
  }
}
