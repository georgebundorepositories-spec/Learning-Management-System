"use client";

import { createClient } from "@/lib/supabase/client";
import type { CalendarRow, SyncOp } from "./protocol";
import { validCursor } from "./protocol";
import type { SyncTransport } from "./reconcile";

const endpoint = process.env.NEXT_PUBLIC_SUPABASE_URL;
const apiKey = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY;

type TokenResult = { token: string };
async function authenticatedToken(): Promise<TokenResult> {
  const supabase = createClient();
  const { data, error } = await supabase.auth.getSession();
  if (error || !data.session?.access_token) {
    throw new Error("Sign in online before synchronising offline changes");
  }
  return { token: data.session.access_token };
}
async function edgeCall(path: string, init: RequestInit): Promise<unknown> {
  if (!endpoint?.startsWith("https://") || !apiKey?.startsWith("sb_publishable_")) {
    throw new Error("Sync server is not configured");
  }
  const { token } = await authenticatedToken();
  const response = await fetch(`${endpoint.replace(/\/$/, "")}/functions/v1/${path}`, {
    ...init, headers: {
      "apikey": apiKey,
      "Authorization": `Bearer ${token}`,
      ...(init.body ? { "Content-Type": "application/json" } : {}),
    },
    cache: "no-store",
  });
  if (!response.ok) {
    // Network errors, expired sessions and provider outages retain queue entries.
    throw new Error(`Sync server returned ${response.status}; queued changes were not discarded`);
  }
  return response.json();
}

export function createSyncTransport(deviceId: string): SyncTransport {
  if (!/^[0-9a-f-]{36}$/i.test(deviceId)) throw new Error("Invalid device ID");
  return {
    async push(op: SyncOp) {
      const data = await edgeCall("sync-push", {
        method: "POST", body: JSON.stringify({
          device_id: deviceId, client_version: "0.2.0-p1", ops: [op]
        }),
      }) as { results?: Array<{
        op_id: string; result: "applied" | "conflict" | "rejected";
        server_row?: CalendarRow; conflict_detail?: unknown
      }> };
      const outcome = data?.results?.[0];
      if (!outcome || outcome.op_id !== op.op_id ||
          !["applied", "conflict", "rejected"].includes(outcome.result)) {
        throw new Error("Unexpected sync acknowledgement; queue retained");
      }
      return outcome;
    },
    async pull(cursor: string, limit: number) {
      if (!validCursor(cursor)) throw new Error("Invalid cursor");
      const path = `sync-pull?cursor=${encodeURIComponent(cursor)}&entities=calendar_events&limit=${limit}`;
      const data = await edgeCall(path, { method: "GET" }) as {
        changes?: { calendar_events?: CalendarRow[] };
        next_cursor?: string; has_more?: boolean;
      };
      const rows = data?.changes?.calendar_events;
      if (!Array.isArray(rows) || typeof data.next_cursor !== "string" ||
          !validCursor(data.next_cursor) || typeof data.has_more !== "boolean") {
        throw new Error("Invalid pull response; cursor was not advanced");
      }
      return { rows, nextCursor: data.next_cursor, hasMore: data.has_more };
    },
  };
}
