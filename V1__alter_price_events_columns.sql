-- Convert event_id to bigint
ALTER TABLE price_events 
ALTER COLUMN event_id TYPE bigint USING event_id::bigint;

-- Convert price_id to bigint
ALTER TABLE price_events 
ALTER COLUMN price_id TYPE bigint USING price_id::bigint;

-- Convert rule_id to bigint
ALTER TABLE price_events 
ALTER COLUMN rule_id TYPE bigint USING rule_id::bigint;

-- Add new columns
ALTER TABLE price_events 
ADD COLUMN IF NOT EXISTS margin numeric(10,2),
ADD COLUMN IF NOT EXISTS markup_percent numeric(10,2);

-- Modify existing numeric columns
ALTER TABLE price_events 
ALTER COLUMN base_price TYPE numeric(10,2),
ALTER COLUMN selling_price TYPE numeric(10,2);
