"use client";

import { useState } from "react";
import Link from "next/link";
import { demoSchools, demoTotals } from "@/lib/demo/data";

const categories = ["Academics", "Calendar", "Staff", "Parents", "Finance"] as const;

export default function DemoPage() {
  const [selected, setSelected] = useState(0);
  const school = demoSchools[selected];
  return (
    <main className="demo-shell">
      <header className="demo-header">
        <div className="demo-header-content">
          <Link href="/demo" className="demo-identity" aria-label="ZimSchool LMS home">
            <span className="demo-monogram">Z</span>
            <span><strong>ZimSchool</strong><small>LEARNING MANAGEMENT SYSTEM</small></span>
          </Link>
          <nav className="demo-header-links" aria-label="Site navigation">
            <a href="#schools">Schools</a>
            <a href="#modules">Platform</a>
            <Link href="/login" className="demo-header-login">Sign in <span aria-hidden>↗</span></Link>
          </nav>
        </div>
      </header>
      <section className="demo-hero">
        <div className="demo-hero-inner">
          <div className="demo-hero-copy">
            <div className="demo-kicker"><span className="demo-pulse" /> MADE FOR ZIMBABWEAN SCHOOLS</div>
            <h1>Every school day,<br /><em>better connected.</em></h1>
            <p className="demo-lead">A dependable home for calendars, classes, school operations and the people who make learning happen — designed for the reality of unreliable internet.</p>
            <div className="demo-hero-actions">
              <a href="#schools" className="demo-primary-button">Explore the demonstration <span aria-hidden>→</span></a>
              <Link href="/login" className="demo-secondary-button">School member sign-in</Link>
            </div>
            <p className="demo-hero-fine">This is a fictional demonstration. No real learner, guardian or staff records are displayed.</p>
          </div>
          <div className="demo-hero-art" aria-hidden="true">
            <div className="demo-art-orbit demo-art-orbit-one" />
            <div className="demo-art-orbit demo-art-orbit-two" />
            <div className="demo-art-panel">
              <div className="demo-art-toolbar"><span className="demo-art-dots"><i /><i /><i /></span><small>Term 3 · 2026</small></div>
              <div className="demo-art-heading">The week ahead</div>
              <div className="demo-art-event"><span className="demo-art-date">10</span><span><strong>Teaching plans review</strong><small>Academic • 09:00</small></span><b>●</b></div>
              <div className="demo-art-event"><span className="demo-art-date">13</span><span><strong>Project exhibition</strong><small>Heritage & culture</small></span><b>●</b></div>
              <div className="demo-art-event"><span className="demo-art-date">20</span><span><strong>Inter-house athletics</strong><small>Sports programme</small></span><b>●</b></div>
              <div className="demo-art-status"><span>↻</span><div><strong>Offline-first design</strong><small>Save locally. Reconnect to synchronise.</small></div></div>
            </div>
          </div>
        </div>
      </section>
      <section className="demo-data-banner" role="note">
        <span className="demo-data-icon">i</span>
        <p><strong>Demonstration environment.</strong> All school names and event details on this preview are invented. These are sample interface records, not a live view of private school data.</p>
      </section>

      <section id="schools" className="demo-section demo-schools-section">
        <div className="demo-section-heading">
          <div><span className="demo-label">THE DEMONSTRATION</span><h2>A school system that fits its community.</h2><p>Switch between two fictional Midlands schools to see how the same platform adapts to different learners and subjects.</p></div>
          <div className="demo-stage-pill"><span /> TERM 3 · 2026</div>
        </div>
        <div className="demo-overview">
          <div className="demo-stat"><span>Demo schools</span><strong>{String(demoTotals.schools).padStart(2, "0")}</strong><small>Primary & secondary</small></div>
          <div className="demo-stat"><span>Classes</span><strong>{String(demoTotals.classes).padStart(2, "0")}</strong><small>Illustrative year groups</small></div>
          <div className="demo-stat"><span>Subjects</span><strong>{demoTotals.subjects}</strong><small>ZIMSEC-oriented samples</small></div>
          <div className="demo-stat"><span>Academic terms</span><strong>{demoTotals.terms}</strong><small>2026 and 2027</small></div>
        </div>

        <div className="demo-school-picker" role="group" aria-label="Choose demonstration school">
          {demoSchools.map((s, i) => <button key={s.id} type="button" aria-pressed={selected === i} className={selected === i ? "demo-school-choice active" : "demo-school-choice"} onClick={() => setSelected(i)}>
            <span className="demo-choice-icon">{s.type === "Primary" ? "01" : "02"}</span>
            <span><strong>{s.name}</strong><small>{s.type} school · {s.district}, {s.province}</small></span>
            <span className="demo-choice-arrow">↗</span>
          </button>)}
        </div>

        <div className="demo-workspace">
          <aside className="demo-workspace-side">
            <span className="demo-label">SCHOOL PROFILE</span>
            <h3>{school.name}</h3>
            <div className="demo-profile-detail"><span>District</span><strong>{school.district}</strong></div>
            <div className="demo-profile-detail"><span>School type</span><strong>{school.type}</strong></div>
            <div className="demo-profile-detail"><span>Languages</span><strong>{school.language}</strong></div>
            <div className="demo-profile-detail"><span>Curriculum</span><strong>ZIMSEC</strong></div>
            <div className="demo-roster">
              <span className="demo-label">FICTIONAL ROSTER</span>
              <div><span><strong>{school.demoRoster.staff}</strong><small>Staff</small></span><span><strong>{school.demoRoster.learners}</strong><small>Learners</small></span><span><strong>{school.demoRoster.guardians}</strong><small>Guardians</small></span></div>
              <p>All accounts are disabled. The preview displays aggregates only.</p>
            </div>
            <div className="demo-side-note"><span aria-hidden="true">◎</span><p>Built for varied schools and shared devices, with distinct school-level access rules.</p></div>
          </aside>
          <div className="demo-workspace-main">
            <div className="demo-workspace-title"><div><span className="demo-label">ACADEMIC STRUCTURE</span><h3>Classes & subjects</h3></div><span className="demo-counter">{school.classes.length} classes</span></div>
            <div className="demo-class-grid">{school.classes.map((name, i) => <div key={name} className="demo-class-tile"><span className="demo-class-symbol">{String(i + 1).padStart(2, "0")}</span><strong>{name}</strong><small>2026 academic year</small></div>)}</div>
            <span className="demo-small-heading">SUBJECT CATALOGUE <span>{school.subjects.length} subjects</span></span>
            <div className="demo-subjects">{school.subjects.map(subject => <span key={subject}>{subject}</span>)}</div>
            <div className="demo-events-title"><div><span className="demo-label">SAMPLE CALENDAR</span><h3>What's happening next</h3></div><span className="demo-demo-pill">Sample events</span></div>
            <div className="demo-event-list">{school.events.map((event, i) => <div key={event.title} className="demo-event-row"><span className="demo-event-number">{String(i + 1).padStart(2, "0")}</span><span className="demo-event-date">{event.date}</span><div><strong>{event.title}</strong><small>{event.venue}</small></div><span className="demo-event-type">{event.category}</span></div>)}</div>
          </div>
        </div>
      </section>

      <section id="modules" className="demo-feature-section">
        <div className="demo-feature-container"><div><span className="demo-label demo-label-light">THE PLATFORM</span><h2>Built for the classroom.<br />Ready for the real world.</h2><p>Practical school tools, connected through a single secure foundation. The following is a roadmap overview, not a claim that every module is already operational.</p></div>
          <div className="demo-modules">{categories.map((name, i) => <div key={name}><span>{String(i + 1).padStart(2, "0")}</span><strong>{name}</strong><span>{i === 1 ? "Calendar synchronisation backend staged" : "Planned school module"}</span></div>)}</div>
        </div>
      </section>
      <section className="demo-sync-section">
        <div className="demo-sync-header"><span className="demo-label">OFFLINE-FIRST ROADMAP</span><h2>Connectivity shouldn't decide who can learn.</h2><p>A resilient workflow for intermittent networks. The calendar sync backend is deployed; complete offline acceptance remains in development.</p></div>
        <div className="demo-sync-flow"><div><span>01</span><strong>Keep working</strong><p>Record changes locally when internet access drops.</p></div><i aria-hidden="true">→</i><div><span>02</span><strong>Queue safely</strong><p>Retain operations until each gets a server acknowledgement.</p></div><i aria-hidden="true">→</i><div><span>03</span><strong>Reconnect & resolve</strong><p>Synchronise updates and keep conflicting edits for review.</p></div></div>
      </section>
      <footer className="demo-footer"><div className="demo-footer-inner"><div className="demo-footer-brand"><span className="demo-monogram">Z</span><span><strong>ZimSchool LMS</strong><small>Purpose-built for Zimbabwe</small></span></div><p>Fictional preview • No real pupil or staff data • Phase 1 in development</p><Link href="/login">Member sign-in →</Link></div></footer>
    </main>
  );
}
