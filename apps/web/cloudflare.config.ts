import { bindings, defineConfig, defineWorker } from "cf/config";

export default defineConfig({
  worker: defineWorker({
    name: "zimschool-lms",
    entrypoint: "vinext/server/fetch-handler",
    compatibilityDate: "2026-11-15",
    compatibilityFlags: ["nodejs_compat"],
    assets: {
      htmlHandling: "none",
      notFoundHandling: "none",
      runWorkerFirst: ["/*", "!/_next/static/*", "!/*/_next/static/*"],
    },
    env: {
      ASSETS: bindings.assets(),
      // Public project identifiers only (never elevated credentials).
      NEXT_PUBLIC_SUPABASE_URL: bindings.text("https://mezbtcdkmvtkjxdmmhbw.supabase.co"),
      NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY: bindings.text("sb_publishable_nlxCwwfA_MWEpI-3zX1hWg__PlUl91m"),
    },
  }),
});
