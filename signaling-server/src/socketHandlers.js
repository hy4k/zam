import crypto from "node:crypto";
import {
  MAX_ROOM_PARTICIPANTS,
  validateIcePayload,
  validateRoomId,
  validateSdpPayload,
} from "./auth.js";

function normalizePayload(payload) {
  return payload && typeof payload === "object" && !Array.isArray(payload) ? payload : {};
}

function resolveAck(payload, ack) {
  if (typeof ack === "function") return ack;
  if (typeof payload === "function") return payload;
  return null;
}

/**
 * Deterministic Perfect Negotiation helper for 3-way mesh pairs:
 * - Alphabetically smaller memberId initiates offers (`MAFITHA < MITHUN < ZAYN`)
 * - Alphabetically larger memberId acts as polite peer (rolls back on SDP glare)
 */
export function computePairRole(selfMemberId, peerMemberId) {
  const shouldInitiateOffer = String(selfMemberId) < String(peerMemberId);
  return {
    shouldInitiateOffer,
    polite: !shouldInitiateOffer,
  };
}

export function broadcastLeave(io, leaveResult) {
  if (!leaveResult || !leaveResult.peer) return;
  io.to(leaveResult.roomId).emit("peer-left", {
    roomId: leaveResult.roomId,
    socketId: leaveResult.peer.socketId,
    memberId: leaveResult.peer.memberId,
    peers: leaveResult.remainingPeers,
    roomDeleted: leaveResult.roomDeleted,
    timestampMs: Date.now(),
  });
}

function generateRoomId() {
  return `family-${crypto.randomBytes(4).toString("hex")}`;
}

/**
 * Registers all Socket.io WebRTC 3-way mesh signaling handlers on an authenticated socket:
 * - Room management: `create-room`, `join-room`, `get-room-peers`, `leave-room`, `disconnect`
 * - SDP negotiation: `offer`, `answer`
 * - ICE exchange & recovery: `ice-candidate`, `ice-restart-request`
 * - Presence & media state: `media-state`, `heartbeat`
 */
