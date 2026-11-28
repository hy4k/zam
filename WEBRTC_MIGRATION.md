# LiveKit → Native WebRTC 3-Way Mesh Migration Note

## Summary
Video and voice calling for **Holy Moly!** (Mithun, Mafitha, and Zayn Adam) has been completely migrated from LiveKit to native Android WebRTC (`org.webrtc.PeerConnection` via `io.getstream:stream-webrtc-android:1.3.10`) in a lightweight 3-way peer-to-peer mesh topology signaled over the existing Supabase Realtime WebSocket channel.

---

## LiveKit Code & Dependencies Removed
- **Deleted files**:
  - `app/src/main/java/com/example/media/LiveKitCallEngine.kt`
  - `app/src/main/java/com/example/media/LiveKitTokenProvider.kt`
  - `app/src/main/res/layout/view_livekit_renderer.xml`
- **Removed Gradle & resource references**:
  - `gradle/libs.versions.toml`: removed `livekitAndroid` and `livekit-android`
  - `app/build.gradle.kts`: removed LiveKit dependency; added `implementation(libs.stream.webrtc.android)`
  - `app/src/main/res/values/ids.xml`: removed unused `livekit_video_renderer`
  - `app/src/main/java/com/example/data/model/FamilyModels.kt`: renamed `OfflineSyncState.liveKitConfigured` → `webRtcSignalingConfigured`
  - `.env.example`: removed `LIVEKIT_WS_URL`, `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET`

---

## Files Added & Updated

### Client WebRTC Modules (`app/src/main/java/com/example/media/`)
- **`TurnCredentialsProvider.kt`** *(new)* — Always includes Google public STUN servers (`stun.l.google.com:19302`..`stun4`), fetches short-TTL dynamic coturn credentials (UDP 3478, TCP 3478, TLS 443) from `WEBRTC_TURN_ENDPOINT` (or auto-derived `${SUPABASE_URL}/functions/v1/webrtc-turn-credentials`), caches until 60s before expiry, and falls back gracefully to static `WEBRTC_TURN_*` Secrets or STUN-only.
- **`WebRtcMediaManager.kt`** *(new)* — Encapsulates `PeerConnectionFactory`, shared `EglBase`, hardware AEC/NS `JavaAudioDeviceModule`, `Camera2`/`Camera1` capture (`640x480 @ 24fps`), camera flip, mic/camera toggle, and Android `AudioManager` communication routing.
- **`WebRtcSignalingClient.kt`** *(new)* — Room-ID-scoped signaling client over Supabase Realtime (`webrtc_signal`), enforcing a strict **max-3 participant cap** (`MAX_ROOM_PARTICIPANTS = 3`, emits `ROOM_FULL` on 4th joiner), peer lists, heartbeats, and instant socket reconnect on network switch.
- **`WebRtcCallEngine.kt`** *(new / refactored)* — Orchestrates the 3-way mesh with:
  - **True Perfect Negotiation**: deterministic polite/impolite roles (`localMember.name > remote.name`) with SDP `ROLLBACK` on simultaneous offer glare.
  - **Network-switch recovery (`onNetworkChanged()`)**: refreshes TURN credentials, reconnects signaling, and issues ICE restarts on Wi-Fi ↔ Cellular handoffs.
  - **Escalating failure recovery**: `ICE_DISCONNECTED` → 2.5s grace → ICE restart (up to 2 attempts) → full `PeerConnection` renegotiation fallback → graceful dead-peer cleanup (removes tile without disrupting remaining participants; allows seamless rejoin).
  - **Real-time DataChannels** (with signaling fallback) for AR filters, emoji bursts, screen taps, and live drawing.
- **`WebRtcTextureVideoView.kt`** *(new)* — Compose-safe `TextureView` + `EglRenderer` `VideoSink` rendering local and remote `org.webrtc.VideoTrack`s.

### App Integration
- **`app/src/main/java/com/example/sync/SupabaseRealtimeSignaling.kt`** — Added `WebRtcSignalEnvelope` on the `webrtc_signal` broadcast event and `reconnectForNetworkSwitch()`.
- **`app/src/main/java/com/example/ui/KinNestViewModel.kt`** — Wired `ConnectivityManager` Wi-Fi ↔ Cellular changes to `webRtcEngine.onNetworkChanged()`, handled `peerDroppedEvents` (tile removal + seamless rejoin) and `roomFullEvents`, and kept all existing call/mute/camera/AR controls intact.
- **`app/src/main/java/com/example/ui/screens/ActiveVideoCallScreen.kt`** & **`MainActivity.kt`** — Swapped video tile rendering to `WebRtcTextureVideoView` with zero UI redesign.

### Self-Hosted Coturn & Short-TTL Backend Deliverables
- **`coturn/turnserver.conf`** *(new)* — Production coturn config with UDP 3478, TCP 3478, TLS 443, `use-auth-secret`, and private IP relay hardening.
- **`coturn/docker-compose.yml`** *(new)* — Host-networked Docker Compose definition for running coturn with Let's Encrypt TLS certificates.
- **`supabase/functions/webrtc-turn-credentials/index.ts`** *(new)* — Supabase Edge Function minting time-limited (default 600s) HMAC-SHA1 coturn credentials using `TURN_DOMAIN` and `TURN_SHARED_SECRET` environment secrets.

---

## Environment Variables / AI Studio Secrets

### Remove (no longer used)
- `LIVEKIT_WS_URL`
- `LIVEKIT_API_KEY`
- `LIVEKIT_API_SECRET`

### Keep / Configure in AI Studio Secrets
- `SUPABASE_URL` — Required for Realtime WebSocket signaling (and default TURN Edge Function URL).
- `SUPABASE_ANON_KEY` — Required for Realtime signaling and authenticating the TURN Edge Function.
- `WEBRTC_TURN_ENDPOINT` *(optional)* — Custom URL for short-TTL TURN credentials (defaults automatically to `${SUPABASE_URL}/functions/v1/webrtc-turn-credentials`).
- `WEBRTC_TURN_URL`, `WEBRTC_TURN_USERNAME`, `WEBRTC_TURN_CREDENTIAL` *(optional static fallback)* — Only needed if you prefer static TURN credentials instead of the Edge Function.

### Deploy Coturn + Short-TTL TURN Function (for symmetric NAT / strict mobile firewalls)
1. Start coturn on a small VPS using `coturn/docker-compose.yml` + `coturn/turnserver.conf` (open UDP/TCP 3478, TCP 443, and UDP 49152–65535).
2. Set Supabase Edge Function secrets:
   ```bash
   supabase secrets set TURN_DOMAIN=turn.yourdomain.com TURN_SHARED_SECRET=<your-strong-random-secret>
   supabase functions deploy webrtc-turn-credentials
   ```
