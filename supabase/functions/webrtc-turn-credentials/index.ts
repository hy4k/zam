// Supabase Edge Function: webrtc-turn-credentials
// Generates short-TTL (default 600s) RFC coturn REST API HMAC-SHA1 TURN credentials
// covering UDP 3478, TCP 3478, and TLS 443.
//
// Required Supabase Function Secrets (set via `supabase secrets set`):
//   TURN_DOMAIN=turn.yourdomain.com
//   TURN_SHARED_SECRET=<same static-auth-secret as coturn/turnserver.conf>
// Optional:
//   TURN_TTL_SECONDS=600

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

async function hmacSha1Base64(secret: string, message: string): Promise<string> {
  const encoder = new TextEncoder();
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-1" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("HMAC", key, encoder.encode(message));
  const bytes = new Uint8Array(signature);
  let binary = "";
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const turnDomain = (Deno.env.get("TURN_DOMAIN") ?? "").trim();
  const sharedSecret = (Deno.env.get("TURN_SHARED_SECRET") ?? "").trim();
  const ttlSeconds = Math.min(
    Math.max(Number(Deno.env.get("TURN_TTL_SECONDS") ?? "600") || 600, 120),
    3600,
  );

  let identity = "family";
  try {
    const body = await req.json();
    if (typeof body?.identity === "string" && body.identity.trim().length > 0) {
      identity = body.identity.trim().replace(/[^a-zA-Z0-9_-]/g, "").slice(0, 32) || "family";
    }
  } catch {
    // Allow empty POST body
  }

  const nowSeconds = Math.floor(Date.now() / 1000);
  const expiresAtSeconds = nowSeconds + ttlSeconds;
  const expiresAtMs = expiresAtSeconds * 1000;

  const iceServers: Array<{ urls: string[]; username?: string; credential?: string }> = [
    {
      urls: [
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302",
      ],
    },
  ];

  if (turnDomain && sharedSecret) {
    const username = `${expiresAtSeconds}:${identity}`;
    const credential = await hmacSha1Base64(sharedSecret, username);
    iceServers.push({
      urls: [
        `turn:${turnDomain}:3478?transport=udp`,
        `turn:${turnDomain}:3478?transport=tcp`,
        `turns:${turnDomain}:443?transport=tcp`,
      ],
      username,
      credential,
    });
  }

  return new Response(
    JSON.stringify({
      ttl: ttlSeconds,
      expiresAtMs,
      iceServers,
    }),
    {
      status: 200,
      headers: {
        ...corsHeaders,
        "Content-Type": "application/json",
        "Cache-Control": "no-store",
      },
    },
  );
});
