# ZimSchool LMS — Vercel web preview and demo data

Status: Phase 1 *development preview*, not a public operational school LMS.

## Repository integration

The code is in `georgebundorepositories-spec/Learning-Management-System`. The working Phase 1 web UI is on the **`feature/phase1-offline-sync`** branch (draft PR #1). The `main` branch intentionally remains the stable baseline while release gates are incomplete.

**Connecting a GitHub account to Vercel is not the same as importing this repository as a Vercel Project.** In the Vercel dashboard, use Add New > Project > Import this repository, select **Root Directory `apps/web`**, Framework **Next.js**, and deploy. Vercel should produce a unique Preview URL for draft PR #1 after project import. No Vercel URL was verified at the time this document was committed.

### Environment variables

The only application connection settings are public:
- `NEXT_PUBLIC_SUPABASE_URL=https://mezbtcdkmvtkjxdmmhbw.supabase.co`
- `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`: current active **publishable** key, available in the checked-in `apps/web/vercel.json` (nonsecret public client identifier).

For maintainability, prefer setting both through Vercel > Project Settings > Environment Variables, and remove the checked-in `env` override after the project settings have been verified. Never provide or commit a `service_role` or `sb_secret_` key.

### Available routes

- `/`: redirects to the fictional demonstration.
- `/demo`: read-only public interface preview; all schools, events and subject examples are fictional; this route does not query sensitive school records.
- `/login`: Supabase authentication (requires a provisioned school account; none has been created for the public demo).
- `/dashboard`: server-verified school dashboard; not publicly accessible.
- `/calendar`: experimental staff-only calendar editor with per-device IndexedDB queue, automatic reconnect attempts, and the deployed JWT-protected `sync-push` and `sync-pull` functions.
- `/manifest.webmanifest`, `/sw.js`: limited app-shell caching. The service worker does not cache private dashboard or Supabase Auth requests.

### Existing data in LMS Omni

Synthetic seed script: `scripts/seed-fictional-schools.sql`.
Two `is_demo=true` fictional Midlands schools are hosted with 4 years, 12 terms, 8 classes, 13 subjects and 6 feature flags. No students, parents, or staff authentication identities were created. Don't use dummy student names as actual school records; provision real accounts through the reviewed admin process.

### Release boundaries

No authenticated hosted calendar sync test has completed because live test accounts are not yet provisioned. Android WorkManager background sync and full 72-hour mixed-entity convergence acceptance remain unfinished. Do not upload personal learner information or market this preview as fully operational. The local browser calendar queue is **not yet encrypted at rest**; do not use it for sensitive student or guardian data.

**A GitHub Actions build is not evidence of a Vercel deployment.** A verified Vercel Preview URL and its corresponding deployment status are required before claiming the website is live.
