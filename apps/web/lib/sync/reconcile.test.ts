import { describe, expect, it } from "vitest";
import { advanceCursor, type CalendarRow, type QueuedOp, type SyncCursor, type SyncOp } from "./protocol";
import { projectCalendarRows, runSync, type SyncPort, type SyncTransport } from "./reconcile";

const schoolId = "11111111-1111-4111-8111-111111111111";
const deviceA = "22222222-2222-4222-8222-222222222222";
const deviceB = "33333333-3333-4333-8333-333333333333";
const oldTime = new Date(Date.now() - 73 * 60 * 60 * 1000).toISOString();
function id(i: number): string {
  return `44444444-4444-4444-8444-${String(i).padStart(12, "0")}`;
}
function op(i: number): SyncOp {
  return {
    op_id: id(200 + i),
    entity: "calendar_events", entity_id: id(i),
    op_type: "insert", base_version: null,
    client_ts: oldTime,
    payload: { title: `Offline event ${i}`, start_at: "2026-12-01T08:00:00Z",
      end_at: "2026-12-01T09:00:00Z" }
  };
}
function makeRow(item: SyncOp): CalendarRow {
  return {
    id: item.entity_id, school_id: schoolId, title: String(item.payload.title),
    description: null, start_at: String(item.payload.start_at),
    end_at: String(item.payload.end_at), location: null,
    approval_status: "pending", is_published: false,
    deleted_at: null, sync_version: 1, created_by: deviceA
  };
}
class MemoryPort implements SyncPort {
  queue: QueuedOp[] = [];
  rows = new Map<string, CalendarRow>();
  cursor = "0";
  getQueued(limit: number): Promise<QueuedOp[]> {
    return Promise.resolve(this.queue.filter(op => op.status === "queued").slice(0, limit).map(x => ({ ...x })));
  }
  getCursor(): Promise<SyncCursor> { return Promise.resolve(this.cursor); }
  acknowledge(op: QueuedOp, result: { result: "applied"|"conflict"|"rejected"; server_row?: CalendarRow; conflict_detail?: unknown }): Promise<void> {
    const at = this.queue.findIndex(x => x.op_id === op.op_id);
    if (at < 0) return Promise.resolve();
    if (result.result === "applied" && result.server_row) {
      this.rows.set(result.server_row.id, result.server_row);
      this.queue.splice(at, 1);
      const next = this.queue.find(x => x.entity_id === op.entity_id && x.status === "queued" && x.op_type !== "insert");
      if (next) next.base_version = result.server_row.sync_version;
    } else this.queue[at] = { ...this.queue[at], status: result.result as "conflict"|"rejected", conflict_detail: result.conflict_detail };
    return Promise.resolve();
  }
  applyPage(rows: CalendarRow[], next: string): Promise<void> {
    this.cursor = advanceCursor(this.cursor, next);
    rows.forEach(row => this.rows.set(row.id, row));
    return Promise.resolve();
  }
}
class FakeServer {
  private seq = 0;
  readonly rows = new Map<string, CalendarRow>();
  readonly log: Array<{ seq: number; row: CalendarRow }> = [];
  readonly applied = new Map<string, CalendarRow>();
  transport(): SyncTransport {
    return {
      push: async item => {
        const prior = this.applied.get(item.op_id);
        if (prior) return { result: "applied", server_row: prior };
        if (this.rows.has(item.entity_id)) return { result: "conflict", conflict_detail: { reason: "exists" } };
        const row = makeRow(item);
        this.rows.set(row.id, row);
        this.applied.set(item.op_id, row);
        this.log.push({ seq: ++this.seq, row });
        return { result: "applied", server_row: row };
      },
      pull: async (cursor, limit) => {
        const all = this.log.filter(event => event.seq > Number(cursor));
        const page = all.slice(0, limit);
        return { rows: page.map(x => x.row), nextCursor: String(page.at(-1)?.seq ?? cursor),
          hasMore: all.length > page.length };
      }
    };
  }
}

describe("Phase 1 offline sync contract", () => {
  it("two devices replay 100 edits after 72h without loss or duplicate rows", async () => {
    const server = new FakeServer();
    const a = new MemoryPort(), b = new MemoryPort();
    a.queue = Array.from({ length: 50 }, (_, index) => ({ ...op(index + 1), seq: index + 1, status: "queued" }));
    b.queue = Array.from({ length: 50 }, (_, index) => ({ ...op(index + 51), seq: index + 1, status: "queued" }));
    const transport = server.transport();
    const first = await runSync(a, transport);
    const second = await runSync(b, transport);
    const converge = await runSync(a, transport);
    expect(first.pushed).toBe(50);
    expect(second.pushed).toBe(50);
    expect(converge.pulled).toBe(50);
    expect(a.queue).toHaveLength(0);
    expect(b.queue).toHaveLength(0);
    expect(server.rows.size).toBe(100);
    expect(a.rows.size).toBe(100);
    expect(b.rows.size).toBe(100);
    expect(a.cursor).toBe("100");
    expect(b.cursor).toBe("100");
  });

  it("replayed operation IDs do not duplicate writes", async () => {
    const server = new FakeServer();
    const transport = server.transport();
    const first = op(301);
    const res1 = await transport.push(first);
    const res2 = await transport.push(first);
    expect(res1).toEqual(res2);
    expect(server.rows.size).toBe(1);
  });

  it("optimistic edits never mutate the server snapshot and remain visible offline", () => {
    const base = makeRow(op(11));
    const local: QueuedOp[] = [
      { ...op(11), op_type: "update", base_version: 1, payload: { title: "Changed offline" }, status: "queued", seq: 1 }
    ];
    expect(projectCalendarRows([base], local)[0].title).toBe("Changed offline");
    expect(base.title).toBe("Offline event 11");
  });

  it("failed operations remain in the queue and are not silently projected", () => {
    const base = makeRow(op(12));
    const local: QueuedOp[] = [
      { ...op(12), op_type: "delete", base_version: 1, payload: {}, status: "conflict", seq: 1 }
    ];
    expect(projectCalendarRows([base], local)).toHaveLength(1);
  });

  it("rejects cursor regression rather than skipping server changes", () => {
    expect(() => advanceCursor("123", "122")).toThrow();
    expect(() => advanceCursor("123", "124")).not.toThrow();
  });
});
