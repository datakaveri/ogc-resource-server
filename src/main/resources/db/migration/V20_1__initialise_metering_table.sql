-- Create the metering table

CREATE TABLE IF NOT EXISTS metering (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    collection_id UUID NOT NULL,
    api_path TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    resp_size BIGINT NOT NULL
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_metering_user_id ON metering(user_id);
CREATE INDEX IF NOT EXISTS idx_metering_collection_id ON metering(collection_id);
CREATE INDEX IF NOT EXISTS idx_metering_api_path ON metering(api_path);
CREATE INDEX IF NOT EXISTS idx_metering_timestamp ON metering(timestamp);

-- Composite index to optimize user/collection/time-based queries
CREATE INDEX IF NOT EXISTS idx_metering_user_collection_time
    ON metering(user_id, collection_id, timestamp);

ALTER TABLE metering OWNER TO ${flyway:user};
GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE metering TO ${ogcUser};

-- Grant usage on metering id sequence
GRANT USAGE ON SEQUENCE metering_id_seq TO ${ogcUser};