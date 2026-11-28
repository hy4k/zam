import test from "node:test";
import assert from "node:assert/strict";
import {
  authorizeFamilyMember,
  parseAllowedMembers,
  parseMemberPins,
  validateIcePayload,
  validateRoomId,
  validateSdpPayload,
} from "../src/auth.js";
import { RoomManager } from "../src/roomManager.js";
import { generateShortLivedTurnCredentials } from "../src/turnCredentials.js";

test("authorizes only the 3 allowed family members with valid secret and PIN", () => {
  const allowedMembers = parseAllowedMembers("MITHUN,MAFITHA,ZAYN,INTRUDER");
  assert.equal(allowedMembers.size, 3);
  assert.equal(allowedMembers.has("INTRUDER"), false);

  const memberPins = parseMemberPins(JSON.stringify({ MITHUN: "1111", MAFITHA: "2222", ZAYN: "3333" }));

  const ok = authorizeFamilyMember({
    memberId: "mithun",
    token: "test-family-secret",
    pin: "1111",
    allowedMembers,
    sharedSecret: "test-family-secret",
    memberPins,
  });
  assert.equal(ok.ok, true);
  assert.equal(ok.memberId, "MITHUN");

  const wrongSecret = authorizeFamilyMember({
    memberId: "MITHUN",
    token: "wrong-secret",
    pin: "1111",
    allowedMembers,
    sharedSecret: "test-family-secret",
    memberPins,
  });
  assert.equal(wrongSecret.ok, false);
  assert.equal(wrongSecret.code, "INVALID_SECRET");

  const wrongPin = authorizeFamilyMember({
    memberId: "MITHUN",
    token: "test-family-secret",
    pin: "9999",
    allowedMembers,
    sharedSecret: "test-family-secret",
    memberPins,
  });
  assert.equal(wrongPin.ok, false);
  assert.equal(wrongPin.code, "INVALID_PIN");

  const outsider = authorizeFamilyMember({
    memberId: "GUEST",
    token: "test-family-secret",
    pin: "1111",
    allowedMembers,
    sharedSecret: "test-family-secret",
    memberPins,
  });
  assert.equal(outsider.ok, false);
  assert.equal(outsider.code, "FORBIDDEN_MEMBER");
});

test("RoomManager allows 3 authorized family members, rejects 4th joiner, scopes SDP routing, and cleans up empty rooms", () => {
  const allowedMembers = parseAllowedMembers("MITHUN,MAFITHA,ZAYN");
  const manager = new RoomManager({ allowedMembers });

  const r1 = manager.joinRoom({ roomId: "holymoly-family-room", socketId: "sock-1", memberId: "MITHUN", now: 1000 });
  assert.equal(r1.ok, true);
  assert.equal(r1.createdRoom, true);
  assert.equal(r1.peers.length, 1);

  const r2 = manager.joinRoom({ roomId: "holymoly-family-room", socketId: "sock-2", memberId: "MAFITHA", now: 1000 });
  assert.equal(r2.ok, true);
  assert.equal(r2.createdRoom, false);
  assert.equal(r2.peers.length, 2);

  const r3 = manager.joinRoom({ roomId: "holymoly-family-room", socketId: "sock-3", memberId: "ZAYN", now: 1000 });
  assert.equal(r3.ok, true);
  assert.equal(r3.peers.length, 3);

  // Unauthorized 4th user is rejected
  const unauthorized = manager.joinRoom({ roomId: "holymoly-family-room", socketId: "sock-4", memberId: "OUTSIDER", now: 1000 });
  assert.equal(unauthorized.ok, false);
  assert.equal(unauthorized.code, "FORBIDDEN_MEMBER");

  // Even without an allowlist override, a 4th distinct peer is strictly rejected with ROOM_FULL
  const openManager = new RoomManager({ maxParticipants: 3 });
  openManager.joinRoom({ roomId: "room-a", socketId: "s1", memberId: "A" });
  openManager.joinRoom({ roomId: "room-a", socketId: "s2", memberId: "B" });
  openManager.joinRoom({ roomId: "room-a", socketId: "s3", memberId: "C" });
  const fourth = openManager.joinRoom({ roomId: "room-a", socketId: "s4", memberId: "D" });
  assert.equal(fourth.ok, false);
  assert.equal(fourth.code, "ROOM_FULL");

  // Same-room routing succeeds; cross-room routing is blocked
  const validRoute = manager.resolveTargetInSameRoom("sock-1", { targetMemberId: "ZAYN" });
  assert.equal(validRoute?.target.socketId, "sock-3");

  manager.joinRoom({ roomId: "other-room", socketId: "sock-other", memberId: "MITHUN" });
  const crossRoom = manager.resolveTargetInSameRoom("sock-other", { targetSocketId: "sock-2" });
  assert.equal(crossRoom, null);

  // Dead socket sweep removes stale sockets and deletes empty rooms
  manager.updateHeartbeat("sock-2", 25_000);
  const evicted = manager.sweepDeadPeers(10_000, 26_000);
  assert.ok(evicted.some((e) => e.socketId === "sock-1"));
  assert.ok(evicted.some((e) => e.socketId === "sock-3"));
  assert.equal(manager.getPeers("holymoly-family-room").length, 1);

  const finalLeave = manager.leaveRoom("sock-2");
  assert.equal(finalLeave?.roomDeleted, true);
  assert.equal(manager.rooms.has("holymoly-family-room"), false);
});

test("validates room IDs, SDP payloads, ICE payloads, and generates short-TTL TURN credentials", () => {
  assert.equal(validateRoomId("holymoly-family-room"), "holymoly-family-room");
  assert.equal(validateRoomId("../bad/room"), null);

  assert.deepEqual(validateSdpPayload({ type: "offer", sdp: "v=0\r\n" }), { type: "offer", sdp: "v=0\r\n" });
  assert.equal(validateSdpPayload({ type: "invalid", sdp: "v=0" }), null);

  assert.deepEqual(
    validateIcePayload({ candidate: "candidate:1 1 UDP 2122252543 192.0.2.1 54321 typ host", sdpMid: "0", sdpMLineIndex: 0 }),
    { candidate: "candidate:1 1 UDP 2122252543 192.0.2.1 54321 typ host", sdpMid: "0", sdpMLineIndex: 0 },
  );
  assert.equal(validateIcePayload({ candidate: "" }), null);

  const turn = generateShortLivedTurnCredentials({
    identity: "MITHUN",
    turnDomain: "turn.example.com",
    turnSharedSecret: "unit-test-secret",
    ttlSeconds: 600,
    nowMs: 1_700_000_000_000,
  });
  assert.equal(turn.ttl, 600);
  assert.equal(turn.iceServers.length, 2);
  assert.equal(turn.iceServers[1].urls.length, 3);
  assert.ok(turn.iceServers[1].username.endsWith(":MITHUN"));
  assert.ok(turn.iceServers[1].credential.length > 10);
});
