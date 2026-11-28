import { MAX_ROOM_PARTICIPANTS } from "./auth.js";

/**
 * Manages WebRTC signaling rooms with strict enforcement of:
 * - Dynamic room creation on first authorized join
 * - Maximum 3 authorized family participants per room (4th joiner rejected gracefully with ROOM_FULL)
 * - Automatic stale/dead socket sweeping via heartbeat timestamps
 * - Immediate empty-room cleanup when the last participant leaves
 */
export class RoomManager {
  constructor({ maxParticipants = MAX_ROOM_PARTICIPANTS, allowedMembers } = {}) {
    this.maxParticipants = Math.min(maxParticipants, MAX_ROOM_PARTICIPANTS);
    this.allowedMembers = allowedMembers;
    /** @type {Map<string, Map<string, { socketId: string, memberId: string, joinedAt: number, lastHeartbeatAt: number, cameraEnabled: boolean, micEnabled: boolean }>>} */
    this.rooms = new Map();
    /** @type {Map<string, string>} socketId -> roomId */
    this.socketToRoom = new Map();
  }

  /**
   * Adds an authorized family member to a room, creating the room if it does not exist.
   * Rejects unauthorized members, duplicate active sessions (when room is full), or any 4th joiner.
   */
  joinRoom({ roomId, socketId, memberId, cameraEnabled = true, micEnabled = true, now = Date.now() }) {
    if (this.allowedMembers && !this.allowedMembers.has(memberId)) {
      return { ok: false, code: "FORBIDDEN_MEMBER", reason: "Member is not in the authorized family list" };
    }

    // Leave any previous room this socket was in
    const prevRoomId = this.socketToRoom.get(socketId);
    let leftPrevious = null;
    if (prevRoomId && prevRoomId !== roomId) {
      leftPrevious = this.leaveRoom(socketId);
    }

    let room = this.rooms.get(roomId);
    const createdRoom = !room;
    if (!room) {
      room = new Map();
      this.rooms.set(roomId, room);
    }

    // Check if the same authorized family member already has a socket in this room
    let replacedSocketId = null;
    for (const [existingSocketId, peer] of room.entries()) {
      if (peer.memberId === memberId && existingSocketId !== socketId) {
        // If room is already at capacity (3 active members) and a 4th device tries to piggyback,
        // or if replacing an orphaned reconnect from the same user:
        // Allow replacing the old socket only if it's a clean reconnect, OR reject if caller
        // explicitly shouldn't evict. For 3-way family calls where a phone switches Wi-Fi -> 5G,
        // replacing the stale socket for the SAME authorized member avoids locking them out,
        // while keeping room size strictly <= 3.
        replacedSocketId = existingSocketId;
        room.delete(existingSocketId);
        this.socketToRoom.delete(existingSocketId);
        break;
      }
    }

    // Enforce strict max-3 participant cap
    if (!room.has(socketId) && room.size >= this.maxParticipants) {
      if (room.size === 0) {
        this.rooms.delete(roomId);
      }
      return {
        ok: false,
        code: "ROOM_FULL",
        reason: `Room is full (maximum ${this.maxParticipants} authorized family members)`,
        maxParticipants: this.maxParticipants,
        currentPeers: this.getPeers(roomId),
      };
    }

    const peerRecord = {
      socketId,
      memberId,
      joinedAt: now,
      lastHeartbeatAt: now,
      cameraEnabled: Boolean(cameraEnabled),
      micEnabled: Boolean(micEnabled),
    };

    room.set(socketId, peerRecord);
    this.socketToRoom.set(socketId, roomId);

    return {
      ok: true,
      roomId,
      createdRoom,
      replacedSocketId,
      leftPrevious,
      peer: peerRecord,
      peers: this.getPeers(roomId),
    };
  }