export function registerSocketHandlers({ io, socket, roomManager, rateLimiter }) {
  const memberId = socket.data.memberId;

  function handleJoin(rawPayload, rawAck, { allowAutoGenerateRoomId = false } = {}) {
    const ack = resolveAck(rawPayload, rawAck);
    const data = normalizePayload(rawPayload);

    if (!rateLimiter.allow(socket.id)) {
      return ack?.({ ok: false, code: "RATE_LIMITED", message: "Too many signaling events" });
    }

    const requestedRoomId =
      typeof data.roomId === "string" && data.roomId.trim().length > 0
        ? data.roomId
        : allowAutoGenerateRoomId
          ? generateRoomId()
          : "";

    const roomId = validateRoomId(requestedRoomId);
    if (!roomId) {
      return ack?.({
        ok: false,
        code: "INVALID_ROOM_ID",
        message: "roomId must be 1-64 alphanumeric, hyphen, or underscore characters",
      });
    }

    const joinResult = roomManager.joinRoom({
      roomId,
      socketId: socket.id,
      memberId,
      cameraEnabled: data.cameraEnabled ?? true,
      micEnabled: data.micEnabled ?? true,
    });

    if (!joinResult.ok) {
      socket.emit("room-full", {
        roomId,
        code: joinResult.code,
        message: joinResult.reason,
        maxParticipants: joinResult.maxParticipants ?? MAX_ROOM_PARTICIPANTS,
      });
      return ack?.({
        ok: false,
        code: joinResult.code,
        message: joinResult.reason,
        maxParticipants: joinResult.maxParticipants ?? MAX_ROOM_PARTICIPANTS,
      });
    }

    // Notify previous room if socket moved between rooms
    if (joinResult.leftPrevious) {
      socket.leave(joinResult.leftPrevious.roomId);
      broadcastLeave(io, joinResult.leftPrevious);
    }

    // Cleanly evict any stale socket held by the same family member (e.g. after Wi-Fi -> 5G handoff)
    if (joinResult.replacedSocketId) {
      const staleSocket = io.sockets.sockets.get(joinResult.replacedSocketId);
      if (staleSocket) {
        staleSocket.leave(roomId);
        staleSocket.emit("session-replaced", {
          roomId,
          message: "Your family session reconnected from a newer connection",
        });
        staleSocket.disconnect(true);
      }
      // Tell remaining peers to drop the stale PeerConnection before establishing the fresh one
      socket.to(roomId).emit("peer-left", {
        roomId,
        socketId: joinResult.replacedSocketId,
        memberId,
        peers: joinResult.peers.filter((p) => p.socketId !== socket.id),
        replaced: true,
        timestampMs: Date.now(),
      });
    }

    socket.join(roomId);

    const otherPeers = joinResult.peers
      .filter((p) => p.socketId !== socket.id)
      .map((p) => ({
        ...p,
        ...computePairRole(memberId, p.memberId),
      }));

    // Send current room peer list + deterministic Perfect Negotiation roles to the joining peer
    socket.emit("room-peers", {
      roomId,
      self: joinResult.peer,
      peers: otherPeers,
      allPeers: joinResult.peers,
      maxParticipants: MAX_ROOM_PARTICIPANTS,
      timestampMs: Date.now(),
    });

    // Notify existing room members so each initiates or awaits its mesh PeerConnection
    socket.to(roomId).emit("peer-joined", {
      roomId,
      peer: joinResult.peer,
      peers: joinResult.peers,
      timestampMs: Date.now(),
    });

    return ack?.({
      ok: true,
      roomId,
      createdRoom: joinResult.createdRoom,
      self: joinResult.peer,
      peers: otherPeers,
      allPeers: joinResult.peers,
      maxParticipants: MAX_ROOM_PARTICIPANTS,
    });
  }

  // Room creation (generates a roomId if omitted, or validates the supplied one)
  socket.on("create-room", (payload, ack) => {
    handleJoin(payload, ack, { allowAutoGenerateRoomId: true });
  });

  // Join existing room (or auto-create if first authorized family member)
  socket.on("join-room", (payload, ack) => {
    handleJoin(payload, ack, { allowAutoGenerateRoomId: false });
  });

  // Query current peers in the socket's active room
  socket.on("get-room-peers", (payload, ack) => {
    const done = resolveAck(payload, ack);
    const roomId = roomManager.getRoomIdForSocket(socket.id);
    if (!roomId) {
      return done?.({ ok: false, code: "NOT_IN_ROOM", message: "Join a room first" });
    }
    const allPeers = roomManager.getPeers(roomId);
    const peers = allPeers
      .filter((p) => p.socketId !== socket.id)
      .map((p) => ({
        ...p,
        ...computePairRole(memberId, p.memberId),
      }));
    return done?.({ ok: true, roomId, peers, allPeers, maxParticipants: MAX_ROOM_PARTICIPANTS });
  });

  // Leave current room cleanly
  socket.on("leave-room", (payload, ack) => {
    const done = resolveAck(payload, ack);
    const leaveResult = roomManager.leaveRoom(socket.id);
    if (leaveResult) {
      socket.leave(leaveResult.roomId);
      broadcastLeave(io, leaveResult);
    }
    return done?.({ ok: true, roomDeleted: leaveResult?.roomDeleted ?? false });
  });

  // Relay SDP offer to a specific peer in the same room
  socket.on("offer", (payload, ack) => {
    const done = resolveAck(payload, ack);
    const data = normalizePayload(payload);
    if (!rateLimiter.allow(socket.id)) {
      return done?.({ ok: false, code: "RATE_LIMITED", message: "Too many signaling events" });
    }

    const rawSdp =
      typeof data.sdp === "string"
        ? { type: "offer", sdp: data.sdp }
        : data.sdp && typeof data.sdp === "object"
          ? data.sdp
          : data;
    const sdp = validateSdpPayload(rawSdp);
    if (!sdp || sdp.type !== "offer") {
      return done?.({ ok: false, code: "INVALID_SDP", message: "Valid SDP offer is required" });
    }

    const route = roomManager.resolveTargetInSameRoom(socket.id, {
      targetSocketId: data.targetSocketId ?? data.to,
      targetMemberId: data.targetMemberId ?? data.targetId,
    });
    if (!route) {
      return done?.({ ok: false, code: "PEER_NOT_FOUND", message: "Target peer is not in your room" });
    }

    roomManager.updateHeartbeat(socket.id);
    io.to(route.target.socketId).emit("offer", {
      roomId: route.roomId,
      fromSocketId: route.sender.socketId,
      fromMemberId: route.sender.memberId,
      sdp,
      iceRestart: Boolean(data.iceRestart),
      timestampMs: Date.now(),
    });
    return done?.({ ok: true });
  });

  // Relay SDP answer to a specific peer in the same room
  socket.on("answer", (payload, ack) => {
    const done = resolveAck(payload, ack);
    const data = normalizePayload(payload);
    if (!rateLimiter.allow(socket.id)) {
      return done?.({ ok: false, code: "RATE_LIMITED", message: "Too many signaling events" });
    }

    const rawSdp =
      typeof data.sdp === "string"
        ? { type: "answer", sdp: data.sdp }
        : data.sdp && typeof data.sdp === "object"
          ? data.sdp
          : data;
    const sdp = validateSdpPayload(rawSdp);
    if (!sdp || sdp.type !== "answer") {
      return done?.({ ok: false, code: "INVALID_SDP", message: "Valid SDP answer is required" });
    }

    const route = roomManager.resolveTargetInSameRoom(socket.id, {
      targetSocketId: data.targetSocketId ?? data.to,
      targetMemberId: data.targetMemberId ?? data.targetId,
    });
    if (!route) {
      return done?.({ ok: false, code: "PEER_NOT_FOUND", message: "Target peer is not in your room" });
    }

    roomManager.updateHeartbeat(socket.id);
    io.to(route.target.socketId).emit("answer", {
      roomId: route.roomId,
      fromSocketId: route.sender.socketId,
      fromMemberId: route.sender.memberId,
      sdp,
      timestampMs: Date.now(),
    });
    return done?.({ ok: true });
  });

  // Relay ICE candidate to a specific peer in the same room
  socket.on("ice-candidate", (payload, ack) => {
    const done = resolveAck(payload, ack);
    const data = normalizePayload(payload);
    if (!rateLimiter.allow(socket.id)) {
      return done?.({ ok: false, code: "RATE_LIMITED", message: "Too many signaling events" });
    }

    const rawIce = data.candidate && typeof data.candidate === "object" ? data.candidate : data;
    const ice = validateIcePayload(rawIce);
    if (!ice) {
      return done?.({ ok: false, code: "INVALID_ICE", message: "Valid ICE candidate is required" });
    }

    const route = roomManager.resolveTargetInSameRoom(socket.id, {
      targetSocketId: data.targetSocketId ?? data.to,
      targetMemberId: data.targetMemberId ?? data.targetId,
    });
    if (!route) {
      return done?.({ ok: false, code: "PEER_NOT_FOUND", message: "Target peer is not in your room" });
    }

    roomManager.updateHeartbeat(socket.id);
    io.to(route.target.socketId).emit("ice-candidate", {
      roomId: route.roomId,
      fromSocketId: route.sender.socketId,
      fromMemberId: route.sender.memberId,
      candidate: ice,
      timestampMs: Date.now(),
    });
    return done?.({ ok: true });
  });

  // Request ICE restart from a specific peer (e.g. after Wi-Fi <-> Cellular handoff)
  socket.on("ice-restart-request", (payload, ack) => {
    const done = resolveAck(payload, ack);
    const data = normalizePayload(payload);
    if (!rateLimiter.allow(socket.id)) {
      return done?.({ ok: false, code: "RATE_LIMITED", message: "Too many signaling events" });
    }

    const route = roomManager.resolveTargetInSameRoom(socket.id, {
      targetSocketId: data.targetSocketId ?? data.to,
      targetMemberId: data.targetMemberId ?? data.targetId,
    });
    if (!route) {
      return done?.({ ok: false, code: "PEER_NOT_FOUND", message: "Target peer is not in your room" });
    }

    roomManager.updateHeartbeat(socket.id);
    io.to(route.target.socketId).emit("ice-restart-request", {
      roomId: route.roomId,
      fromSocketId: route.sender.socketId,
      fromMemberId: route.sender.memberId,
      reason: typeof data.reason === "string" ? data.reason.slice(0, 64) : "network_change",
      timestampMs: Date.now(),
    });
    return done?.({ ok: true });
  });

  // Broadcast camera / microphone mute state to other peers in the room
  socket.on("media-state", (payload, ack) => {
    const done = resolveAck(payload, ack);
    const data = normalizePayload(payload);
    if (!rateLimiter.allow(socket.id)) {
      return done?.({ ok: false, code: "RATE_LIMITED", message: "Too many signaling events" });
    }

    const updated = roomManager.updateMediaState(socket.id, {
      cameraEnabled: data.cameraEnabled,
      micEnabled: data.micEnabled,
    });
    if (!updated) {
      return done?.({ ok: false, code: "NOT_IN_ROOM", message: "Join a room first" });
    }

    socket.to(updated.roomId).emit("media-state", {
      roomId: updated.roomId,
      socketId: updated.peer.socketId,
      memberId: updated.peer.memberId,
      cameraEnabled: updated.peer.cameraEnabled,
      micEnabled: updated.peer.micEnabled,
      timestampMs: Date.now(),
    });
    return done?.({ ok: true });
  });

  // Client heartbeat to keep socket fresh and prevent dead-socket eviction
  socket.on("heartbeat", (payload, ack) => {
    const done = resolveAck(payload, ack);
    const ok = roomManager.updateHeartbeat(socket.id);
    return done?.({ ok, serverTimeMs: Date.now() });
  });

  // Cleanly remove peer and delete empty room on socket disconnect
  socket.on("disconnect", () => {
    rateLimiter.clear(socket.id);
    const leaveResult = roomManager.leaveRoom(socket.id);
    if (leaveResult) {
      broadcastLeave(io, leaveResult);
    }
  });
}
