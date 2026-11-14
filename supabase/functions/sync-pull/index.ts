import { cors, json, jwtClient } from "../_shared/sync.ts";

/**
 * RLS-scoped, sequence-ordered calendar delta. Other entities are not yet
 * enabled: they require separate entity-specific access-control tests.
 */
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { status: 204, headers: cors });
  if (req.method !== "GET") return json({ error: "method_not_allowed" }, 405);
  const auth = jwtClient(req);
  if (!auth) return json({ error: "unauthorized" }, 401);
  const { data: identity, error: authError } = await auth.client.auth.getUser(auth.token);
  if (authError || !identity.user) return json({ error: "unauthorized" }, 401);

  const url = new URL(req.url);
  const start = url.searchParams.get("cursor") ?? "0";
  const entities = url.searchParams.get("entities") ?? "calendar_events";
  const limitRaw = url.searchParams.get("limit") ?? "500";
  if (!/^(0|[1-9]\d{0,18})$/.test(start) ||
      BigInt(start) > BigInt("9223372036854775807") ||
      entities !== "calendar_events" || !/^[1-9]\d{0,2}$/.test(limitRaw)) {
    return json({ error: "invalid_cursor_or_entity" }, 400);
  }
  const limit = Number(limitRaw);
  if (limit > 500) return json({ error: "limit_exceeded" }, 400);

  const { data, error } = await auth.client.rpc("sync_pull_calendar", {
    p_cursor: start, p_limit: limit
  });
  if (error || !data) return json({ error: "temporarily_unavailable" }, 503);
  return json(data);
});
