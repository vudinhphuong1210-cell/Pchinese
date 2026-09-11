-- F04 media intake is YouTube-only.  Keep legacy rows intact while enforcing
-- these constraints for every newly created or subsequently changed asset.
ALTER TABLE public.media_assets
    ADD CONSTRAINT ck_media_assets_youtube_provider
        CHECK (provider_name = 'YOUTUBE') NOT VALID,
    ADD CONSTRAINT ck_media_assets_youtube_video_kind
        CHECK (media_kind = 'VIDEO') NOT VALID,
    ADD CONSTRAINT ck_media_assets_youtube_video_id
        CHECK (provider_asset_identifier ~ '^[A-Za-z0-9_-]{11}$') NOT VALID;

-- A reviewed asset keeps its source identity forever.  A replacement video is
-- a new asset with a separate review and audit trail.
CREATE OR REPLACE FUNCTION public.reject_media_asset_source_replacement()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.provider_name IS DISTINCT FROM OLD.provider_name
       OR NEW.provider_asset_identifier IS DISTINCT FROM OLD.provider_asset_identifier
       OR NEW.media_kind IS DISTINCT FROM OLD.media_kind THEN
        RAISE EXCEPTION 'Media asset source is immutable; create a replacement media asset instead.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_media_assets_source_immutable
    BEFORE UPDATE ON public.media_assets
    FOR EACH ROW
    EXECUTE FUNCTION public.reject_media_asset_source_replacement();
