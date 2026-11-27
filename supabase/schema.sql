-- =============================================================================
-- Holy Moly ! (A12 E) — Complete Supabase Backend Schema & Storage Setup
-- Run this entire script ONCE in Supabase Dashboard -> SQL Editor -> New Query
-- Covers every active feature:
--   1. family_profiles    (Profile pics, badges, display names, halo colors)
--   2. family_messages    (Chat text, Walkie-Talkie voice notes, photos/videos, reactions)
--   3. xoxo_games         (Live 2-of-3 XOXO board state, turn, scores, round)
--   4. family_presence    (Live Online / Offline & last-seen status)
--   5. family_parent_logs (Mithun & Mafitha PIN-protected grown-up logs)
--   6. family_call_logs   (Group & 1-on-1 live video call history)
--   7. Storage Bucket     ('family-media' for avatars, walkie-talkie clips, chat media)
-- =============================================================================

-- 1. Family Profiles (Profile pictures, daily badges, roles)
CREATE TABLE IF NOT EXISTS public.family_profiles (
    member_id TEXT PRIMARY KEY CHECK (member_id IN ('MITHUN', 'MAFITHA', 'ZAYN')),
    display_name TEXT NOT NULL,
    role_label TEXT NOT NULL,
    avatar_url TEXT,
    badge_element TEXT NOT NULL DEFAULT 'STARLIGHT',
    halo_color_hex BIGINT NOT NULL DEFAULT 4281908984,
    updated_at_ms BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

INSERT INTO public.family_profiles (member_id, display_name, role_label, badge_element, halo_color_hex)
VALUES
    ('MITHUN', 'Mithun', 'Vapi', 'COMPASS', 4281908984),
    ('MAFITHA', 'Mafitha', 'Ummi', 'STARLIGHT', 4294668677),
    ('ZAYN', 'Zayn Adam', 'Explorer', 'ROCKET', 4294688548)
ON CONFLICT (member_id) DO NOTHING;

-- 2. Family Chat Messages & Walkie-Talkie Voice Clips
CREATE TABLE IF NOT EXISTS public.family_messages (
    id TEXT PRIMARY KEY,
    thread_id TEXT NOT NULL DEFAULT 'family_group',
    sender_id TEXT NOT NULL CHECK (sender_id IN ('MITHUN', 'MAFITHA', 'ZAYN')),
    recipient_ids_csv TEXT NOT NULL DEFAULT 'MITHUN,MAFITHA,ZAYN',
    body TEXT NOT NULL DEFAULT '',
    message_type TEXT NOT NULL DEFAULT 'TEXT',
    media_url TEXT,
    reaction_emoji TEXT,
    delivery_status TEXT NOT NULL DEFAULT 'DELIVERED',
    timestamp_ms BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_family_messages_thread_ts
    ON public.family_messages (thread_id, timestamp_ms DESC);

-- 3. Premium XOXO Game State & Scoreboard (2 of 3 family members)
CREATE TABLE IF NOT EXISTS public.xoxo_games (
    id TEXT PRIMARY KEY DEFAULT 'family_main',
    board_json TEXT NOT NULL DEFAULT '["","","","","","","","",""]',
    player_x TEXT NOT NULL DEFAULT 'MITHUN' CHECK (player_x IN ('MITHUN', 'MAFITHA', 'ZAYN')),
    player_o TEXT NOT NULL DEFAULT 'ZAYN' CHECK (player_o IN ('MITHUN', 'MAFITHA', 'ZAYN')),
    current_turn TEXT NOT NULL DEFAULT 'X' CHECK (current_turn IN ('X', 'O')),
    starting_mark TEXT NOT NULL DEFAULT 'X' CHECK (starting_mark IN ('X', 'O')),
    winner_mark TEXT CHECK (winner_mark IS NULL OR winner_mark IN ('X', 'O')),
    winning_line_json TEXT NOT NULL DEFAULT '[]',
    is_draw BOOLEAN NOT NULL DEFAULT FALSE,
    x_score INT NOT NULL DEFAULT 0,
    o_score INT NOT NULL DEFAULT 0,
    draw_score INT NOT NULL DEFAULT 0,
    round_number INT NOT NULL DEFAULT 1,
    updated_at_ms BIGINT NOT NULL DEFAULT 0,
    CHECK (player_x <> player_o)
);

INSERT INTO public.xoxo_games (id, player_x, player_o)
VALUES ('family_main', 'MITHUN', 'ZAYN')
ON CONFLICT (id) DO NOTHING;

-- 4. Live Family Online / Offline Presence
CREATE TABLE IF NOT EXISTS public.family_presence (
    member_id TEXT PRIMARY KEY CHECK (member_id IN ('MITHUN', 'MAFITHA', 'ZAYN')),
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen_ms BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

INSERT INTO public.family_presence (member_id, is_online)
VALUES
    ('MITHUN', FALSE),
    ('MAFITHA', FALSE),
    ('ZAYN', FALSE)
ON CONFLICT (member_id) DO NOTHING;

-- 5. Parent Settings — Mithun & Mafitha's Grown-Up Log
CREATE TABLE IF NOT EXISTS public.family_parent_logs (
    id TEXT PRIMARY KEY,
    author_id TEXT NOT NULL CHECK (author_id IN ('MITHUN', 'MAFITHA', 'ZAYN')),
    title TEXT NOT NULL,
    subtitle TEXT NOT NULL DEFAULT '',
    cost_estimate TEXT,
    target_date_label TEXT DEFAULT 'Adults Only',
    created_at_ms BIGINT NOT NULL
);

-- 6. Video Call Session Logs (Group & 1-on-1 LiveKit Calls)
CREATE TABLE IF NOT EXISTS public.family_call_logs (
    id TEXT PRIMARY KEY,
    caller_id TEXT NOT NULL CHECK (caller_id IN ('MITHUN', 'MAFITHA', 'ZAYN')),
    participants_csv TEXT NOT NULL,
    room_name TEXT NOT NULL DEFAULT 'holymoly-family-room',
    is_three_way BOOLEAN NOT NULL DEFAULT TRUE,
    duration_seconds BIGINT NOT NULL DEFAULT 0,
    started_at_ms BIGINT NOT NULL
);

-- =============================================================================
-- Row Level Security (RLS) Policies for Equipment-Assigned 3-Person Family App
-- =============================================================================
ALTER TABLE public.family_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.family_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.xoxo_games ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.family_presence ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.family_parent_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.family_call_logs ENABLE ROW LEVEL SECURITY;

-- Profiles policies
DROP POLICY IF EXISTS "family_profiles_select" ON public.family_profiles;
CREATE POLICY "family_profiles_select" ON public.family_profiles
    FOR SELECT USING (member_id IN ('MITHUN', 'MAFITHA', 'ZAYN'));

DROP POLICY IF EXISTS "family_profiles_insert" ON public.family_profiles;
CREATE POLICY "family_profiles_insert" ON public.family_profiles
    FOR INSERT WITH CHECK (member_id IN ('MITHUN', 'MAFITHA', 'ZAYN'));

DROP POLICY IF EXISTS "family_profiles_update" ON public.family_profiles;
CREATE POLICY "family_profiles_update" ON public.family_profiles
    FOR UPDATE USING (member_id IN ('MITHUN', 'MAFITHA', 'ZAYN'))
    WITH CHECK (member_id IN ('MITHUN', 'MAFITHA', 'ZAYN'));

-- Messages policies
DROP POLICY IF EXISTS "family_messages_select" ON public.family_messages;
CREATE POLICY "family_messages_select" ON public.family_messages
    FOR SELECT USING (sender_id IN ('MITHUN', 'MAFITHA', 'ZAYN'));

DROP POLICY IF EXISTS "family_messages_insert" ON public.family_messages;
CREATE POLICY "family_messages_insert" ON public.family_messages
    FOR INSERT WITH CHECK (sender_id IN ('MITHUN', 'MAFITHA', 'ZAYN'));

DROP POLICY IF EXISTS "family_messages_update" ON public.family_messages;
CREATE POLICY "family_messages_update" ON public.family_messages
    FOR UPDATE USING (sender_id IN ('MITHUN', 'MAFITHA', 'ZAYN'))
    WITH CHECK (sender_id IN ('MITHUN', 'MAFITHA', 'ZAYN'));

-- XOXO game policies
DROP POLICY IF EXISTS "xoxo_games_select" ON public.xoxo_games;
CREATE POLICY "xoxo_games_select" ON public.xoxo_games
    FOR SELECT USING (id = 'family_main');

DROP POLICY IF EXISTS "xoxo_games_insert" ON public.xoxo_games;
CREATE POLICY "xoxo_games_insert" ON public.xoxo_games
    FOR INSERT WITH CHECK (id = 'family_main' AND player_x <> player_o);

DROP POLICY IF EXISTS "xoxo_games_update" ON public.xoxo_games;
CREATE POLICY "xoxo_games_update" ON public.xoxo_games
    FOR UPDATE USING (id = 'family_main')
    WITH CHECK (id = 'family_main' AND player_x <> player_o);

-- Presence policies
DROP POLICY IF EXISTS "family_presence_select" ON public.family_presence;
CREATE POLICY "family_presence_select" ON public.family_presence
    FOR SELECT USING (member_id IN ('MITHUN', 'MAFITHA', 'ZAYN'));

DROP POLICY IF EXISTS "family_presence_insert" ON public.family_presence;
CREATE POLICY "family_presence_insert" ON public.family_presence
    FOR INSERT WITH CHECK (member_id IN ('MITHUN', 'MAFITHA', 'ZAYN'));

DROP POLICY IF EXISTS "family_presence_update" ON public.family_presence;
CREATE POLICY "family_presence_update" ON public.family_presence
    FOR UPDATE USING (member_id IN ('MITHUN', 'MAFITHA', 'ZAYN'))
    WITH CHECK (member_id IN ('MITHUN', 'MAFITHA', 'ZAYN'));

-- Parent logs policies
DROP POLICY IF EXISTS "family_parent_logs_select" ON public.family_parent_logs;
CREATE POLICY "family_parent_logs_select" ON public.family_parent_logs
    FOR SELECT USING (author_id IN ('MITHUN', 'MAFITHA', 'ZAYN'));

DROP POLICY IF EXISTS "family_parent_logs_insert" ON public.family_parent_logs;
CREATE POLICY "family_parent_logs_insert" ON public.family_parent_logs
    FOR INSERT WITH CHECK (author_id IN ('MITHUN', 'MAFITHA', 'ZAYN'));

DROP POLICY IF EXISTS "family_parent_logs_update" ON public.family_parent_logs;
CREATE POLICY "family_parent_logs_update" ON public.family_parent_logs
    FOR UPDATE USING (author_id IN ('MITHUN', 'MAFITHA', 'ZAYN'))
    WITH CHECK (author_id IN ('MITHUN', 'MAFITHA', 'ZAYN'));

-- Call logs policies
DROP POLICY IF EXISTS "family_call_logs_select" ON public.family_call_logs;
CREATE POLICY "family_call_logs_select" ON public.family_call_logs
    FOR SELECT USING (caller_id IN ('MITHUN', 'MAFITHA', 'ZAYN'));

DROP POLICY IF EXISTS "family_call_logs_insert" ON public.family_call_logs;
CREATE POLICY "family_call_logs_insert" ON public.family_call_logs
    FOR INSERT WITH CHECK (caller_id IN ('MITHUN', 'MAFITHA', 'ZAYN'));

-- =============================================================================
-- 7. Supabase Storage Bucket ('family-media') for Profile Pics, Voice & Chat Media
-- =============================================================================
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'family-media',
    'family-media',
    TRUE,
    26214400, -- 25 MB max per file
    ARRAY['image/jpeg', 'image/png', 'image/webp', 'audio/mp4', 'audio/m4a', 'audio/aac', 'audio/x-m4a', 'video/mp4']
)
ON CONFLICT (id) DO UPDATE SET
    public = EXCLUDED.public,
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

DROP POLICY IF EXISTS "family_media_public_read" ON storage.objects;
CREATE POLICY "family_media_public_read" ON storage.objects
    FOR SELECT USING (bucket_id = 'family-media');

DROP POLICY IF EXISTS "family_media_upload" ON storage.objects;
CREATE POLICY "family_media_upload" ON storage.objects
    FOR INSERT WITH CHECK (bucket_id = 'family-media');

DROP POLICY IF EXISTS "family_media_update" ON storage.objects;
CREATE POLICY "family_media_update" ON storage.objects
    FOR UPDATE USING (bucket_id = 'family-media')
    WITH CHECK (bucket_id = 'family-media');
