-- ==========================================
-- SCRIPT SQL COMPLETO PARA SUPABASE - AFRODITA (FASE 2)
-- ==========================================

-- 1. Tabla de Salas
CREATE TABLE IF NOT EXISTS public.rooms (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code TEXT UNIQUE NOT NULL,
  host_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  num_couples INT NOT NULL DEFAULT 1,
  response_mode TEXT DEFAULT 'solo',
  intensity TEXT DEFAULT 'medio',
  distance_mode TEXT DEFAULT 'separados',
  status TEXT NOT NULL DEFAULT 'waiting',
  current_turn INT DEFAULT 0,
  current_round INT DEFAULT 1,
  current_card JSONB DEFAULT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.rooms ADD COLUMN IF NOT EXISTS intensity TEXT DEFAULT 'medio';
ALTER TABLE public.rooms ADD COLUMN IF NOT EXISTS response_mode TEXT DEFAULT 'solo';
ALTER TABLE public.rooms ADD COLUMN IF NOT EXISTS distance_mode TEXT DEFAULT 'separados';

-- 2. Tabla de Jugadores por Sala
CREATE TABLE IF NOT EXISTS public.room_players (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  room_id UUID NOT NULL REFERENCES public.rooms(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  couple_name TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending', -- 'pending', 'accepted', 'rejected'
  toys TEXT[] DEFAULT '{}',
  positions TEXT[] DEFAULT '{}',
  joined_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.room_players ADD COLUMN IF NOT EXISTS toys TEXT[] DEFAULT '{}';
ALTER TABLE public.room_players ADD COLUMN IF NOT EXISTS positions TEXT[] DEFAULT '{}';

-- 3. Tabla de Chat en Tiempo Real
CREATE TABLE IF NOT EXISTS public.chat_messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  room_id UUID NOT NULL REFERENCES public.rooms(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  couple_name TEXT NOT NULL,
  message TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

-- 4. Habilitar RLS (Row Level Security)
ALTER TABLE public.rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.room_players ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;

-- Políticas para rooms
DROP POLICY IF EXISTS "rooms_select" ON public.rooms;
DROP POLICY IF EXISTS "rooms_insert" ON public.rooms;
DROP POLICY IF EXISTS "rooms_update" ON public.rooms;

CREATE POLICY "rooms_select" ON public.rooms FOR SELECT TO authenticated USING (true);
CREATE POLICY "rooms_insert" ON public.rooms FOR INSERT TO authenticated WITH CHECK (auth.uid() = host_id);
CREATE POLICY "rooms_update" ON public.rooms FOR UPDATE TO authenticated USING (true);

-- Políticas para room_players
DROP POLICY IF EXISTS "room_players_select" ON public.room_players;
DROP POLICY IF EXISTS "room_players_insert" ON public.room_players;
DROP POLICY IF EXISTS "room_players_update" ON public.room_players;

CREATE POLICY "room_players_select" ON public.room_players FOR SELECT TO authenticated USING (true);
CREATE POLICY "room_players_insert" ON public.room_players FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
CREATE POLICY "room_players_update" ON public.room_players FOR UPDATE TO authenticated USING (true);

-- Políticas estrictas para chat_messages: Solo usuarios cuya invitación tenga status = 'accepted' en la sala
DROP POLICY IF EXISTS "chat_select" ON public.chat_messages;
DROP POLICY IF EXISTS "chat_insert" ON public.chat_messages;
DROP POLICY IF EXISTS "chat_select_accepted" ON public.chat_messages;
DROP POLICY IF EXISTS "chat_insert_accepted" ON public.chat_messages;

CREATE POLICY "chat_select_accepted" ON public.chat_messages
  FOR SELECT TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.room_players
      WHERE room_players.room_id = chat_messages.room_id
        AND room_players.user_id = auth.uid()
        AND room_players.status = 'accepted'
    )
  );

CREATE POLICY "chat_insert_accepted" ON public.chat_messages
  FOR INSERT TO authenticated
  WITH CHECK (
    auth.uid() = user_id AND
    EXISTS (
      SELECT 1 FROM public.room_players
      WHERE room_players.room_id = chat_messages.room_id
        AND room_players.user_id = auth.uid()
        AND room_players.status = 'accepted'
    )
  );

-- 5. Habilitar Realtime
ALTER PUBLICATION supabase_realtime ADD TABLE public.rooms;
ALTER PUBLICATION supabase_realtime ADD TABLE public.room_players;
ALTER PUBLICATION supabase_realtime ADD TABLE public.chat_messages;
