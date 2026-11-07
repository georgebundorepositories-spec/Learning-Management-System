# Security baseline (Phase 0)

ZimSchool LMS processes children's school records. Default to deny and data minimisation.

## Implemented
- All 11 current `public` tables have RLS and at least one policy.
- Default tenant isolation uses `auth.uid()` resolved through helper functions in the unexposed `app_private` schema to prevent recursive RLS evaluation.
- Profile changes cannot be used by ordinary users to promote themselves to head or admin, or reassign schools.
- Cross-school relationships are checked on writes to terms, classes, guardian links and teaching assignments.
- Server secrets are not placed in the frontend environment template.

## Explicit specification deviation
The uploaded spec stores `offline_pin_hash` in `public.users`. That field is omitted from the cloud-exposed schema: a password/PIN verifier should be derived with a slow KDF and stored in the encrypted local device database/key store. The authenticated cloud session remains the source of identity; offline access is bounded and cached on the device. This reduces exfiltration risk from API-visible columns. The Android implementation and acceptance tests must enforce the offline PIN lockout and freshness rules.

## Not yet verified
- Cross-school and cross-role policy matrix needs real fixture identities and negative tests.
- No teacher/parent onboarding flow or JWT session integration yet.
- No offline 72-hour convergence test yet.
- Payment provider callbacks, SMS, USSD and external services are not configured.
- Storage policies, backup/restore and retention are pending later phases.

Do not publish student data or enable live transactions until these checks pass.
