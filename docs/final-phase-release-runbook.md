# Final-phase release runbook — ZimSchool LMS

**Release classification on 11 November 2026: DEVELOPMENT PASSED; FULL PRODUCTION BLOCKED.**

This runbook reflects the actual `integration/zimschool-web-android-v05` branch, not the older Phase 0 status pages. The draft integration PR targets `feature/role-portals-foundation`, **not** `main`. Neither a GitHub CI success nor a merged development PR is proof of a running website, invited real school users or a signed release APK.

## What was completed in code

- Android v0.17 Compose application: verified school login and offline PIN, curriculum lessons, local progress, school-scoped registers, notices, private results, teacher assignments, attendance, lesson planning, coursework, classroom assessments with scores and feedback, and administrator class/guardian management.
- Web Next.js role portals and fictional demonstration. A verified HTTPS site deployment has NOT been demonstrated.
- Hosted Supabase migrated tenant RLS, scoped private-data access and calendar-only sync functions. Production Data API `TRUNCATE`, `TRIGGER`, `REFERENCES`, and `MAINTAIN` privileges revoked from client roles. Current hosted project has **one active administrator and no active teachers, students or guardians**.
- Full CI for latest code revision `dba2684b` **PASSED**:
  - Android #43014992470 (API26, API29, unit tests, debug and unsigned minified)
  - Supabase #43014992462 (7 real local JWT identities; tenant isolation, marks and guardian links)
  - Web #43014992653
  - Deployed API unauthenticated rejection #43014992493 (does NOT prove web deployment).

## Strict go/no-go gate

Run GitHub Actions workflow **ZimSchool FINAL release gate (fail-closed)** manually from the development branch. It does NOT merge, publish, sign, send invitations or create users. It checks the deployment URL, presence of GitHub deployment/signing configuration, and *submitted* HTTPS links to independent evidence. Missing items cause an explicit red failure and a machine-readable `release-evidence/readiness.json` workflow artifact.

Evidence links are not certified merely by being present: the release owner must inspect actual results and approve. There are two targets:
- `limited-pilot`: does not claim SMS, USSD, payment or receipts are operational; these features must remain disabled.
- `full`: additionally requires approved payment-settlement, messaging, and USSD evidence.

## External tasks that require owner/provider credentials

1. **Vercel website.** Import the repository in Vercel, set **Root Directory** to `apps/web`, framework Next.js, and connect the intended deployment branch. Set its public `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`; never put service-role credentials in a `NEXT_PUBLIC_` variable. Set repository secrets `VERCEL_TOKEN`, `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID`. Run `.github/workflows/vercel-preview.yml` and verify the resulting HTTPS `/demo`, `/login`, `/accept-invite` pages. Record the actual root URL under repository variable `ZIMSCHOOL_WEB_ORIGIN`.
2. **Supabase invitation delivery.** Set Auth Site URL and redirect allow-list for the EXACT verified `https://<hostname>/accept-invite`; configure working SMTP. Set the trusted Edge Function secret `ZIMSCHOOL_WEB_ORIGIN` to the real HTTPS root and then deploy the repository's `supabase/functions/invite-school-member/index.ts` revision. Keep the hosted version 1 unchanged until the destination works. Test that a real consented teacher/learner/guardian receives and accepts an emailed invite, chooses their own password and reaches ONLY their authorised portal.
3. **Account and child-data safeguards.** Obtain the actual school owner's authority and guardian consent where relevant. Do not invent emails, phone numbers, school enrolments or passwords. Confirm account deactivation removes access immediately, and parent links cannot cross schools. Review backup restoration, logs, retention, confidentiality and data access requests.
4. **Android.** Store a *persistent owner-controlled keystore* in GitHub environment `android-release` using `ANDROID_KEYSTORE_BASE64`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_PASSWORD`, `ANDROID_KEY_ALIAS` as secrets; back up the keystore OFFLINE. Run the existing `android-signed-candidate.yml` with its explicit QA confirmation, verify `apksigner` certificate/fingerprint, install the resulting APK on real Android 8+ devices, and test update and rollback. Debug artifacts are for testing and signed with ephemeral CI debug keys.
5. **72-hour/2G offline.** Use two real devices with approved test accounts, leave one offline for 72 hours, test calendar edits, conflicts, tombstones, retries, session expiry, account revocation and reconnect. The current local sync engine is **calendar only**. Do not claim offline grades, finance, fees, attendance or all school CRUD are synchronised. Those features remain online-only until extended and accepted.
6. **Full product providers.** Do not claim USSD, bank/mobile payments, receipts or SMS/push are active until provider contracts, signed webhooks, durable reconciliation ledger, opt-in, retries, cost controls, refunds and real-device testing pass. A read-only invoice is not proof of payment.
7. **Final approval.** Review WCAG 2.2 AA accessibility, Shona/Ndebele/English translations, protection for minors, slow network performance, backup and restore, breach response and licence terms. Record each independent signed-off item as an HTTPS evidence link in the matching GitHub repository variable prefixed `ZIMSCHOOL_EVIDENCE_` (see workflow for exact names). After the gate passes AND owner reviews all evidence, only then review merging into production and public release.

## Never overstate

- `supabase/functions/sync-push` and `sync-pull` support calendar changes only, not all records.
- The hosted Supabase security adviser still lists **Leaked Password Protection Disabled**; fix this in hosted Auth settings.
- A successful CI check for the **deployed Phase 1 API** verifies HTTP 401 for unauthenticated requests; it does **not** verify a public web deployment.
- The draft PR is not a production branch. The default `main` branch is behind development.
- Public release must not accept real student or guardian records before the school's data-protection and approval gates pass.