  /**
   * Removes a socket from its current room and deletes the room automatically if empty.
   */
  leaveRoom(socketId) {
    const roomId = this.socketToRoom.get(socketId);
    if (!roomId) return null;

    this.socketToRoom.delete(socketId);
    const room = this.rooms.get(roomId);
    if (!room) return null;

    const peer = room.get(socketId) ?? null;
    room.delete(socketId);

    let roomDeleted = false;
    if (room.size === 0) {
      this.rooms.delete(roomId);
      roomDeleted = true;
    }

    return {
      roomId,
      peer,
      roomDeleted,
      remainingPeers: roomDeleted ? [] : this.getPeers(roomId),
    };
  }

  updateHeartbeat(socketId, now = Date.now()) {
    const roomId = this.socketToRoom.get(socketId);
    if (!roomId) return false;
    const peer = this.rooms.get(roomId)?.get(socketId);
    if (!peer) return false;
    peer.lastHeartbeatAt = now;
    return true;
  }

  updateMediaState(socketId, { cameraEnabled, micEnabled }) {
    const roomId = this.socketToRoom.get(socketId);
    if (!roomId) return null;
    const peer = this.rooms.get(roomId)?.get(socketId);
    if (!peer) return null;
    if (typeof cameraEnabled === "boolean") peer.cameraEnabled = cameraEnabled;
    if (typeof micEnabled === "boolean") peer.micEnabled = micEnabled;
    peer.lastHeartbeatAt = Date.now();
    return { roomId, peer };
  }

  /**
   * Resolves a target peer inside the SAME room as [senderSocketId] by socketId or memberId.
   * Prevents cross-room message spoofing.
   */
  resolveTargetInSameRoom(senderSocketId, { targetSocketId, targetMemberId }) {
    const roomId = this.socketToRoom.get(senderSocketId);
    if (!roomId) return null;
    const room = this.rooms.get(roomId);
    if (!room) return null;

    const sender = room.get(senderSocketId);
    if (!sender) return null;

    if (typeof targetSocketId === "string" && targetSocketId.trim()) {
      const target = room.get(targetSocketId.trim());
      if (target && target.socketId !== senderSocketId) {
        return { roomId, sender, target };
      }
      return null;
    }

    if (typeof targetMemberId === "string" && targetMemberId.trim()) {
      const normalized = targetMemberId.trim().toUpperCase();
      for (const peer of room.values()) {
        if (peer.memberId === normalized && peer.socketId !== senderSocketId) {
          return { roomId, sender, target: peer };
        }
      }
    }

    return null;
  }

  /**
   * Sweeps peers whose last heartbeat is older than [timeoutMs] and cleans up empty rooms.
   * Returns an array of evicted peer records so the server can notify remaining peers and disconnect dead sockets.
   */
  sweepDeadPeers(timeoutMs, now = Date.now()) {
    const evicted = [];
    for (const [roomId, room] of this.rooms.entries()) {
      for (const [socketId, peer] of room.entries()) {
        if (now - peer.lastHeartbeatAt > timeoutMs) {
          const result = this.leaveRoom(socketId);
          if (result) {
            evicted.push({ ...result, socketId });
          }
        }
      }
    }
    return evicted;
  }

  getRoomIdForSocket(socketId) {
    return this.socketToRoom.get(socketId) ?? null;
  }

  getPeers(roomId) {
    const room = this.rooms.get(roomId);
    if (!room) return [];
    return Array.from(room.values()).map((p) => ({
      socketId: p.socketId,
      memberId: p.memberId,
      cameraEnabled: p.cameraEnabled,
      micEnabled: p.micEnabled,
      joinedAt: p.joinedAt,
    }));
  }

  getStats() {
    let totalPeers = 0;
    for (const room of this.rooms.values()) {
      totalPeers += room.size;
    }
    return {
      activeRooms: this.rooms.size,
      activePeers: totalPeers,
      maxParticipantsPerRoom: this.maxParticipants,
    };
  }
}
