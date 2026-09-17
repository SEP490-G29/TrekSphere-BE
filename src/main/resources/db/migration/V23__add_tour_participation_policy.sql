CREATE TABLE IF NOT EXISTS tour_participation_policy (
    policy_id UUID PRIMARY KEY,
    tour_id UUID NOT NULL UNIQUE,
    min_age INTEGER,
    max_age INTEGER,
    min_height_cm DECIMAL(5,2),
    max_height_cm DECIMAL(5,2),
    min_weight_kg DECIMAL(5,2),
    max_weight_kg DECIMAL(5,2),
    fitness_level VARCHAR(20) NOT NULL DEFAULT 'ANY',
    health_requirements TEXT,
    restricted_medical_conditions TEXT,
    required_experience TEXT,
    required_skills TEXT,
    required_equipment TEXT,
    required_documents TEXT,
    requires_health_declaration BOOLEAN NOT NULL DEFAULT FALSE,
    requires_medical_certificate BOOLEAN NOT NULL DEFAULT FALSE,
    guardian_required_under_age INTEGER,
    additional_requirements TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_policy_tour FOREIGN KEY (tour_id) REFERENCES tour(tour_id) ON DELETE CASCADE,
    CONSTRAINT chk_policy_age CHECK (max_age IS NULL OR min_age IS NULL OR max_age >= min_age)
);

CREATE INDEX IF NOT EXISTS ix_tour_policy_tour_id
    ON tour_participation_policy (tour_id)
    WHERE is_deleted = FALSE;
