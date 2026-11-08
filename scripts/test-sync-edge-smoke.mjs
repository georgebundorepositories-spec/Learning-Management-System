/**
 * Phase 1 real HTTP/JWT integration test: Supabase GoTrue -> Edge -> RPC -> RLS.
 * SAFETY: localhost-only, synthetic identities, disposable Supabase CLI instance.
 * Do not point at hosted LMS Omni or use production credentials.
 */
import { createClient } from "@supabase/supabase-js";
import assert from "node:assert/strict";
import { randomUUID, randomBytes } from "node:crypto";

const base = process.env.SUPABASE_URL;
const secret = process.env.SUPABASE_SECRET_KEY;
const anon = process.env.SUPABASE_PUBLISHABLE_KEY;
if (!/^http:\/\/(127\.0\.0\.1|localhost):54321\/?$/.test(base ?? "")) {
  throw new Error("Safety guard: Edge tests must run against local Supabase port 54321 only");
}
if (!secret || !anon) throw new Error("Local Supabase keys are required");
const admin = createClient(base, secret, {
  auth: { persistSession: false, autoRefreshToken: false }
});

async function checked(promise, label) {
  const { data, error } = await promise;
  if (error) throw new Error(label + ": " + error.message);
  return data;
}
async function enroll(schoolId, role, name, n) {
  const email = `edge-test-${n}@example.invalid`;
  const password = "CI!" + randomBytes(18).toString("hex");
  const created = await checked(admin.auth.admin.createUser({
    email, password, email_confirm: true
  }), "Create synthetic Auth identity");
  await checked(admin.from("users").insert({
    id: created.user.id, school_id: schoolId, phone: `synthetic-edge-${n}`,
    email, full_name: name, role, is_active: true
  }), "Provision synthetic school role");
  const client = createClient(base, anon, {
    auth: { persistSession: false, autoRefreshToken: false }
  });
  const session = await checked(client.auth.signInWithPassword({ email, password }),
    "Sign into local GoTrue");
  assert.equal(session.user?.id, created.user.id);
  assert.ok(session.session?.access_token);
  return { id: created.user.id, client, token: session.session.access_token };
}

async function edge(user, fn, method, payload = null, params = "") {
  const url = `${base.replace(/\/$/,"")}/functions/v1/${fn}${params}`;
  const response = await fetch(url, {
    method, headers: {
      "apikey": anon, "Authorization": `Bearer ${user.token}`,
      ...(payload ? { "content-type": "application/json" } : {})
    }, ...(payload ? { body: JSON.stringify(payload) } : {})
  });
  const raw = await response.text();
  let body;
  try { body = JSON.parse(raw); } catch {
    throw new Error(`${fn} returned non-JSON HTTP ${response.status}: ${raw.slice(0,220)}`);
  }
  if (!response.ok) throw new Error(`${fn} HTTP ${response.status}: ${JSON.stringify(body)}`);
  return body;
}

const oldClock = new Date(Date.now() - 73 * 60 * 60 * 1000).toISOString();
const demoSchoolName = "EDGE CI - synthetic school";
const anotherSchoolName = "EDGE CI - isolated second school";
const schoolA = (await checked(admin.from("schools").insert({ name: demoSchoolName })
  .select("id").single(), "Insert local school A")).id;
const schoolB = (await checked(admin.from("schools").insert({ name: anotherSchoolName })
  .select("id").single(), "Insert local school B")).id;

const teacherA = await enroll(schoolA, "teacher", "CI Teacher A", 1);
const teacherB = await enroll(schoolA, "teacher", "CI Teacher B", 2);
const teacherC = await enroll(schoolB, "teacher", "CI Teacher C", 3);
const deviceA = randomUUID();
const deviceB = randomUUID();

function insertOp(entityId, title, counter = null) {
  return {
    op_id: randomUUID(), entity: "calendar_events",
    entity_id: entityId, op_type: "insert", base_version: null,
    client_ts: oldClock,
    payload: {
      title, start_at: "2026-12-02T09:00:00Z",
      end_at: "2026-12-02T10:00:00Z", category: "academic",
      ...(counter ? { school_id: schoolB } : {})
    }
  };
}
async function push(user, deviceId, operations) {
  const data = await edge(user, "sync-push", "POST", {
    device_id: deviceId, client_version: "ci-integration", ops: operations
  });
  assert.equal(data.results?.length, operations.length);
  return data.results;
}
async function pull(user, cursor = "0") {
  const data = await edge(user, "sync-pull", "GET", null,
    `?cursor=${cursor}&entities=calendar_events&limit=500`);
  assert.ok(Array.isArray(data.changes?.calendar_events));
  return data;
}

