import test from "node:test";
import assert from "node:assert/strict";
import { RoomManager } from "../src/roomManager.js";
import { registerSocketHandlers } from "../src/socketHandlers.js";

/**
 * Lightweight in-memory Socket.io mock harness to verify all signaling event handlers
 * end-to-end across a 3-way mesh without requiring external network ports.
 */
function createMockSocketIo() {
  const sockets = new Map();
  const rooms = new Map(); // roomId -> Set<socketId>

  const io = {
    sockets: { sockets },
    to(targetId) {
      return {
        emit(event, payload) {
          if (sockets.has(targetId)) {
            sockets.get(targetId).received.push({ event, payload });
            return;
          }
          const members = rooms.get(targetId);
          if (!members) return;
          for (const sid of members) {
            sockets.get(sid)?.received.push({ event, payload });
          }
        },
      };
    },
  };

  function connectSocket(socketId, memberId, roomManager, rateLimiter) {
    const handlers = new Map();
    const received = [];
    let disconnected = false;

    const socket = {
      id: socketId,
      data: { memberId },
      received,
      get disconnected() {
        return disconnected;
      },
      on(event, fn) {
        handlers.set(event, fn);
      },
      emit(event, payload) {
        received.push({ event, payload });
      },
      join(roomId) {
        if (!rooms.has(roomId)) rooms.set(roomId, new Set());
        rooms.get(roomId).add(socketId);
      },
      leave(roomId) {
        rooms.get(roomId)?.delete(socketId);
        if (rooms.get(roomId)?.size === 0) rooms.delete(roomId);
      },
      to(roomId) {
        return {
          emit(event, payload) {
            const members = rooms.get(roomId);
            if (!members) return;
            for (const sid of members) {
              if (sid !== socketId) {
                sockets.get(sid)?.received.push({ event, payload });
              }
            }
          },
        };
      },
      disconnect() {
        disconnected = true;
      },
      trigger(event, payload) {
        let ackValue;
        const fn = handlers.get(event);
        assert.ok(fn, `Expected handler for ${event}`);
        fn(payload, (res) => {
          ackValue = res;
        });
        return ackValue;
      },
    };

    sockets.set(socketId, socket);
    registerSocketHandlers({ io, socket, roomManager, rateLimiter });
    return socket;
  }

  return { io, connectSocket };
}

function createNoopRateLimiter() {
  return { allow: () => true, clear: () => {} };
}

