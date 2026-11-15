import { createBrowserClient } from "@supabase/ssr";
import {supabasePublicUrl,supabasePublishableKey} from "./public-config";

export function createClient() {
  const url = supabasePublicUrl;
  const key = supabasePublishableKey;
  if (!url || !key) throw new Error("Missing public Supabase configuration");
  return createBrowserClient(url, key);
}
