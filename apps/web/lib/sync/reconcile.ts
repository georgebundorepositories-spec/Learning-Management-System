import type { CalendarRow, QueuedOp, SyncCursor, SyncOp } from "./protocol";
import { advanceCursor, validateOp } from "./protocol";

/**
 * Render the last acknowledged server snapshot plus the local operation log.
 * Never mutate the base snapshot or silently discard a rejected/conflicted op.
 * The queue retains every edit until an explicit outcome is committed to disk.
 */
export function projectCalendarRows(
  baseRows: readonly CalendarRow[],
  localOps: readonly QueuedOp[],
): CalendarRow[] {
  const rows = new Map(baseRows.map(row => [row.id, { ...row }]));
  for (const op of [...localOps].sort((a, b) => (a.seq ?? 0) - (b.seq ?? 0))) {
    if (op.status !== "queued") continue;
    if (op.op_type === "delete") {
      const previous = rows.get(op.entity_id);
      if (previous) rows.set(previous.id, { ...previous, deleted_at: op.client_ts });
      continue;
    }
    const previous = rows.get(op.entity_id);
    if (op.op_type === "insert") {
      rows.set(op.entity_id, {
        id: op.entity_id,
        school_id: previous?.school_id ?? "",
        title: String(op.payload.title ?? ""),
        description: typeof op.payload.description === "string" ? op.payload.description : null,
        start_at: String(op.payload.start_at ?? ""),
        end_at: String(op.payload.end_at ?? ""),
        location: typeof op.payload.location === "string" ? op.payload.location : null,
        created_by: previous?.created_by ?? "",
        approval_status: "pending",
        is_published: false,
        deleted_at: null,
        sync_version: 0,
      });
    } else if (previous) {
      // UI optimism only. Server decides allowable fields and conflict outcomes.
      const permitted = ["title", "description", "start_at", "end_at", "location"] as const;
      const patch: Record<string, unknown> = {};
      for (const field of permitted) if (Object.prototype.hasOwnProperty.call(op.payload, field)) {
        patch[field] = op.payload[field];
      }
      rows.set(op.entity_id, { ...previous, ...patch });
    }
  }
  return [...rows.values()].filter(row => row.deleted_at === null);
}

export interface SyncPort {
  getQueued(limit: number): Promise<QueuedOp[]>;
  getCursor(): Promise<SyncCursor>;
  acknowledge(op: QueuedOp, result: { result: "applied" | "conflict" | "rejected"; server_row?: CalendarRow; conflict_detail?: unknown }): Promise<void>;
  applyPage(rows: CalendarRow[], nextCursor: SyncCursor): Promise<void>;
}
export interface SyncTransport {
  push(op: SyncOp): Promise<{ result: "applied" | "conflict" | "rejected"; server_row?: CalendarRow; conflict_detail?: unknown }>;
  pull(cursor: SyncCursor, limit: number): Promise<{ rows: CalendarRow[]; nextCursor: SyncCursor; hasMore: boolean }>;
}
export async function runSync(port: SyncPort, transport: SyncTransport, maxOps = 100): Promise<{
  pushed: number; conflicted: number; rejected: number; pulled: number; cursor: string;
}> {
  let pushed = 0, conflicted = 0, rejected = 0, pulled = 0;
  // One in-flight op at a time preserves local order and makes same-entity CAS rebasing possible.
  const operations = await port.getQueued(Math.min(100, maxOps));
  for (const op of operations) {
    if (op.status !== "queued") continue;
    validateOp(op);
    const result = await transport.push(op);
    // Commit acknowledgement before sending another op. A lost response is replay-safe by op_id.
    await port.acknowledge(op, result);
    if (result.result === "applied") pushed++;
    else if (result.result === "conflict") conflicted++;
    else rejected++;
  }
  let cursor = await port.getCursor();
  let pages = 0;
  while (pages++ < 1000) {
    const page = await transport.pull(cursor, 500);
    const next = advanceCursor(cursor, page.nextCursor);
    if (page.hasMore && next === cursor) throw new Error("Server returned a non-advancing cursor");
    // Persist materialized rows and cursor together. A failed transaction will retry this page.
    await port.applyPage(page.rows, next);
    pulled += page.rows.length;
    cursor = next;
    if (!page.hasMore) return { pushed, conflicted, rejected, pulled, cursor };
  }
  throw new Error("Excessive sync pages; retry from last durable cursor");
}
