import { createSignalingServer } from "./server.js";

const port = Number(process.env.PORT) || 3001;
const host = process.env.HOST || "0.0.0.0";

const server = createSignalingServer({
  allowedFamilyMembers: process.env.ALLOWED_FAMILY_MEMBERS,
  familyMemberPinsJson: process.env.FAMILY_MEMBER_PINS_JSON,
  signalingSharedSecret: process.env.SIGNALING_SHARED_SECRET,
  corsOrigins: process.env.CORS_ORIGINS,
  heartbeatTimeoutMs: process.env.HEARTBEAT_TIMEOUT_MS,
  sweepIntervalMs: process.env.SWEEP_INTERVAL_MS,
  turnDomain: process.env.TURN_DOMAIN,
  turnSharedSecret: process.env.TURN_SHARED_SECRET,
  turnTtlSeconds: process.env.TURN_TTL_SECONDS,
});

server.httpServer.listen(port, host, () => {
  console.log(`Holy Moly! WebRTC signaling server listening on http://${host}:${port}`);
});

async function shutdown(signal) {
  console.log(`Received ${signal}, shutting down signaling server gracefully...`);
  await server.close();
  process.exit(0);
}

process.on("SIGTERM", () => shutdown("SIGTERM"));
process.on("SIGINT", () => shutdown("SIGINT"));
