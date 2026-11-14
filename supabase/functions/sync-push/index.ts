import { cors, json, jwtClient, uuid } from "../_shared/sync.ts";
type CalendarOp = {
  op_id: string; entity: "calendar_events"; entity_id: string;
  op_type: "insert" | "update" | "delete"; client_ts: string;
  base_version: number | null; payload: Record<string, unknown>;
};

/**
 * Every write passes through the allowlisted DB RPC. Never call .from(op.entity),
 * never accept a caller-supplied school_id, never use service_role for user writes.
 */
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { status: 204, headers: cors });
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const auth = jwtClient(req);
  if (!auth) return json({ error: "unauthorized" }, 401);
  const { data: identity, error: authError } = await auth.client.auth.getUser(auth.token);
  if (authError || !identity.user) return json({ error: "unauthorized" }, 401);

  const maxBody = 256_000;
  if (Number(req.headers.get("content-length") ?? "0") > maxBody) {
    return json({ error: "payload_too_large" }, 413);
  }
  const raw = await req.text();
  if (new TextEncoder().encode(raw).byteLength > maxBody) {
    return json({ error: "payload_too_large" }, 413);
  }
  let body: Record<string, unknown>;
  try { body = JSON.parse(raw); } catch { return json({ error: "invalid_json" }, 400); }

  if (!uuid(body.device_id) || !Array.isArray(body.ops) || body.ops.length > 100 ||
      body.ops.some((op: CalendarOp) => !op || typeof op !== "object" ||
        !uuid(op.op_id) || !uuid(op.entity_id) || op.entity !== "calendar_events" ||
        !["insert", "update", "delete"].includes(op.op_type) ||
        typeof op.client_ts !== "string" || !Number.isFinite(Date.parse(op.client_ts)) ||
        !op.payload || typeof op.payload !== "object" || Array.isArray(op.payload) ||
        (op.op_type !== "insert" && (!Number.isInteger(op.base_version) ||
          (op.base_version ?? 0) < 1)))) {
    return json({ error: "invalid_or_unsupported_operations" }, 400);
  }

  const results: Array<Record<string, unknown>> = [];
  for (const op of body.ops as CalendarOp[]) {
    const { data, error } = await auth.client.rpc("sync_calendar_operation", {
      p_device_id: body.device_id, p_op_id: op.op_id, p_entity_id: op.entity_id,
      p_op_type: op.op_type, p_payload: op.payload, p_base_version: op.base_version,
      p_client_ts: op.client_ts,
    });
    // Transient server failures must NOT be acknowledged as a terminal rejection.
    // The client will retain the queue and retry with the exact same op_id.
    if (error || !data) return json({ error: "temporarily_unavailable" }, 503);
    results.push({ op_id: op.op_id, ...data });
  }
  const { data: watermark, error: watermarkError } =
    await auth.client.rpc("sync_current_cursor");
  if (watermarkError) return json({ error: "temporarily_unavailable" }, 503);
  return json({ results, server_cursor: watermark ?? "0" });
});
