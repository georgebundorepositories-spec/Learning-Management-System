import { createClient } from "https://esm.sh/@supabase/supabase-js@2.119.0";

export const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Cache-Control": "no-store",
};
export function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status, headers: { ...cors, "Content-Type": "application/json; charset=utf-8" }
  });
}
export function jwtClient(req: Request) {
  const auth = req.headers.get("authorization");
  if (!auth || !/^Bearer\s+\S+$/i.test(auth)) return null;
  const url = Deno.env.get("SUPABASE_URL");
  const key = Deno.env.get("SUPABASE_ANON_KEY");
  if (!url || !key) throw Error("Missing Supabase public server config");
  return {
    token: auth.slice(7),
    client: createClient(url, key, {
      global: { headers: { Authorization: auth } },
      auth: { persistSession: false, autoRefreshToken: false }
    })
  };
}
export const uuid = (input: unknown): input is string =>
  typeof input === "string" &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(input);
export const cursor = (v: string): boolean => /^(0|[1-9]\d{0,18})$/.test(v) &&
  BigInt(v) <= BigInt("9223372036854775807");
