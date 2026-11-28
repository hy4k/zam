import crypto from "node:crypto";

export const MAX_ROOM_PARTICIPANTS = 3;
export const DEFAULT_ALLOWED_MEMBERS = ["MITHUN", "MAFITHA", "ZAYN"];

const ROOM_ID_REGEX = /^[a-zA-Z0-9_-]{1,64}$/;
const MAX_SDP_LENGTH = 65_536;
const MAX_ICE_CANDIDATE_LENGTH = 2_048;

/**
 * Constant-time equality check using SHA-256 digests so comparison timing
 * never leaks secret length or prefix bytes.
 */
export function constantTimeEquals(a, b) {
  if (typeof a !== "string" || typeof b !== "string" || a.length === 0 || b.length === 0) {
    return false;
  }
  const hashA = crypto.createHash("sha256").update(a, "utf8").digest();
  const hashB = crypto.createHash("sha256").update(b, "utf8").digest();
  return crypto.timingSafeEqual(hashA, hashB);
}

/**
 * Parses and normalizes up to 3 authorized family member IDs from config.
 */
export function parseAllowedMembers(rawCsv) {
  if (typeof rawCsv !== "string" || rawCsv.trim().length === 0) {
    return new Set(DEFAULT_ALLOWED_MEMBERS);
  }
  const parsed = rawCsv
    .split(",")
    .map((entry) => entry.trim().toUpperCase())
    .filter((entry) => /^[A-Z0-9_-]{1,32}$/.test(entry))
    .slice(0, MAX_ROOM_PARTICIPANTS);

  return new Set(parsed.length > 0 ? parsed : DEFAULT_ALLOWED_MEMBERS);
}

/**
 * Parses optional per-member PIN JSON from environment variable.
 * Example: {"MITHUN":"1234","MAFITHA":"5678","ZAYN":"9012"}
 */
export function parseMemberPins(rawJson) {
  if (typeof rawJson !== "string" || rawJson.trim().length === 0) {
    return new Map();
  }
  try {
    const parsed = JSON.parse(rawJson);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return new Map();
    }
    const pins = new Map();
    for (const [key, value] of Object.entries(parsed)) {
      const normalizedKey = String(key).trim().toUpperCase();
      const normalizedPin = String(value ?? "").trim();
      if (normalizedKey && normalizedPin) {
        pins.set(normalizedKey, normalizedPin);
      }
    }
    return pins;
  } catch {
    return new Map();
  }
}

/**
 * Authenticates an incoming family member handshake against:
 * 1. The strict 3-member allowlist (MITHUN, MAFITHA, ZAYN by default).
 * 2. Configured server secret (`sharedSecret`) and/or per-member PIN (`memberPins`).
 *
 * Fail-closed: if neither `sharedSecret` nor `memberPins` is configured on the server,
 * authentication is rejected unless `allowUnauthenticatedDev` is explicitly enabled in tests.
 */
export function authorizeFamilyMember({
  memberId,
  token,
  pin,
  allowedMembers,
  sharedSecret,
  memberPins,
  allowUnauthenticatedDev = false,
}) {
  if (typeof memberId !== "string" || memberId.trim().length === 0) {
    return { ok: false, code: "UNAUTHORIZED", reason: "Missing memberId" };
  }

  const normalizedMember = memberId.trim().toUpperCase();
  if (!allowedMembers.has(normalizedMember)) {
    return {
      ok: false,
      code: "FORBIDDEN_MEMBER",
      reason: "Only authorized family members may join this session",
    };
  }

  const hasSharedSecret = typeof sharedSecret === "string" && sharedSecret.trim().length > 0;
  const expectedPin = memberPins instanceof Map ? memberPins.get(normalizedMember) : undefined;
  const hasPinConfig = typeof expectedPin === "string" && expectedPin.length > 0;

  if (!hasSharedSecret && !hasPinConfig) {
    if (allowUnauthenticatedDev) {
      return { ok: true, memberId: normalizedMember };
    }
    return {
      ok: false,
      code: "SERVER_NOT_CONFIGURED",
      reason: "Server auth secret is not configured (set SIGNALING_SHARED_SECRET or FAMILY_MEMBER_PINS_JSON)",
    };
  }

  if (hasSharedSecret && !constantTimeEquals(String(token ?? ""), sharedSecret.trim())) {
    return { ok: false, code: "INVALID_SECRET", reason: "Invalid family signaling secret" };
  }

  if (hasPinConfig && !constantTimeEquals(String(pin ?? ""), expectedPin)) {
    return { ok: false, code: "INVALID_PIN", reason: "Invalid family member PIN" };
  }

  return { ok: true, memberId: normalizedMember };
}

export function validateRoomId(roomId) {
  if (typeof roomId !== "string") return null;
  const trimmed = roomId.trim();
  return ROOM_ID_REGEX.test(trimmed) ? trimmed : null;
}

export function validateSdpPayload(payload) {
  if (!payload || typeof payload !== "object") return null;
  const sdp = payload.sdp;
  const type = payload.type;
  if (typeof sdp !== "string" || sdp.length === 0 || sdp.length > MAX_SDP_LENGTH) {
    return null;
  }
  if (type !== "offer" && type !== "answer" && type !== "pranswer" && type !== "rollback") {
    return null;
  }
  return { type, sdp };
}

export function validateIcePayload(payload) {
  if (!payload || typeof payload !== "object") return null;
  const candidate = payload.candidate;
  if (typeof candidate !== "string" || candidate.length === 0 || candidate.length > MAX_ICE_CANDIDATE_LENGTH) {
    return null;
  }
  const sdpMid = typeof payload.sdpMid === "string" ? payload.sdpMid.slice(0, 64) : "";
  const sdpMLineIndex = Number.isInteger(payload.sdpMLineIndex) && payload.sdpMLineIndex >= 0
    ? payload.sdpMLineIndex
    : 0;
  return { candidate, sdpMid, sdpMLineIndex };
}
