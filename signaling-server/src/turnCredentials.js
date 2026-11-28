import crypto from "node:crypto";

const DEFAULT_STUN_SERVERS = [
  {
    urls: [
      "stun:stun.l.google.com:19302",
      "stun:stun1.l.google.com:19302",
    ],
  },
];

/**
 * Generates RFC coturn REST API short-TTL HMAC-SHA1 TURN credentials
 * supporting UDP 3478, TCP 3478, and TLS 443.
 */
export function generateShortLivedTurnCredentials({
  identity = "family",
  turnDomain = "",
  turnSharedSecret = "",
  ttlSeconds = 600,
  nowMs = Date.now(),
}) {
  const safeTtl = Math.min(Math.max(Number(ttlSeconds) || 600, 120), 3600);
  const safeIdentity =
    String(identity || "family")
      .trim()
      .replace(/[^a-zA-Z0-9_-]/g, "")
      .slice(0, 32) || "family";

  const expiresAtSeconds = Math.floor(nowMs / 1000) + safeTtl;
  const expiresAtMs = expiresAtSeconds * 1000;
  const iceServers = [...DEFAULT_STUN_SERVERS];

  const domain = String(turnDomain || "").trim();
  const secret = String(turnSharedSecret || "").trim();

  if (domain && secret) {
    const username = `${expiresAtSeconds}:${safeIdentity}`;
    const credential = crypto
      .createHmac("sha1", secret)
      .update(username)
      .digest("base64");

    iceServers.push({
      urls: [
        `turn:${domain}:3478?transport=udp`,
        `turn:${domain}:3478?transport=tcp`,
        `turns:${domain}:443?transport=tcp`,
      ],
      username,
      credential,
    });
  }

  return {
    ttl: safeTtl,
    expiresAtMs,
    iceServers,
  };
}
