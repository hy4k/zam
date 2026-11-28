# Holy Moly! Node.js + Socket.io WebRTC Signaling Server

Lightweight signaling server built with **Node.js**, **Express**, and **Socket.io** for 3-way peer-to-peer WebRTC mesh calls, restricting every session strictly to the **3 authorized family members** (`MITHUN`, `MAFITHA`, `ZAYN`).

## Key Features
- **Strict 3-user family authorization**: Verifies both the family member allowlist (`ALLOWED_FAMILY_MEMBERS`, max 3) and a server-side shared secret (`SIGNALING_SHARED_SECRET`) and/or per-member PINs (`FAMILY_MEMBER_PINS_JSON`) during the Socket.io handshake using constant-time verification.
- **Room creation & capacity enforcement**: Creates rooms dynamically on first authorized join, broadcasts peer lists (`room-peers`, `peer-joined`, `peer-left`), rejects any 4th joiner with `room-full`, and deletes rooms automatically when empty.
- **Scoped SDP & ICE relay**: Relays `offer`, `answer`, `ice-candidate`, `ice-restart-request`, and `media-state` strictly between verified peers in the same room with payload validation and rate limiting.
- **Heartbeat & dead-socket cleanup**: Built-in Socket.io ping/pong plus periodic stale-peer sweeping evicts dead sockets so dropped connections never occupy a room slot.
- **Built-in short-TTL TURN credentials**: Authenticated `POST /api/turn-credentials` endpoint mints time-limited coturn HMAC-SHA1 credentials (UDP 3478, TCP 3478, TLS 443).

## Quick Start

```bash
cd signaling-server
npm install
cp .env.example .env
# Edit .env and set SIGNALING_SHARED_SECRET (e.g. `openssl rand -hex 32`)
npm start
```

Run tests:
```bash
npm test
```

## Socket.io Event Contract

### Handshake Auth
```js
const socket = io("https://signal.yourdomain.com", {
  auth: {
    memberId: "MITHUN", // MITHUN | MAFITHA | ZAYN
    token: process.env.SIGNALING_SHARED_SECRET,
    // pin: "1234", // optional if FAMILY_MEMBER_PINS_JSON is configured
  },
});
```

### Client → Server Events
| Event | Payload | Purpose |
|---|---|---|
| `create-room` | `{ roomId?, cameraEnabled?, micEnabled? }` | Create (auto-generating a room ID if omitted) and join a room |
| `join-room` | `{ roomId, cameraEnabled?, micEnabled? }` | Join/create room (max 3 authorized family users); returns `{ ok, self, peers, allPeers }` |
| `get-room-peers` | `{}` | Fetch current peers and deterministic Perfect Negotiation roles (`shouldInitiateOffer`, `polite`) |
| `leave-room` | `{}` | Leave current room and clean up if empty |
| `offer` | `{ targetSocketId \| targetMemberId, sdp: { type: "offer", sdp } }` | Send SDP offer to a peer in the same room |
| `answer` | `{ targetSocketId \| targetMemberId, sdp: { type: "answer", sdp } }` | Send SDP answer to a peer in the same room |
| `ice-candidate` | `{ targetSocketId \| targetMemberId, candidate: { candidate, sdpMid, sdpMLineIndex } }` | Exchange ICE candidate with a peer in the same room |
| `ice-restart-request` | `{ targetSocketId \| targetMemberId, reason? }` | Request ICE restart after Wi-Fi ↔ mobile handoff |
| `media-state` | `{ cameraEnabled?, micEnabled? }` | Broadcast mic/camera toggle state to room |
| `heartbeat` | `{}` | Refresh liveness timestamp |

### Server → Client Events
- `room-peers` — Sent to joining peer with existing room participants.
- `peer-joined` — Broadcast to existing room members when an authorized member joins.
- `peer-left` — Broadcast when a member leaves, disconnects, or times out.
- `room-full` — Emitted if a 4th participant attempts to join a full room.
- `offer`, `answer`, `ice-candidate`, `ice-restart-request`, `media-state` — Relayed peer-to-peer signals.
