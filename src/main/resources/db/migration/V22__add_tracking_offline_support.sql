CREATE TABLE tracking_device_session (
    tracking_device_session_id UUID PRIMARY KEY,
    tour_session_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    device_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tracking_device_session_tour_session
        FOREIGN KEY (tour_session_id) REFERENCES tour_session(tour_session_id) ON DELETE CASCADE,
    CONSTRAINT fk_tracking_device_session_actor
        FOREIGN KEY (actor_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uq_tracking_device_session UNIQUE (tour_session_id, actor_id, device_id),
    CONSTRAINT ck_tracking_device_session_status
        CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED'))
);

CREATE INDEX idx_tracking_device_session_actor_status
    ON tracking_device_session(actor_id, status);
CREATE INDEX idx_tracking_device_session_expires_at
    ON tracking_device_session(expires_at);

CREATE TABLE tracking_session_revision (
    tour_session_id UUID PRIMARY KEY,
    revision BIGINT NOT NULL DEFAULT 0,
    last_event_id UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tracking_revision_tour_session
        FOREIGN KEY (tour_session_id) REFERENCES tour_session(tour_session_id) ON DELETE CASCADE,
    CONSTRAINT ck_tracking_revision_non_negative CHECK (revision >= 0)
);

CREATE TABLE tracking_ingested_event (
    tracking_ingested_event_id UUID PRIMARY KEY,
    client_event_id UUID NOT NULL,
    tour_session_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    device_id UUID NOT NULL,
    sequence_number BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    payload JSONB NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    base_revision BIGINT,
    result_revision BIGINT,
    processing_status VARCHAR(20) NOT NULL,
    error_code VARCHAR(100),
    result_message VARCHAR(500),
    resource_type VARCHAR(50),
    resource_id UUID,
    CONSTRAINT fk_tracking_event_tour_session
        FOREIGN KEY (tour_session_id) REFERENCES tour_session(tour_session_id) ON DELETE CASCADE,
    CONSTRAINT fk_tracking_event_actor
        FOREIGN KEY (actor_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uq_tracking_event_client
        UNIQUE (actor_id, device_id, client_event_id),
    CONSTRAINT uq_tracking_event_sequence
        UNIQUE (tour_session_id, actor_id, device_id, sequence_number),
    CONSTRAINT ck_tracking_event_sequence CHECK (sequence_number >= 0),
    CONSTRAINT ck_tracking_event_status
        CHECK (processing_status IN ('RECEIVED', 'ACCEPTED', 'REJECTED', 'CONFLICT'))
);

CREATE INDEX idx_tracking_event_session_received
    ON tracking_ingested_event(tour_session_id, received_at DESC);
CREATE INDEX idx_tracking_event_session_type_occurred
    ON tracking_ingested_event(tour_session_id, event_type, occurred_at);
CREATE INDEX idx_tracking_event_status_received
    ON tracking_ingested_event(processing_status, received_at);
CREATE INDEX idx_tracking_event_result_revision
    ON tracking_ingested_event(tour_session_id, result_revision)
    WHERE result_revision IS NOT NULL;

CREATE TABLE tracking_location_sample (
    sample_id UUID PRIMARY KEY,
    tour_session_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    device_id UUID NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    accuracy_meters DECIMAL(8,2),
    speed_mps DECIMAL(8,2),
    heading_degrees DECIMAL(6,2),
    validation_status VARCHAR(20) NOT NULL,
    is_late BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_tracking_location_tour_session
        FOREIGN KEY (tour_session_id) REFERENCES tour_session(tour_session_id) ON DELETE CASCADE,
    CONSTRAINT fk_tracking_location_actor
        FOREIGN KEY (actor_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT ck_tracking_location_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_tracking_location_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_tracking_location_accuracy CHECK (accuracy_meters IS NULL OR accuracy_meters >= 0),
    CONSTRAINT ck_tracking_location_heading
        CHECK (heading_degrees IS NULL OR (heading_degrees >= 0 AND heading_degrees < 360)),
    CONSTRAINT ck_tracking_location_status
        CHECK (validation_status IN ('VALID', 'LOW_ACCURACY', 'OUTLIER'))
);

CREATE INDEX idx_tracking_location_session_actor_recorded
    ON tracking_location_sample(tour_session_id, actor_id, recorded_at DESC);
CREATE INDEX idx_tracking_location_session_recorded
    ON tracking_location_sample(tour_session_id, recorded_at);
CREATE INDEX idx_tracking_location_received
    ON tracking_location_sample(received_at);
