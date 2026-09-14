CREATE TABLE tour_behavior_event (
    behavior_event_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    tour_id UUID NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    source VARCHAR(30) NOT NULL,
    session_id VARCHAR(100),
    display_position INTEGER,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tbe_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_tbe_tour FOREIGN KEY (tour_id) REFERENCES tour(tour_id) ON DELETE CASCADE,
    CONSTRAINT chk_tbe_event_type CHECK (
        event_type IN ('IMPRESSION', 'VIEW', 'CLICK', 'SAVE', 'UNSAVE', 'DISMISS')
    ),
    CONSTRAINT chk_tbe_source CHECK (
        source IN ('RECOMMENDATION', 'HOME', 'SEARCH', 'TOUR_LIST', 'DIRECT', 'OTHER')
    ),
    CONSTRAINT chk_tbe_display_position CHECK (
        display_position IS NULL OR display_position BETWEEN 0 AND 1000
    )
);

CREATE INDEX ix_tbe_user_occurred
    ON tour_behavior_event (user_id, occurred_at DESC);

CREATE INDEX ix_tbe_user_tour_type_occurred
    ON tour_behavior_event (user_id, tour_id, event_type, occurred_at DESC);
