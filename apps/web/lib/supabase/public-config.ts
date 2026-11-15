/**
 * Public Supabase project identifiers are safe to include in client bundles.
 * Never put the Supabase service_role/secret key in a Worker or browser.
 * Explicit environment variables override the published pilot defaults.
 */
export const supabasePublicUrl =
  process.env.NEXT_PUBLIC_SUPABASE_URL || "https://mezbtcdkmvtkjxdmmhbw.supabase.co";

export const supabasePublishableKey =
  process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY || "sb_publishable_nlxCwwfA_MWEpI-3zX1hWg__PlUl91m";