// With no token, both endpoints must be rejected in the actual Edge runtime.
for (const fn of ["sync-push", "sync-pull"]) {
  const response = await fetch(`${base}/functions/v1/${fn}`);
  assert.equal(response.status, 401, "Anonymous Edge API must reject access");
}

const sharedId = randomUUID();
const created = insertOp(sharedId, "CI calendar private to Teacher A", 1);
const first = (await push(teacherA,deviceA,[created]))[0];
assert.equal(first.result, "applied");
assert.equal(first.server_row.school_id,schoolA);
const retry = (await push(teacherA,deviceA,[created]))[0];
assert.equal(retry.result, "applied", "Same op_id is idempotent");
assert.equal(retry.server_row.id, sharedId);

// Another teacher in same school has no right to read this event via
// a deliberate insert collision. This checks the deployed security hotfix.
const collision = (await push(teacherB,deviceB,[insertOp(sharedId,"CI collision attack")]))[0];
assert.equal(collision.result,"rejected");
assert.ok(collision.server_row == null, "No other teacher event data leaked");
const teacherBEvents = await pull(teacherB);
assert.equal(teacherBEvents.changes.calendar_events.length,0);

// Teacher C belongs to school B: both read and update must be denied.
const crossSchool = (await push(teacherC,deviceB,[{
  ...insertOp(sharedId,"CI foreign write"), op_type:"update",
  base_version:1,payload:{title:"FORBIDDEN"}
}]))[0];
assert.equal(crossSchool.result,"rejected");
assert.ok(crossSchool.server_row == null);
assert.equal((await pull(teacherC)).changes.calendar_events.length,0);

const stale=(await push(teacherA,deviceA,[{
  ...insertOp(sharedId,"CI stale"),op_type:"update",base_version:999,
  payload:{title:"stale overwrite"}
}]))[0];
assert.equal(stale.result,"conflict");
const latest=(await push(teacherA,deviceA,[{
  ...insertOp(sharedId,"CI current"),op_type:"update",base_version:1,
  payload:{title:"CI updated"}
}]))[0];
assert.equal(latest.result,"applied");
assert.equal(latest.server_row.sync_version,2);

const deleted=(await push(teacherA,deviceA,[{
  ...insertOp(sharedId,"CI delete"),op_type:"delete",base_version:2,payload:{}
}]))[0];
assert.equal(deleted.result,"applied");
assert.ok(deleted.server_row.deleted_at, "Delete must be represented by tombstone");

// Two logical devices with the SAME authenticated teacher identity,
// each with 50 distinct edits created with timestamps older than 72 hours.
const deviceOne = Array.from({length:50},(_,i)=>insertOp(randomUUID(), `CI device A event ${i+1}`));
const deviceTwo = Array.from({length:50},(_,i)=>insertOp(randomUUID(), `CI device B event ${i+1}`));
for (const [device,ops] of [[deviceA,deviceOne],[deviceB,deviceTwo]]) {
  // Keep batches small to approximate reconnect with limited bandwidth.
  for (let i=0;i<ops.length;i+=10) {
    const outcomes=await push(teacherA,device,ops.slice(i,i+10));
    assert.ok(outcomes.every(x=>x.result==="applied"));
  }
}
const viewOne=await pull(teacherA,"0");
const viewTwo=await pull(teacherA,"0");
const idsOne=viewOne.changes.calendar_events.map(x=>x.id);
const idsTwo=viewTwo.changes.calendar_events.map(x=>x.id);
assert.deepEqual(idsOne,idsTwo,"Both logical devices converge");
assert.equal(new Set(idsOne).size,101,"One tombstoned event plus 100 new events (latest row per entity)");
assert.equal(viewOne.changes.calendar_events.filter(x=>x.deleted_at).length,1);
assert.ok(viewOne.changes.calendar_events.length>=103,"Pull preserves all change versions");
assert.equal(viewOne.has_more,false);

// Verify idempotency did not create an extra change (insert + update + delete + 100).
const { count, error: countError }=await admin.from("sync_change_log")
  .select("seq",{count:"exact",head:true}).eq("school_id",schoolA);
if(countError)throw countError;
assert.equal(count,103,"Replay must not append duplicate server changes");
console.log("PASS: Auth JWT -> real Edge RPC, school privacy, CAS, 72h-old logical 50+50 replay and tombstones");
