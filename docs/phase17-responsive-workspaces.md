# Phase 17 — Real responsive and interactive school workflows

**Branch:** `integration/zimschool-web-android-v05` — draft [PR #10](https://github.com/georgebundorepositories-spec/Learning-Management-System/pull/10). **Not a deployed production release.**

## Web changes

- The authenticated dashboard has a real task launcher instead of a static list of cards. Search updates immediately; category filters narrow the *server-authorised* modules; links navigate to the existing working school routes.
- The dashboard adapts to large desktop, tablet and narrow phone widths. Buttons and inputs use practical minimum 44px/48px targets, accessible labels, visible keyboard focus, and reduced-motion overrides. Users can press `/` to focus tool search (when not typing in another field) and Escape to clear it.
- Modules use `prefetch={false}` to avoid making requests for every dashboard destination on low-bandwidth connections. No role or school records are stored in the launcher or sent to a search service.
- The school membership manager keeps class and homeroom selectors synced with the **currently selected** member/class so the user sees the existing value before submitting; unchanged edits are disabled. Guardian creation and revocation each require explicit checked confirmation **on the form AND server**, rather than a cosmetic instruction. Database RLS remains the real permission boundary.
- Web unit tests confirm search never reveals a role-restricted destination and that guardian confirmation is required. Next.js typecheck, test and build run on every web-related change.

## Android v0.18

- The Compose dashboard uses available device width measured in **dp**: 1 column below 600dp, 2 from 600dp, 3 from 900dp. Module cards have usable accessible tap targets; longer labels/descriptions can wrap instead of disappearing.
- Search available dashboard tools instantly, filter categories, clear filters and open modules. Every result is a real navigation destination already authorised for the current role, not a fake interaction.
- Teacher/learner/guardian names can be searched in permitted directories without additional network requests or a plaintext search cache. The teacher attendance class selector now uses a searchable, lazily rendered horizontal list instead of showing hundreds of chips vertically.
- On app resume, previous private school content is hidden **before** asynchronous role verification; a revoked staff role or guardian access will not remain displayed while a slow network re-check is running. Offline curriculum remains available.
- Unit tests check role privacy, search matching, 500-class filtering and dp layout breakpoints. Android 8 and 10 emulator tests navigate through actual filtered destination cards and verify the no-results/clear-search flow.

## Acceptance tasks still requiring the owner and real devices

These are **unverified until signed off**; automated emulator checks are not actual user testing:

1. Use Android 8 phones with 320dp widths, standard ~390dp phones, tablets at 600–899dp, and >=900dp landscape devices. Confirm every card is readable and tappable at 200% system font scale.
2. On web, verify 320px, 375px, 768px, 1024px and 1440px viewports, touch interaction, keyboard navigation and reduced-motion settings using an actually deployed HTTPS site.
3. Test search and navigation with real appropriately consented staff, learner and parent accounts. Confirm no unauthorised module is displayed and server RLS rejects direct denied URLs.
4. Test saving teacher assignments, learner enrolment, marking and guardian linking with approved records. After a change, refresh and verify a real database row changed. Revoke guardian access; confirm the former guardian can no longer read the learner’s records.
5. Switch a running app to background, deactivate a test role on the server, then resume it on slow 2G. Ensure private screen content is removed immediately and offline curriculum still works.
6. Record accessibility and Zimbabwean English/Shona/Ndebele UX feedback, and confirm offline attendance/finance is NOT falsely advertised as synced.

**Infrastructure blockers remain:** no configured Vercel deployment/verified public website, real school invitations, persistent owner-signed Android release, provider integrations or privacy approvals. The final release gate will keep the full production launch blocked until verified.
