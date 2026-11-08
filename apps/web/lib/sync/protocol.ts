/**
 * Phase 1 sync protocol (Part E). Calendar-only initial allowlist.
 * New entity names require explicit RLS, conflict semantics and tests.
 */
export const SYNC_ENTITY = "calendar_events" as const;
export type SyncEntity = typeof SYNC_ENTITY;
export type OpType = "insert" | "update" | "delete";
export type OpStatus = "queued" | "conflict" | "rejected";
export type SyncResultCode = "applied" | "conflict" | "rejected";
export type SyncCursor = string; // decimal PostgreSQL bigint, never a client clock
export interface CalendarRow {
  id: string;
  school_id: string;
  title: string;
  description: string | null;
  start_at: string;
  end_at: string;
  location: string | null;
  approval_status: "draft" | "pending" | "approved" | "rejected";
  is_published: boolean;
  deleted_at: string | null;
  sync_version: number;
  created_by: string;
  [key: string]: unknown;
}
export interface SyncOp {
  op_id: string;
  entity: SyncEntity;
  entity_id: string;
  op_type: OpType;
  payload: Record<string, unknown>;
  client_ts: string;
  base_version: number | null; // Mandatory for update/delete. Version-based CAS.
}
export interface QueuedOp extends SyncOp {
  status: OpStatus;
  conflict_detail?: unknown;
  seq?: number;
}
export interface PushRequest {
  device_id: string;
  client_version: string;
  ops: SyncOp[];
}
export interface PushOutcome {
  op_id: string;
  result: SyncResultCode;
  server_row?: CalendarRow;
  conflict_detail?: unknown;
}
export interface PushResponse {
  results: PushOutcome[];
  server_cursor: SyncCursor;
}
export interface PullResponse {
  changes: { calendar_events: CalendarRow[] };
  next_cursor: SyncCursor;
  has_more: boolean;
}
export function validCursor(value: string): boolean {
  return /^(0|[1-9]\d{0,18})$/.test(value) &&
    BigInt(value) <= BigInt("9223372036854775807");
}
export function advanceCursor(current: SyncCursor, incoming: SyncCursor): SyncCursor {
  if (!validCursor(current) || !validCursor(incoming)) throw new Error("Invalid sync cursor");
  if (BigInt(incoming) < BigInt(current)) throw new Error("Regressive sync cursor");
  return incoming;
}
export function validateOp(op: SyncOp): void {
  if (op.entity !== SYNC_ENTITY) throw new Error("Entity not yet supported");
  if (!["insert", "update", "delete"].includes(op.op_type)) throw new Error("Invalid operation");
  if (!/^[0-9a-f]{8}-[0-9a-f-]{27,}$/i.test(op.op_id) ||
      !/^[0-9a-f]{8}-[0-9a-f-]{27,}$/i.test(op.entity_id)) {
    throw new Error("Operation and entity IDs must be UUIDs");
  }
  if (!Number.isFinite(Date.parse(op.client_ts))) throw new Error("Invalid timestamp");
  if (op.op_type !== "insert" && (!Number.isInteger(op.base_version) || op.base_version! < 1)) {
    throw new Error("Updates and deletes require a server base version");
  }
  if (!op.payload || typeof op.payload !== "object" || Array.isArray(op.payload)) {
    throw new Error("Operation payload must be a JSON object");
  }
}
