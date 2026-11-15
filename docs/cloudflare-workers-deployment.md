# Deploy ZimSchool LMS to Cloudflare Workers (GitHub integration)

This replaces the previously blocked Vercel deployment. The existing repository is a **root-level npm workspace monorepo**; the Next.js application lives in `apps/web`. The application remains Supabase-backed and requires server rendering and Server Actions. **Use Cloudflare Workers, not static Cloudflare Pages.**

## Verified build

The official Cloudflare `vinext init --platform=cloudflare` generated `apps/web/vite.config.ts`, `cloudflare.config.ts` and pinned dependencies in the root `package-lock.json`. The migration check reported **96% compatibility (0 blocking issues)**: the only partial feature was App Router `reactStrictMode` enforcement. The Workers Vite production build passed. A follow-up CI workflow verifies real Worker preview routes and existing school permission tests before main promotion.

* `worker.name`: **zimschool-lms**
* Runtime: Cloudflare Workers, `nodejs_compat`, **no shared response or data cache**, no image optimizer.
* Existing Next.js `npm run dev`, `npm run build` remain working.
* `npm run build:vinext --workspace=@zimschool/web` builds the Worker.
* `npm run deploy:vinext --workspace=@zimschool/web` builds and deploys through `cf`.

## Publish from Cloudflare's connected GitHub repository

1. Sign in to [Cloudflare Workers & Pages](https://dash.cloudflare.com/?to=/:account/workers-and-pages), select **Create Worker / Import a repository**, then choose the already-connected GitHub repository `georgebundorepositories-spec/Learning-Management-System`.
2. Set production branch **main**, worker name **zimschool-lms**, framework **Custom / Workers with existing config** (do not choose Pages' static Next.js preset).
3. Set **Root directory = repository root** (`/`) because `package-lock.json` is at the root and `apps/web` is an npm workspace.
4. Workers Builds **Build command: leave empty**. **Deploy command:** `npm run deploy:vinext --workspace=@zimschool/web`. **Preview command:** `npm exec --workspace=@zimschool/web -- vinext-cloudflare previews deploy`.
5. Set Node.js to **22.18+** for `cf` programmatic configuration. Supply `CLOUDFLARE_ACCOUNT_ID` in the build environment if Workers Builds does not supply it automatically.
6. The Supabase URL `https://mezbtcdkmvtkjxdmmhbw.supabase.co` and its **public** `sb_publishable_` key are configured as default non-secret values in the code and Workers bindings. Override both `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` as build vars if deploying a different project. **Never put any service-role, database, private or elevated key in the app or a public repository.**
7. Confirm a real `https://<account-subdomain>.workers.dev` URL is returned. Test `/demo`, `/login`, `/accept-invite`, login, refresh, logout, teacher/student/parent/administrator role boundaries and a real invite, with user consent.
8. In Supabase Auth URL configuration, set the Site URL and approved redirect URL(s) for the **verified** new Workers HTTPS domain. Do not invent a `workers.dev` hostname.
9. Add a custom domain after the tested pilot and update Supabase Auth redirect configuration accordingly.

**Important:** Linking the Cloudflare GitHub *account* alone does not create the `zimschool-lms` Worker or configure its build settings. A specific Worker must be connected to this repository in the Cloudflare dashboard. Since the connection is account-specific, do not claim a public deployment without observing a successful deploy and opening the actual URL.

## Smoke tests and safety

- Check `/demo` contains the fictional public demo; check `/login` and `/accept-invite` load.
- Verify authenticated users cannot view another school's learners, grades or guardian links. School IDs and role must come from server-confirmed `public.users`, not client UI or metadata.
- Keep students' private conversations disabled until moderation is implemented.
- Never cache authenticated HTTP responses in a shared CDN. The initial vinext setup explicitly chooses **no cache**.
- Android application remains separate and retains its Supabase project and role permissions.
- This does **not** enable unimplemented SMS, payments, club membership or offline full-database sync.

Cloudflare docs: https://developers.cloudflare.com/workers/framework-guides/web-apps/nextjs/ and https://vinext.dev/docs/deploying/cloudflare and https://developers.cloudflare.com/workers/ci-cd/builds/git-integration/ .
