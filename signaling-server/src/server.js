import http from "node:http";
import express from "express";
import { Server as SocketIOServer } from "socket.io";
import {
  authorizeFamilyMember,
  constantTimeEquals,
  parseAllowedMembers,
  parseMemberPins,
} from "./auth.js";
import { RoomManager } from "./roomManager.js";
import { broadcastLeave, registerSocketHandlers } from "./socketHandlers.js";
import { generateShortLivedTurnCredentials } from "./turnCredentials.js";

function parseAllowedOrigins(rawCsv) {
  if (typeof rawCsv !== "string" || rawCsv.trim().length === 0) {
    return new Set();
  }
  return new Set(
    rawCsv
      .split(",")
      .map((origin) => origin.trim())
      .filter(Boolean),
  );
}

/**
 * Checks browser Origin header against an explicit allowlist.
 * Non-browser clients (Android/iOS/native) omit Origin and are permitted.
 * Wildcard `*` is intentionally disallowed on authenticated signaling endpoints.
 */
function isOriginAllowed(origin, allowedOrigins) {
  if (!origin) return true;
  if (allowedOrigins.size === 0) return false;
  return allowedOrigins.has(origin);
}

/**
 * Simple per-socket fixed-window rate limiter to protect against event floods.
 */
export function createRateLimiter({ maxEvents = 120, windowMs = 10_000 } = {}) {
  const buckets = new Map();
  return {
    allow(socketId) {
      const now = Date.now();
      let bucket = buckets.get(socketId);
      if (!bucket || now - bucket.windowStart >= windowMs) {
        bucket = { count: 1, windowStart: now };
        buckets.set(socketId, bucket);
        return true;
      }
      bucket.count += 1;
      return bucket.count <= maxEvents;
    },
    clear(socketId) {
      buckets.delete(socketId);
    },
  };
}

/**
 * Creates a configured Express + Socket.io WebRTC signaling server enforcing
 * a strict 3-participant authorized family room policy.
 */
export function createSignalingServer(config = {}) {
  const allowedMembers = parseAllowedMembers(config.allowedFamilyMembers);
  const memberPins = parseMemberPins(config.familyMemberPinsJson);
  const sharedSecret = String(config.signalingSharedSecret ?? "").trim();
  const allowedOrigins = parseAllowedOrigins(config.corsOrigins);
  const heartbeatTimeoutMs = Math.max(Number(config.heartbeatTimeoutMs) || 20_000, 5_000);
  const sweepIntervalMs = Math.max(Number(config.sweepIntervalMs) || 5_000, 1_000);
  const turnDomain = String(config.turnDomain ?? "").trim();
  const turnSharedSecret = String(config.turnSharedSecret ?? "").trim();
  const turnTtlSeconds = Number(config.turnTtlSeconds) || 600;
  const allowUnauthenticatedDev = Boolean(config.allowUnauthenticatedDev);

  const roomManager = new RoomManager({ allowedMembers });
  const rateLimiter = createRateLimiter();

  const app = express();
  app.disable("x-powered-by");
  app.use(express.json({ limit: "16kb" }));

  // Explicit CORS middleware (never wildcard on authenticated endpoints)
  app.use((req, res, next) => {
    const origin = req.headers.origin;
    if (origin && isOriginAllowed(origin, allowedOrigins)) {
      res.setHeader("Access-Control-Allow-Origin", origin);
      res.setHeader("Vary", "Origin");
      res.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
      res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    }
    if (req.method === "OPTIONS") {
      return res.sendStatus(204);
    }
    return next();
  });

  app.get("/health", (_req, res) => {
    res.json({
      status: "ok",
      service: "holymoly-webrtc-signaling",
      stats: roomManager.getStats(),
      timestampMs: Date.now(),
    });
  });

  // Short-TTL coturn TURN credentials endpoint (requires family authorization)
  app.post("/api/turn-credentials", (req, res) => {
    const authHeader = String(req.headers.authorization ?? "");
    const bearerToken = authHeader.startsWith("Bearer ") ? authHeader.slice(7).trim() : "";

    const memberId = String(req.body?.identity ?? req.body?.memberId ?? "").trim().toUpperCase();
    const pin = req.body?.pin;

    const authResult = authorizeFamilyMember({
      memberId,
      token: bearerToken,
      pin,
      allowedMembers,
      sharedSecret,
      memberPins,
      allowUnauthenticatedDev,
    });

    if (!authResult.ok) {
      return res.status(401).json({ error: authResult.code, message: authResult.reason });
    }

    const creds = generateShortLivedTurnCredentials({
      identity: authResult.memberId,
      turnDomain,
      turnSharedSecret,
      ttlSeconds: turnTtlSeconds,
    });

    res.setHeader("Cache-Control", "no-store");
    return res.status(200).json(creds);
  });

  const httpServer = http.createServer(app);

  const io = new SocketIOServer(httpServer, {
    pingInterval: 10_000,
    pingTimeout: 10_000,
    maxHttpBufferSize: 128 * 1024, // 128 KB max per signaling frame
    cors: {
      origin: (origin, callback) => {
        if (isOriginAllowed(origin, allowedOrigins)) {
          callback(null, true);
        } else {
          callback(new Error("Origin not allowed by signaling server CORS policy"));
        }
      },
      methods: ["GET", "POST"],
    },
  });

  // Handshake authentication middleware: rejects unauthorized users BEFORE joining any room
  io.use((socket, next) => {
    const auth = socket.handshake.auth ?? {};
    const authHeader = String(socket.handshake.headers?.authorization ?? "");
    const headerToken = authHeader.startsWith("Bearer ") ? authHeader.slice(7).trim() : "";

    const result = authorizeFamilyMember({
      memberId: auth.memberId,
      token: auth.token ?? auth.familySecret ?? headerToken,
      pin: auth.pin,
      allowedMembers,
      sharedSecret,
      memberPins,
      allowUnauthenticatedDev,
    });

    if (!result.ok) {
      const err = new Error(result.reason);
      err.data = { code: result.code };
      return next(err);
    }

    socket.data.memberId = result.memberId;
    return next();
  });

  io.on("connection", (socket) => {
    registerSocketHandlers({ io, socket, roomManager, rateLimiter });
  });

  // Periodic sweep for silent / half-open dead sockets + automatic empty-room cleanup
  const sweepTimer = setInterval(() => {
    const evicted = roomManager.sweepDeadPeers(heartbeatTimeoutMs);
    for (const item of evicted) {
      const deadSocket = io.sockets.sockets.get(item.socketId);
      if (deadSocket) {
        deadSocket.leave(item.roomId);
        deadSocket.disconnect(true);
      }
      broadcastLeave(io, item);
    }
  }, sweepIntervalMs);
  sweepTimer.unref();

  async function close() {
    clearInterval(sweepTimer);
    await new Promise((resolve) => io.close(() => resolve()));
    await new Promise((resolve) => httpServer.close(() => resolve()));
  }

  return { app, httpServer, io, roomManager, close };
}

export { constantTimeEquals };
