"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import { createClient } from "@/lib/supabase/client";
import { BrowserSyncStore } from "@/lib/sync/indexeddb";
import { runSync } from "@/lib/sync/reconcile";
import { createSyncTransport } from "@/lib/sync/transport";
import type { CalendarRow, QueuedOp, SyncOp } from "@/lib/sync/protocol";

type Identity = { userId: string; schoolId: string; role: "admin" | "head" | "teacher"; name: string };
type SyncStatus = "initializing" | "ready" | "syncing" | "offline" | "unavailable";

function formattedTime(value: string): string {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString("en-ZW", {
    dateStyle: "medium", timeStyle: "short", timeZone: "Africa/Harare"
  });
}

export default function CalendarWorkspacePage() {
  const storeRef = useRef<BrowserSyncStore | null>(null);
  const [identity, setIdentity] = useState<Identity | null>(null);
  const [status, setStatus] = useState<SyncStatus>("initializing");
  const [events, setEvents] = useState<CalendarRow[]>([]);
  const [operations, setOperations] = useState<QueuedOp[]>([]);
  const [message, setMessage] = useState("Loading your school calendar…");
  const [title, setTitle] = useState("");
  const [location, setLocation] = useState("");
  const [start, setStart] = useState("");
  const [end, setEnd] = useState("");
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(async (store: BrowserSyncStore) => {
    const [calendar, queue] = await Promise.all([store.visibleCalendar(), store.getAllQueued()]);
    setEvents(calendar.sort((a, b) => a.start_at.localeCompare(b.start_at)));
    setOperations(queue);
  }, []);

  const sync = useCallback(async (store: BrowserSyncStore) => {
    if (typeof navigator !== "undefined" && !navigator.onLine) {
      setStatus("offline");
      setMessage("Offline. Changes are saved on this device until you reconnect.");
      return;
    }
    setStatus("syncing");
    try {
      const deviceId = await store.getDeviceId();
      const summary = await runSync(store, createSyncTransport(deviceId));
      await refresh(store);
      setStatus("ready");
      setMessage(`Synchronised. ${summary.pushed} uploaded, ${summary.pulled} received, ${summary.conflicted} conflicts.`);
    } catch (err) {
      setStatus("unavailable");
      const text = err instanceof Error ? err.message : "Network request failed";
      setMessage(`Your local changes are safe. Sync could not finish: ${text}`);
    }
  }, [refresh]);

  useEffect(() => {
    let cancelled = false;
    let liveStore: BrowserSyncStore | null = null;
    async function initialize() {
      try {
        const client = createClient();
        const { data: auth, error: authError } = await client.auth.getUser();
        if (authError || !auth.user) throw new Error("Please sign in online to open your calendar.");
        const { data: profile, error: profileError } = await client
          .from("users").select("school_id,full_name,role,is_active")
          .eq("id", auth.user.id).maybeSingle();
        if (profileError || !profile?.is_active || !profile.school_id) {
          throw new Error("A school administrator needs to activate your school account.");
        }
        if (!["admin", "head", "teacher"].includes(profile.role)) {
          throw new Error("Phase 1 calendar editing is limited to school staff.");
        }
        const role = profile.role as Identity["role"];
        const opened = BrowserSyncStore.open(auth.user.id, profile.school_id);
        liveStore = opened;
        if (cancelled) { await opened.close(); return; }
        storeRef.current = opened;
        setIdentity({ userId: auth.user.id, schoolId: profile.school_id, role, name: profile.full_name });
        await refresh(opened);
        if (!cancelled) {
          setStatus(navigator.onLine ? "ready" : "offline");
          setMessage("Your calendar is available. Changes are queued locally before upload.");
          if (navigator.onLine) await sync(opened);
        }
      } catch (err) {
        if (!cancelled) {
          setStatus("unavailable");
          setMessage(err instanceof Error ? err.message : "Unable to initialise offline calendar.");
        }
      }
    }
    void initialize();
    return () => {
      cancelled = true;
      if (storeRef.current === liveStore) storeRef.current = null;
      if (liveStore) void liveStore.close();
    };
  }, [refresh, sync]);

  useEffect(() => {
    const reconnect = () => { const store = storeRef.current; if (store) void sync(store); };
    const disconnect = () => {
      setStatus("offline");
      setMessage("Connection lost. You can continue making offline calendar edits.");
    };
    window.addEventListener("online", reconnect);
    window.addEventListener("offline", disconnect);
    const refreshPeriod = window.setInterval(reconnect, 15 * 60 * 1000);
    return () => {
      window.removeEventListener("online", reconnect);
      window.removeEventListener("offline", disconnect);
      window.clearInterval(refreshPeriod);
    };
  }, [sync]);

  async function enqueue(op: SyncOp) {
    const store = storeRef.current;
    if (!store) throw new Error("Local storage has not been opened");
    await store.enqueue(op);
    await refresh(store);
    setMessage("Saved to this device. Awaiting server acknowledgement.");
    if (navigator.onLine) void sync(store);
  }

  async function createEvent(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy || !identity) return;
    setBusy(true);
    try {
      const startDate = new Date(start);
      const endDate = new Date(end);
      if (!Number.isFinite(startDate.getTime()) || !Number.isFinite(endDate.getTime()) ||
          endDate < startDate || !title.trim() || title.trim().length > 200) {
        throw new Error("Enter a title and a valid date range.");
      }
      await enqueue({
        op_id: crypto.randomUUID(),
        entity: "calendar_events",
        entity_id: crypto.randomUUID(),
        op_type: "insert",
        payload: {
          title: title.trim(), location: location.trim() || null,
          start_at: startDate.toISOString(), end_at: endDate.toISOString(),
          category: "academic"
        },
        base_version: null, client_ts: new Date().toISOString(),
      });
      setTitle(""); setLocation(""); setStart(""); setEnd("");
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Could not save calendar event.");
    } finally { setBusy(false); }
  }

  async function changeEvent(row: CalendarRow, kind: "update" | "delete") {
    if (!storeRef.current || !identity || row.sync_version < 1) {
      setMessage("This event must synchronise before it can be edited again.");
      return;
    }
    if (identity.role === "teacher" && row.created_by !== identity.userId) {
      setMessage("Teachers may only edit events they created.");
      return;
    }
    let payload: Record<string, unknown> = {};
    if (kind === "update") {
      const proposed = window.prompt("New title", row.title);
      if (proposed === null) return;
      if (!proposed.trim() || proposed.trim().length > 200) {
        setMessage("Event title must be between 1 and 200 characters.");
        return;
      }
      payload = { title: proposed.trim() };
    } else if (!window.confirm("Remove this event? This change is also queued if you are offline.")) {
      return;
    }
    try {
      await enqueue({
        op_id: crypto.randomUUID(), entity: "calendar_events", entity_id: row.id,
        op_type: kind, payload, client_ts: new Date().toISOString(),
        base_version: row.sync_version,
      });
    } catch (err) { setMessage(err instanceof Error ? err.message : "Could not queue edit."); }
  }

  const pendingCount = operations.filter(op => op.status === "queued").length;
  const conflictCount = operations.filter(op => op.status !== "queued").length;
  return <main className="calendar-shell">
    <header className="calendar-top">
      <Link href="/demo" className="calendar-logo"><span className="demo-monogram">Z</span> ZimSchool LMS</Link>
      <nav aria-label="Workspace navigation"><Link href="/dashboard">My dashboard</Link><Link href="/demo">Public demo</Link></nav>
    </header>
    <div className="calendar-wrap">
      <div className="calendar-heading"><div><p className="demo-label">PHASE 1 · STAFF CALENDAR</p><h1>School events & offline queue</h1>
        <p>Calendar edits are stored locally first, and synchronised only after the server accepts them.</p></div>
        <div className={status === "ready" ? "calendar-status ok" : "calendar-status"} aria-live="polite">{status === "syncing" ? "Synchronising" : status === "offline" ? "Offline" : status === "ready" ? "Connected" : "Action required"}</div>
      </div>
      <p role="status" aria-live="polite" className="calendar-message">{message}</p>
      {identity ? <>
        <div className="calendar-stats"><div><span>School member</span><strong>{identity.name}</strong><small>{identity.role}</small></div>
          <div><span>Visible events</span><strong>{events.length}</strong><small>Cached on this device</small></div>
          <div><span>Pending upload</span><strong>{pendingCount}</strong><small>Not yet acknowledged</small></div>
          <div><span>Needs review</span><strong>{conflictCount}</strong><small>Conflicts or rejected edits</small></div>
        </div>
        <div className="calendar-actions"><button type="button" onClick={() => { if (storeRef.current) void sync(storeRef.current); }} disabled={status==="syncing"}>↻ Synchronise now</button><span>Reconnect-triggered sync is enabled while this page is open.</span></div>
        <div className="calendar-columns">
          <section className="calendar-editor"><h2>Add an event</h2><p>Create a school event, even while the connection is interrupted.</p>
            <form onSubmit={createEvent}>
              <label htmlFor="event-title">Event title</label>
              <input id="event-title" maxLength={200} required value={title} onChange={e=>setTitle(e.target.value)} placeholder="e.g. Science project exhibition"/>
              <label htmlFor="event-start">Start</label>
              <input id="event-start" type="datetime-local" required value={start} onChange={e=>setStart(e.target.value)}/>
              <label htmlFor="event-end">End</label>
              <input id="event-end" type="datetime-local" required value={end} onChange={e=>setEnd(e.target.value)}/>
              <label htmlFor="event-location">Location (optional)</label>
              <input id="event-location" value={location} onChange={e=>setLocation(e.target.value)} placeholder="Assembly grounds"/>
              <button disabled={busy || status==="initializing"} type="submit">Save event locally</button>
            </form>
            <p className="calendar-footnote">Events remain pending until approved according to your school policy. This module is in staged testing.</p>
          </section>
          <section className="calendar-events"><div className="calendar-list-header"><h2>School calendar</h2><span>{events.length} visible</span></div>
            {events.length===0 ? <div className="calendar-empty">No events stored on this device yet. Create your first event or synchronise existing school records.</div> :
              <div className="calendar-event-items">{events.map(row=><article key={row.id} className="calendar-item">
                <div><span className="calendar-item-date">{formattedTime(row.start_at)}</span>
                  <h3>{row.title}</h3><small>{row.location || "No venue"} · {row.approval_status}</small></div>
                <div className="calendar-item-actions">
                  <button type="button" onClick={()=>void changeEvent(row,"update")} disabled={row.sync_version<1}>Rename</button>
                  <button type="button" onClick={()=>void changeEvent(row,"delete")} disabled={row.sync_version<1}>Remove</button>
                </div>
              </article>)}</div>}
          </section>
        </div>
        {operations.length>0 && <section className="calendar-queue"><h2>Local operation queue</h2>
          <p>Rejected or conflicting changes are retained for review, not silently deleted.</p>
          <div className="calendar-queue-list">{operations.map(op=><div key={op.op_id}><strong>{op.op_type} · {op.status}</strong><small>{op.entity_id.slice(0,8)} · {formattedTime(op.client_ts)}</small></div>)}</div>
        </section>}
      </> : <div className="calendar-login-notice"><p>Staff accounts must be provisioned through Supabase Auth before this workspace becomes available.</p><Link href="/login">Go to secure sign-in</Link></div>}
    </div>
  </main>;
}