test("full 3-way mesh flow: create-room, join-room, offer/answer, ice-candidate, media-state, leave & room cleanup", () => {
  const { connectSocket } = createMockSocketIo();
  const roomManager = new RoomManager({ allowedMembers: new Set(["MITHUN", "MAFITHA", "ZAYN"]) });
  const rateLimiter = createNoopRateLimiter();

  const mithun = connectSocket("s-mithun", "MITHUN", roomManager, rateLimiter);
  const mafitha = connectSocket("s-mafitha", "MAFITHA", roomManager, rateLimiter);
  const zayn = connectSocket("s-zayn", "ZAYN", roomManager, rateLimiter);

  // 1. Mithun creates the family room
  const createAck = mithun.trigger("create-room", { roomId: "holymoly-family-room" });
  assert.equal(createAck.ok, true);
  assert.equal(createAck.createdRoom, true);
  assert.equal(createAck.peers.length, 0);

  // 2. Mafitha joins -> gets Mithun in room-peers with deterministic offerer role (MAFITHA < MITHUN)
  const mafithaAck = mafitha.trigger("join-room", { roomId: "holymoly-family-room" });
  assert.equal(mafithaAck.ok, true);
  assert.equal(mafithaAck.peers.length, 1);
  assert.equal(mafithaAck.peers[0].memberId, "MITHUN");
  assert.equal(mafithaAck.peers[0].shouldInitiateOffer, true);
  assert.equal(mafithaAck.peers[0].polite, false);
  assert.ok(mithun.received.some((m) => m.event === "peer-joined" && m.payload.peer.memberId === "MAFITHA"));

  // 3. Zayn joins -> completes the 3-way mesh; Zayn is polite to both MAFITHA and MITHUN
  const zaynAck = zayn.trigger("join-room", { roomId: "holymoly-family-room" });
  assert.equal(zaynAck.ok, true);
  assert.equal(zaynAck.peers.length, 2);
  assert.ok(zaynAck.peers.every((p) => p.polite === true && p.shouldInitiateOffer === false));

  // 4. Offer from Mafitha -> Zayn
  const offerAck = mafitha.trigger("offer", {
    targetMemberId: "ZAYN",
    sdp: { type: "offer", sdp: "v=0\r\no=- 123 2 IN IP4 127.0.0.1\r\n" },
  });
  assert.equal(offerAck.ok, true);
  const zaynOffer = zayn.received.find((m) => m.event === "offer");
  assert.equal(zaynOffer?.payload.fromMemberId, "MAFITHA");
  assert.equal(zaynOffer?.payload.sdp.type, "offer");

  // 5. Answer from Zayn -> Mafitha
  const answerAck = zayn.trigger("answer", {
    targetSocketId: "s-mafitha",
    sdp: { type: "answer", sdp: "v=0\r\no=- 456 2 IN IP4 127.0.0.1\r\n" },
  });
  assert.equal(answerAck.ok, true);
  const mafithaAnswer = mafitha.received.find((m) => m.event === "answer");
  assert.equal(mafithaAnswer?.payload.fromMemberId, "ZAYN");
  assert.equal(mafithaAnswer?.payload.sdp.type, "answer");

  // 6. ICE candidate from Mithun -> Zayn
  const iceAck = mithun.trigger("ice-candidate", {
    targetMemberId: "ZAYN",
    candidate: { candidate: "candidate:1 1 UDP 2122252543 203.0.113.5 50000 typ host", sdpMid: "0", sdpMLineIndex: 0 },
  });
  assert.equal(iceAck.ok, true);
  const zaynIce = zayn.received.find((m) => m.event === "ice-candidate");
  assert.equal(zaynIce?.payload.fromMemberId, "MITHUN");
  assert.equal(zaynIce?.payload.candidate.sdpMid, "0");

  // 7. Media state broadcast propagates to the other 2 peers only
  const mediaAck = zayn.trigger("media-state", { cameraEnabled: false, micEnabled: true });
  assert.equal(mediaAck.ok, true);
  assert.ok(mithun.received.some((m) => m.event === "media-state" && m.payload.memberId === "ZAYN" && m.payload.cameraEnabled === false));
  assert.ok(mafitha.received.some((m) => m.event === "media-state" && m.payload.memberId === "ZAYN" && m.payload.cameraEnabled === false));

  // 8. Null / malformed payloads fail safely without throwing
  assert.equal(mithun.trigger("offer", null).code, "INVALID_SDP");
  assert.equal(mithun.trigger("ice-candidate", null).code, "INVALID_ICE");

  // 9. Graceful disconnect removes Zayn without breaking Mithun <-> Mafitha, then cleans up room when last leaves
  zayn.trigger("disconnect");
  assert.ok(mithun.received.some((m) => m.event === "peer-left" && m.payload.memberId === "ZAYN"));
  assert.equal(roomManager.getPeers("holymoly-family-room").length, 2);

  mithun.trigger("leave-room");
  const lastLeave = mafitha.trigger("leave-room");
  assert.equal(lastLeave.ok, true);
  assert.equal(lastLeave.roomDeleted, true);
  assert.equal(roomManager.getStats().activeRooms, 0);
});

test("rejects 4th participant with ROOM_FULL and notifies peers when a member reconnects on a new socket", () => {
  const { connectSocket } = createMockSocketIo();
  const roomManager = new RoomManager({ maxParticipants: 3 });
  const rateLimiter = createNoopRateLimiter();

  const p1 = connectSocket("s1", "MITHUN", roomManager, rateLimiter);
  const p2 = connectSocket("s2", "MAFITHA", roomManager, rateLimiter);
  const p3 = connectSocket("s3", "ZAYN", roomManager, rateLimiter);
  const p4 = connectSocket("s4", "GUEST4", roomManager, rateLimiter);

  assert.equal(p1.trigger("join-room", { roomId: "fam" }).ok, true);
  assert.equal(p2.trigger("join-room", { roomId: "fam" }).ok, true);
  assert.equal(p3.trigger("join-room", { roomId: "fam" }).ok, true);

  const fourthAck = p4.trigger("join-room", { roomId: "fam" });
  assert.equal(fourthAck.ok, false);
  assert.equal(fourthAck.code, "ROOM_FULL");
  assert.ok(p4.received.some((m) => m.event === "room-full"));

  // Reconnecting MITHUN on a fresh socket replaces s1 and notifies MAFITHA + ZAYN cleanly
  const p1Fresh = connectSocket("s1-new", "MITHUN", roomManager, rateLimiter);
  const rejoinAck = p1Fresh.trigger("join-room", { roomId: "fam" });
  assert.equal(rejoinAck.ok, true);
  assert.equal(p1.disconnected, true);
  assert.ok(p2.received.some((m) => m.event === "peer-left" && m.payload.socketId === "s1" && m.payload.replaced === true));
  assert.ok(p2.received.some((m) => m.event === "peer-joined" && m.payload.peer.socketId === "s1-new"));
  assert.equal(roomManager.getPeers("fam").length, 3);
});
