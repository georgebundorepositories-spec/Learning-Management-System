# ZimSchool LMS — current security baseline (11 November 2026)

**Development implementation is passing CI; production handling of minors' records is not yet approved.**

- Hosted database: 29 public application tables; all have row-level security, checked 11 November 2026. School identity and subject/guardian permissions are governed by Postgres policies, not by client menu visibility.
- Previously excessive `anon`/`authenticated` grants for TRUNCATE, REFERENCES, TRIGGER and PostgreSQL MAINTAIN have been revoked. A hosted SQL check returned zero exposed application tables for those privileges.
- JWT-backed local tests use seven temporary identities to enforce school/subject isolation, grade ceilings, guardian link revocation, disabled-role behaviour and teaching restrictions. The 11 November database workflow passed.
- No service-role key or signing secret may be compiled into Android, made public through `NEXT_PUBLIC_` web variables or committed to GitHub.
- The offline PIN uses the device Keystore; the cloud role is verified online before private modules are reopened from PIN unlock. Any cached private marks, family roster, invoices or teacher plans are deliberately **not** persisted in plaintext offline Room tables.
- The Phase 1 sync backend allowlists calendar events only and protects writes using user JWT and database RPC. More offline entities must have independent tests before inclusion.
- The public `/demo` route contains fictional school data and no live identities; real private records must never be exposed there.

### Unresolved security and compliance gates

1. Hosted Supabase **leaked-password protection is disabled**, per Security Advisor. Enable through Auth settings; the app connector has no supported operation to change that setting.
2. Hosted `invite-school-member` is still the earlier v1 while the development invitation code requires a **verified HTTPS site** and trusted redirect configuration. No real emailed invite acceptance has been verified.
3. No confirmed live active school teacher/student/parent accounts. Do not activate dummy accounts or create school recipients without true consent.
4. No production website, persistent owner-signed Android APK, physical-device 72-hour session/offline acceptance, privacy/retention signoff, or independently tested backup restore.
5. Payment settlement/receipt verification, SMS/USSD provider security, remote PII storage and long-term reporting controls remain separate work until real providers are configured.

See [final-phase release runbook](final-phase-release-runbook.md) and [GitHub draft PR #10](https://github.com/georgebundorepositories-spec/Learning-Management-System/pull/10). **Never disable RLS to work around an access error.**
