CREATE TABLE IF NOT EXISTS track_updates (
    track_id text NOT NULL,
    observed_at timestamptz NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    altitude double precision,
    heading double precision,
    identity text NOT NULL,
    PRIMARY KEY (track_id, observed_at)
);

CREATE INDEX IF NOT EXISTS track_updates_observed_at_idx
    ON track_updates (observed_at);
