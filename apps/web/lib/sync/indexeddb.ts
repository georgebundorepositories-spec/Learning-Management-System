"use client";

import type { CalendarRow, QueuedOp, SyncCursor, SyncOp } from "./protocol";
import type { SyncPort } from "./reconcile";
import { advanceCursor, validateOp } from "./protocol";
import { projectCalendarRows } from "./reconcile";

const DATABASE_VERSION = 1;
const INITIAL_CURSOR = "0";

function request<T>(r: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    r.onsuccess = () => resolve(r.result);
    r.onerror = () => reject(r.error ?? new Error("IndexedDB request failed"));
  });
}

/**
 * Durable, user-bound browser store. No plaintext secrets or student PII should
 * be written here until the browser cache encryption design has been reviewed.
 * Server snapshots are separate from queued optimistic edits; pulling a page
 * cannot overwrite changes that have not been acknowledged by the server.
 */
export class BrowserSyncStore implements SyncPort {
  private database: Promise<IDBDatabase>;
  private constructor(
    private readonly userId: string,
    private readonly schoolId: string,
  ) {
    const dbName = `zimschool-sync-v1:${userId}:${schoolId}`;
    this.database = new Promise((resolve, reject) => {
      const open = indexedDB.open(dbName, DATABASE_VERSION);
      open.onerror = () => reject(open.error ?? new Error("Offline storage unavailable"));
      open.onblocked = () => reject(new Error("Close other school tabs to upgrade offline storage"));
      open.onupgradeneeded = () => {
        const db = open.result;
        const queue = db.createObjectStore("queue", { keyPath: "seq", autoIncrement: true });
        queue.createIndex("byOpId", "op_id", { unique: true });
        queue.createIndex("byEntityId", "entity_id");
        db.createObjectStore("events", { keyPath: "id" });
        db.createObjectStore("meta", { keyPath: "key" });
      };
      open.onsuccess = () => {
        open.result.onversionchange = () => open.result.close();
        resolve(open.result);
      };
    });
  }

  static open(userId: string, schoolId: string): BrowserSyncStore {
    const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    if (!uuid.test(userId) || !uuid.test(schoolId)) throw new Error("Authenticated user and school required");
    if (typeof indexedDB === "undefined") throw new Error("Offline storage unavailable in this browser");
    return new BrowserSyncStore(userId, schoolId);
  }

  private async transact<T>(
    names: string[],
    mode: IDBTransactionMode,
    work: (tx: IDBTransaction) => Promise<T>,
  ): Promise<T> {
    const db = await this.database;
    return new Promise<T>((resolve, reject) => {
      const tx = db.transaction(names, mode);
      let value: T;
      let workError: unknown;
      tx.onabort = () => reject(workError ?? tx.error ?? new Error("Offline transaction rolled back"));
      tx.onerror = () => reject(tx.error ?? new Error("Offline transaction failed"));
      tx.oncomplete = () => resolve(value);
      void work(tx).then(result => { value = result; }).catch(error => {
        workError = error;
        try { tx.abort(); } catch { reject(error); }
      });
    });
  }

  async enqueue(op: SyncOp): Promise<void> {
    validateOp(op);
    if ("school_id" in op.payload || "approved_by" in op.payload ||
        "approval_status" in op.payload || "is_published" in op.payload) {
      throw new Error("Server-owned fields cannot be queued");
    }
    await this.transact(["queue"], "readwrite", async tx => {
      await request(tx.objectStore("queue").add({ ...op, status: "queued" } satisfies QueuedOp));
    });
  }

  async getQueued(limit: number): Promise<QueuedOp[]> {
    return this.transact(["queue"], "readonly", async tx => {
      const all = await request(tx.objectStore("queue").getAll() as IDBRequest<QueuedOp[]>);
      return all.filter(op => op.status === "queued").slice(0, limit);
    });
  }

  async getAllQueued(): Promise<QueuedOp[]> {
    return this.transact(["queue"], "readonly", async tx =>
      request(tx.objectStore("queue").getAll() as IDBRequest<QueuedOp[]>));
  }

  async getDeviceId(): Promise<string> {
    return this.transact(["meta"], "readwrite", async tx => {
      const meta = tx.objectStore("meta");
      const entry = await request(meta.get("device_id") as IDBRequest<{key:string;value:string}|undefined>);
      if (entry?.value) return entry.value;
      const value = crypto.randomUUID();
      await request(meta.put({ key: "device_id", value }));
      return value;
    });
  }

  async getCursor(): Promise<SyncCursor> {
    return this.transact(["meta"], "readonly", async tx => {
      const item = await request(tx.objectStore("meta").get("cursor") as IDBRequest<{key:string;value:string}|undefined>);
      return item?.value ?? INITIAL_CURSOR;
    });
  }

  async acknowledge(
    op: QueuedOp,
    result: { result: "applied" | "conflict" | "rejected"; server_row?: CalendarRow; conflict_detail?: unknown },
  ): Promise<void> {
    await this.transact(["queue", "events"], "readwrite", async tx => {
      const queue = tx.objectStore("queue");
      const key = await request(queue.index("byOpId").getKey(op.op_id));
      if (key === undefined) return; // Already acknowledged by concurrent tab/retry.
      const current = await request(queue.get(key) as IDBRequest<QueuedOp>);
      if (current.status !== "queued") return;

      if (result.result === "applied") {
        if (!result.server_row) throw new Error("Server acknowledgement is missing the authoritative row");
        if (result.server_row.school_id !== this.schoolId || result.server_row.id !== op.entity_id) {
          throw new Error("Server returned mismatched tenant or entity");
        }
        await request(tx.objectStore("events").put(result.server_row));
        await request(queue.delete(key));
        // Rebase the next queued edit on the new server version in the same transaction.
        const siblings = await request(queue.index("byEntityId").getAll(op.entity_id) as IDBRequest<QueuedOp[]>);
        const next = siblings.filter(x => x.status === "queued" && x.op_type !== "insert")
          .sort((a, b) => (a.seq ?? 0) - (b.seq ?? 0))[0];
        if (next) await request(queue.put({ ...next, base_version: result.server_row.sync_version }));
      } else {
        await request(queue.put({
          ...current,
          status: result.result,
          conflict_detail: result.conflict_detail ?? { reason: result.result },
        }));
      }
    });
  }

  async applyPage(rows: CalendarRow[], nextCursor: SyncCursor): Promise<void> {
    if (rows.some(row => row.school_id !== this.schoolId)) {
      throw new Error("Cross-school sync payload refused");
    }
    await this.transact(["events", "meta"], "readwrite", async tx => {
      const meta = tx.objectStore("meta");
      const existing = await request(meta.get("cursor") as IDBRequest<{key:string;value:string}|undefined>);
      const cursor = advanceCursor(existing?.value ?? INITIAL_CURSOR, nextCursor);
      const events = tx.objectStore("events");
      for (const row of rows) await request(events.put(row));
      await request(meta.put({ key: "cursor", value: cursor }));
    });
  }

  async visibleCalendar(): Promise<CalendarRow[]> {
    const [baseRows, queue] = await Promise.all([
      this.transact(["events"], "readonly", async tx =>
        request(tx.objectStore("events").getAll() as IDBRequest<CalendarRow[]>)),
      this.getAllQueued(),
    ]);
    return projectCalendarRows(baseRows, queue);
  }

  async close(): Promise<void> { (await this.database).close(); }
}
