ALTER TABLE movements
    ALTER COLUMN movement_date SET DEFAULT (now() AT TIME ZONE 'America/Guayaquil');
