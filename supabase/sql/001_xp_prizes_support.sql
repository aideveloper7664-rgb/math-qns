-- Math Baazi: XP update, prize_distribution, support config
-- Run in Supabase SQL Editor. Review status names against your game_sessions data first.

-- 1) Support / social links (app reads game_config key = 'support')
INSERT INTO public.game_config(key, value) VALUES
 ('support', '{"email":"support@mathbaazi.app","whatsapp":"+91XXXXXXXXXX","telegram":"https://t.me/mathbaazi","instagram":"https://instagram.com/mathbaazi","facebook":"https://facebook.com/mathbaazi","youtube":"https://youtube.com/@mathbaazi","twitter":"https://twitter.com/mathbaazi"}'::jsonb)
ON CONFLICT (key) DO NOTHING;

-- 2) Admin-set prizes per tournament, e.g. {"1":5,"2":3,"3":1}
ALTER TABLE public.tournaments          ADD COLUMN IF NOT EXISTS prize_distribution jsonb DEFAULT '{}'::jsonb;
ALTER TABLE public.knockout_tournaments ADD COLUMN IF NOT EXISTS prize_distribution jsonb DEFAULT '{}'::jsonb;

-- 3) XP / matches / wins on game end (fires once when status changes to a finished state)
CREATE OR REPLACE FUNCTION public.trg_game_end_xp() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE v_xp int; v_win boolean;
BEGIN
  v_win := COALESCE(NEW.prize_earned,0) > 0;
  v_xp  := COALESCE(NEW.correct_answers,0) * 5 + CASE WHEN v_win THEN 20 ELSE 0 END;
  UPDATE public.users SET
    xp = COALESCE(xp,0) + v_xp,
    matches_played = COALESCE(matches_played,0) + 1,
    wins   = COALESCE(wins,0)   + CASE WHEN v_win THEN 1 ELSE 0 END,
    losses = COALESCE(losses,0) + CASE WHEN v_win THEN 0 ELSE 1 END,
    mmr    = GREATEST(0, COALESCE(mmr,1000) + CASE WHEN v_win THEN 10 ELSE -5 END)
  WHERE id = NEW.user_id;
  RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS game_end_xp ON public.game_sessions;
CREATE TRIGGER game_end_xp AFTER UPDATE OF status ON public.game_sessions
FOR EACH ROW
WHEN (OLD.status IS DISTINCT FROM NEW.status AND upper(NEW.status) IN ('COMPLETED','ENDED','FINISHED','GAME_OVER','WON','LOST'))
EXECUTE FUNCTION public.trg_game_end_xp();
-- NOTE: if your submit_answer/end_game already updates xp, drop this trigger to avoid double counting.
