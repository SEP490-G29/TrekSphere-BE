-- ----------------------------------------------------------------------------
-- RBAC
-- ----------------------------------------------------------------------------

CREATE TABLE role (
    role_id UUID PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE permission (
    permission_id UUID PRIMARY KEY,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE role_permission (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES role(role_id),
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permission(permission_id)
);

CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    phone VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(10),
    avatar_url VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(255),
    bio TEXT,
    experience_level VARCHAR(30),
    preferred_difficulty VARCHAR(20),
    preferred_areas JSONB NOT NULL DEFAULT '[]'::JSONB,
    skills JSONB NOT NULL DEFAULT '[]'::JSONB,
    trust_score SMALLINT,
    trust_review_count INTEGER NOT NULL DEFAULT 0,
    trust_calculated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE','LOCKED','DEACTIVATED')),
    CONSTRAINT chk_users_gender CHECK (gender IS NULL OR gender IN ('MALE','FEMALE','OTHER')),
    CONSTRAINT chk_users_provider CHECK (provider IN ('LOCAL','GOOGLE')),
    CONSTRAINT chk_users_experience_level CHECK (experience_level IS NULL OR experience_level IN ('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT')),
    CONSTRAINT chk_users_preferred_difficulty CHECK (preferred_difficulty IS NULL OR preferred_difficulty IN ('EASY','MODERATE','HARD','EXTREME')),
    CONSTRAINT chk_users_preferred_areas_array CHECK (jsonb_typeof(preferred_areas) = 'array'),
    CONSTRAINT chk_users_skills_array CHECK (jsonb_typeof(skills) = 'array'),
    CONSTRAINT chk_users_trust_score CHECK (trust_score IS NULL OR trust_score BETWEEN 0 AND 100)
);

CREATE TABLE user_role (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES role(role_id)
);

-- ----------------------------------------------------------------------------
-- Vendor
-- ----------------------------------------------------------------------------

CREATE TABLE vendor (
    vendor_id UUID PRIMARY KEY,
    manager_id UUID NOT NULL UNIQUE,
    company_name VARCHAR(255) NOT NULL,
    description TEXT,
    logo_url VARCHAR(500),
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    tax_code VARCHAR(50) UNIQUE NOT NULL,
    business_license_url VARCHAR(500) NOT NULL,
    business_address VARCHAR(500),
    legal_representative_name VARCHAR(255),
    legal_representative_position VARCHAR(255),
    website_url VARCHAR(500),
    status VARCHAR(10) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_vendor_manager FOREIGN KEY (manager_id) REFERENCES users(user_id),
    CONSTRAINT chk_vendor_status CHECK (status IN ('PENDING','ACTIVE','SUSPENDED'))
);

CREATE TABLE vendor_application (
    vendor_application_id UUID PRIMARY KEY,
    applicant_id UUID NOT NULL,
    company_name VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    tax_code VARCHAR(50) UNIQUE,
    business_license_url VARCHAR(500),
    business_description TEXT,
    application_status VARCHAR(10) NOT NULL,
    rejection_reason TEXT,
    business_address VARCHAR(500),
    legal_representative_name VARCHAR(255),
    legal_representative_position VARCHAR(255),
    website_url VARCHAR(500),
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    vendor_id UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_va_applicant FOREIGN KEY (applicant_id) REFERENCES users(user_id),
    CONSTRAINT fk_va_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(user_id),
    CONSTRAINT fk_va_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(vendor_id),
    CONSTRAINT uq_va_vendor UNIQUE (vendor_id),
    CONSTRAINT chk_va_status CHECK (application_status IN ('DRAFT','PENDING','APPROVED','REJECTED')),
    CONSTRAINT chk_va_submission_complete CHECK (
        application_status = 'DRAFT'
        OR (
            NULLIF(BTRIM(company_name), '') IS NOT NULL
            AND NULLIF(BTRIM(contact_email), '') IS NOT NULL
            AND NULLIF(BTRIM(contact_phone), '') IS NOT NULL
            AND NULLIF(BTRIM(tax_code), '') IS NOT NULL
            AND NULLIF(BTRIM(business_license_url), '') IS NOT NULL
            AND NULLIF(BTRIM(business_description), '') IS NOT NULL
            AND NULLIF(BTRIM(business_address), '') IS NOT NULL
            AND NULLIF(BTRIM(legal_representative_name), '') IS NOT NULL
            AND NULLIF(BTRIM(legal_representative_position), '') IS NOT NULL
        )
    ) NOT VALID
);

CREATE INDEX ix_vendor_application_applicant_status
    ON vendor_application (applicant_id, application_status)
    WHERE is_deleted = FALSE;

-- ----------------------------------------------------------------------------
-- Tour
-- ----------------------------------------------------------------------------

CREATE TABLE tour (
    tour_id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    tour_name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    duration_days INTEGER NOT NULL,
    min_capacity INTEGER NOT NULL DEFAULT 1,
    max_capacity INTEGER NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    total_distance_km DECIMAL(5,2),
    difficulty VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    cover_image_url VARCHAR(500),
    location VARCHAR(255) NOT NULL,
    highlights TEXT,
    includes TEXT,
    excludes TEXT,
    rejection_reason TEXT,
    published_at TIMESTAMP,
    hidden_reason TEXT,
    hidden_at TIMESTAMP,
    hidden_by UUID,
    creator_id UUID NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_tour_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(vendor_id),
    CONSTRAINT fk_tour_creator FOREIGN KEY (creator_id) REFERENCES users(user_id),
    CONSTRAINT fk_tour_hidden_by FOREIGN KEY (hidden_by) REFERENCES users(user_id),
    CONSTRAINT chk_tour_capacity CHECK (max_capacity >= min_capacity),
    CONSTRAINT chk_tour_difficulty CHECK (difficulty IN ('EASY','MODERATE','HARD','EXTREME')),
    CONSTRAINT chk_tour_status CHECK (status IN ('DRAFT','PUBLISHED','HIDDEN')),
    CONSTRAINT chk_tour_price CHECK (price >= 0)
);

CREATE INDEX ix_tour_vendor_status_active ON tour (vendor_id, status, is_deleted);
CREATE INDEX ix_tour_published_at ON tour (published_at DESC) WHERE status = 'PUBLISHED' AND is_deleted = FALSE;

CREATE TABLE tour_image (
    tour_image_id UUID PRIMARY KEY,
    tour_id UUID NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    caption VARCHAR(255),
    sort_order INTEGER NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_ti_tour FOREIGN KEY (tour_id) REFERENCES tour(tour_id)
);

CREATE TABLE tour_schedule (
    tour_schedule_id UUID PRIMARY KEY,
    tour_id UUID NOT NULL,
    departure_date DATE NOT NULL,
    return_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    cancellation_reason TEXT,
    cancelled_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_ts_tour FOREIGN KEY (tour_id) REFERENCES tour(tour_id),
    CONSTRAINT chk_ts_dates CHECK (return_date >= departure_date),
    CONSTRAINT chk_ts_status CHECK (status IN ('OPEN','CLOSED','CANCELLED'))
);

CREATE INDEX ix_tour_schedule_tour_status_departure ON tour_schedule (tour_id, status, departure_date, is_deleted);

CREATE TABLE tour_checkpoint (
    tour_checkpoint_id UUID PRIMARY KEY,
    tour_id UUID NOT NULL,
    checkpoint_name VARCHAR(255) NOT NULL,
    description TEXT,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    altitude DECIMAL(7,1),
    checkpoint_order INTEGER NOT NULL,
    checkpoint_image_url VARCHAR(500),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_tc_tour FOREIGN KEY (tour_id) REFERENCES tour(tour_id),
    CONSTRAINT chk_tc_order CHECK (checkpoint_order > 0)
);

CREATE TABLE tour_participation_policy (
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

CREATE INDEX ix_tour_policy_tour_id ON tour_participation_policy (tour_id) WHERE is_deleted = FALSE;

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

CREATE INDEX ix_tbe_user_occurred ON tour_behavior_event (user_id, occurred_at DESC);
CREATE INDEX ix_tbe_user_tour_type_occurred ON tour_behavior_event (user_id, tour_id, event_type, occurred_at DESC);

-- ----------------------------------------------------------------------------
-- Chat 
-- ----------------------------------------------------------------------------

CREATE TABLE conversation (
    conversation_id UUID PRIMARY KEY,
    title VARCHAR(255),
    conversation_type VARCHAR(20) NOT NULL,
    last_message_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT chk_conversation_type CHECK (conversation_type IN ('DIRECT','GROUP'))
);

CREATE TABLE conversation_participant (
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT fk_cp_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(conversation_id),
    CONSTRAINT fk_cp_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE message (
    message_id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    content TEXT,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    attachment_storage_id VARCHAR(500),
    attachment_url VARCHAR(1000),
    attachment_name VARCHAR(255),
    attachment_mime_type VARCHAR(255),
    attachment_size_bytes BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(conversation_id),
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users(user_id),
    CONSTRAINT chk_message_type CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE', 'SYSTEM')),
    CONSTRAINT chk_message_payload CHECK (
        (message_type IN ('TEXT', 'SYSTEM')
            AND NULLIF(BTRIM(content), '') IS NOT NULL
            AND attachment_url IS NULL)
        OR
        (message_type IN ('IMAGE', 'FILE')
            AND attachment_url IS NOT NULL
            AND attachment_name IS NOT NULL
            AND attachment_mime_type IS NOT NULL
            AND attachment_size_bytes > 0)
    )
);

CREATE INDEX ix_message_conversation_created ON message (conversation_id, created_at);

-- ----------------------------------------------------------------------------
-- Nhóm ghép 
-- ----------------------------------------------------------------------------

CREATE TABLE matching_group (
    matching_group_id UUID PRIMARY KEY,
    tour_id UUID,
    owner_id UUID NOT NULL,
    group_name VARCHAR(255) NOT NULL,
    description TEXT,
    max_size INTEGER NOT NULL,
    current_size INTEGER NOT NULL DEFAULT 1,
    target_date DATE NOT NULL,
    matching_deadline TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    conversation_id UUID,
    cover_image_url VARCHAR(500),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_mg_tour FOREIGN KEY (tour_id) REFERENCES tour(tour_id),
    CONSTRAINT fk_mg_owner FOREIGN KEY (owner_id) REFERENCES users(user_id),
    CONSTRAINT fk_mg_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(conversation_id),
    CONSTRAINT chk_matching_group_current_size CHECK (current_size >= 1 AND current_size <= max_size),
    CONSTRAINT chk_matching_group_status CHECK (status IN ('OPEN','FULL','CLOSED','HIDDEN','IN_PROGRESS','COMPLETED','CANCELLED'))
);

CREATE INDEX ix_matching_group_tour_status_active ON matching_group (tour_id, status, is_deleted);
CREATE INDEX ix_matching_group_owner_completed_tour
    ON matching_group (owner_id, tour_id)
    WHERE is_deleted = FALSE AND status = 'COMPLETED' AND tour_id IS NOT NULL;

CREATE TABLE group_join_application (
    application_id UUID PRIMARY KEY,
    matching_group_id UUID NOT NULL,
    applicant_user_id UUID NOT NULL,
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    reject_reason TEXT,
    withdrawn_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_gja_group FOREIGN KEY (matching_group_id) REFERENCES matching_group(matching_group_id),
    CONSTRAINT fk_gja_applicant FOREIGN KEY (applicant_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_gja_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(user_id),
    CONSTRAINT chk_gja_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT chk_gja_withdrawn_at CHECK (
        (status = 'WITHDRAWN' AND withdrawn_at IS NOT NULL)
        OR (status <> 'WITHDRAWN' AND withdrawn_at IS NULL)
    )
);

CREATE UNIQUE INDEX uq_group_applicant_pending
    ON group_join_application (matching_group_id, applicant_user_id)
    WHERE status = 'PENDING' AND is_deleted = FALSE;
CREATE INDEX ix_gja_group_status ON group_join_application (matching_group_id, status);
CREATE INDEX ix_gja_applicant_status ON group_join_application (applicant_user_id, status);

CREATE TABLE matching_member (
    matching_member_id UUID PRIMARY KEY,
    matching_group_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    source_application_id UUID,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    withdrawn_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_mm_group FOREIGN KEY (matching_group_id) REFERENCES matching_group(matching_group_id),
    CONSTRAINT fk_mm_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_mm_source_application FOREIGN KEY (source_application_id) REFERENCES group_join_application(application_id),
    CONSTRAINT uq_mm_group_user UNIQUE (matching_group_id, user_id),
    CONSTRAINT chk_mm_role CHECK (role IN ('LEADER','MEMBER')),
    CONSTRAINT chk_mm_status CHECK (status IN ('PENDING','ACCEPTED','REJECTED','WITHDRAWN','LEFT','REMOVED')),
    CONSTRAINT chk_mm_withdrawn_at CHECK (
        (status = 'WITHDRAWN' AND withdrawn_at IS NOT NULL)
        OR (status <> 'WITHDRAWN' AND withdrawn_at IS NULL)
    )
);

CREATE UNIQUE INDEX uq_group_active_leader
    ON matching_member (matching_group_id)
    WHERE role = 'LEADER' AND status = 'ACCEPTED' AND is_deleted = FALSE;
CREATE INDEX ix_matching_member_user_accepted_group
    ON matching_member (user_id, matching_group_id)
    WHERE is_deleted = FALSE AND status = 'ACCEPTED';



CREATE TABLE group_trip (
    group_trip_id UUID PRIMARY KEY,
    matching_group_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    scheduled_start_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    start_by UUID,
    ended_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_gt_group FOREIGN KEY (matching_group_id) REFERENCES matching_group(matching_group_id),
    CONSTRAINT fk_gt_start_by FOREIGN KEY (start_by) REFERENCES matching_member(matching_member_id),
    CONSTRAINT fk_gt_ended_by FOREIGN KEY (ended_by) REFERENCES matching_member(matching_member_id),
    CONSTRAINT chk_gt_status CHECK (status IN ('PLANNED','IN_PROGRESS','ENDED','CANCELLED'))
);

-- ----------------------------------------------------------------------------
-- Custom Journey (hành trình tự lên của nhóm)
-- ----------------------------------------------------------------------------

CREATE TABLE custom_journey (
    custom_journey_id UUID PRIMARY KEY,
    matching_group_id UUID NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    difficulty VARCHAR(20) NOT NULL DEFAULT 'MODERATE',
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    locked_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_cj_group FOREIGN KEY (matching_group_id) REFERENCES matching_group(matching_group_id),
    CONSTRAINT chk_cj_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_cj_difficulty CHECK (difficulty IN ('EASY','MODERATE','HARD','EXTREME'))
);

CREATE TABLE custom_journey_checkpoint (
    custom_journey_checkpoint_id UUID PRIMARY KEY,
    custom_journey_id UUID NOT NULL,
    day_no INTEGER,
    checkpoint_order INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    location_name VARCHAR(200),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    planned_start_at TIMESTAMP,
    planned_end_at TIMESTAMP,
    image_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress_updated_at TIMESTAMP,
    progress_updated_by_member_id UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_cjc_journey FOREIGN KEY (custom_journey_id) REFERENCES custom_journey(custom_journey_id),
    CONSTRAINT fk_checkpoint_checked_in_by_member FOREIGN KEY (progress_updated_by_member_id) REFERENCES matching_member(matching_member_id) ON DELETE SET NULL,
    CONSTRAINT chk_cjc_day_no CHECK (day_no IS NULL OR day_no > 0),
    CONSTRAINT chk_cjc_order CHECK (checkpoint_order > 0),
    CONSTRAINT chk_cjc_planned_times CHECK (planned_end_at IS NULL OR planned_start_at IS NULL OR planned_end_at >= planned_start_at)
);

CREATE INDEX ix_cjc_journey_day_order ON custom_journey_checkpoint (custom_journey_id, day_no, checkpoint_order);
-- Unique theo (journey, order) chỉ tính bản ghi chưa xoá mềm
CREATE UNIQUE INDEX uq_cjc_order_active
    ON custom_journey_checkpoint (custom_journey_id, checkpoint_order)
    WHERE is_deleted = FALSE;

CREATE TABLE custom_journey_cost_item (
    custom_journey_cost_item_id UUID PRIMARY KEY,
    custom_journey_id UUID NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    category VARCHAR(50),
    estimated_amount DECIMAL(12,2) NOT NULL,
    note TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_cjci_journey FOREIGN KEY (custom_journey_id) REFERENCES custom_journey(custom_journey_id),
    CONSTRAINT chk_cjci_category CHECK (category IS NULL OR category IN ('PERMIT','GUIDE','FOOD','TRANSPORT','GEAR','OTHER')),
    CONSTRAINT chk_cjci_amount CHECK (estimated_amount >= 0)
);

CREATE TABLE custom_journey_activity (
    custom_journey_activity_id UUID PRIMARY KEY,
    custom_journey_id UUID NOT NULL,
    custom_journey_checkpoint_id UUID,
    day_no INTEGER NOT NULL,
    time_slot VARCHAR(20) NOT NULL,
    activity_order INTEGER NOT NULL DEFAULT 1,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    planned_start_at VARCHAR(50),
    planned_end_at VARCHAR(50),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_cja_journey FOREIGN KEY (custom_journey_id) REFERENCES custom_journey(custom_journey_id) ON DELETE CASCADE,
    CONSTRAINT fk_cja_checkpoint FOREIGN KEY (custom_journey_checkpoint_id) REFERENCES custom_journey_checkpoint(custom_journey_checkpoint_id) ON DELETE SET NULL,
    CONSTRAINT chk_cja_day_no CHECK (day_no > 0),
    CONSTRAINT chk_cja_activity_order CHECK (activity_order > 0),
    CONSTRAINT chk_cja_time_slot CHECK (time_slot IN ('MORNING', 'NOON', 'AFTERNOON', 'EVENING'))
);

CREATE INDEX ix_cja_journey_day_slot ON custom_journey_activity (custom_journey_id, day_no, time_slot) WHERE is_deleted = FALSE;

-- ----------------------------------------------------------------------------
-- SOS — phát cho toàn bộ thành viên nhóm
-- ----------------------------------------------------------------------------

CREATE TABLE sos_alert (
    sos_alert_id UUID PRIMARY KEY,
    group_trip_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    incident_type_code VARCHAR(50) NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    message TEXT,
    status VARCHAR(20) NOT NULL,
    resolved_by UUID,
    idempotency_key VARCHAR(255) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_sos_trip FOREIGN KEY (group_trip_id) REFERENCES group_trip(group_trip_id),
    CONSTRAINT fk_sos_sender FOREIGN KEY (sender_id) REFERENCES users(user_id),
    CONSTRAINT fk_sos_resolved FOREIGN KEY (resolved_by) REFERENCES users(user_id),
    CONSTRAINT uq_sos_idempotency UNIQUE (group_trip_id, sender_id, idempotency_key),
    CONSTRAINT chk_sos_incident_type CHECK (incident_type_code IN ('INJURY','LOST','WEATHER','SUPPLIES','OTHER')),
    CONSTRAINT chk_sos_status CHECK (status IN ('OPEN','ACKNOWLEDGED','RESOLVED','CANCELLED'))
);

-- ----------------------------------------------------------------------------
-- Shared Expense
-- ----------------------------------------------------------------------------

CREATE TABLE group_expense (
    group_expense_id UUID PRIMARY KEY,
    group_trip_id UUID NOT NULL,
    paid_by UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    beneficiary_scope VARCHAR(20) NOT NULL,
    beneficiary_count INTEGER NOT NULL,
    split_method VARCHAR(20) NOT NULL,
    spent_at TIMESTAMP NOT NULL,
    receipt_url VARCHAR(500),
    note TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_ge_trip FOREIGN KEY (group_trip_id) REFERENCES group_trip(group_trip_id),
    CONSTRAINT fk_ge_paid_by FOREIGN KEY (paid_by) REFERENCES matching_member(matching_member_id),
    CONSTRAINT chk_ge_amount CHECK (amount > 0),
    CONSTRAINT chk_ge_beneficiary_count CHECK (beneficiary_count > 0),
    CONSTRAINT chk_ge_beneficiary_scope CHECK (beneficiary_scope IN ('ALL_MEMBERS','SELECTED_MEMBERS')),
    CONSTRAINT chk_ge_split_method CHECK (split_method IN ('EQUAL','CUSTOM'))
);

CREATE INDEX ix_ge_trip_spent ON group_expense (group_trip_id, spent_at DESC);

CREATE TABLE group_expense_share (
    group_expense_share_id UUID PRIMARY KEY,
    group_expense_id UUID NOT NULL,
    matching_member_id UUID NOT NULL,
    share_amount DECIMAL(12,2) NOT NULL,
    reason VARCHAR(500),
    settlement_status VARCHAR(20) NOT NULL DEFAULT 'UNSETTLED',
    settled_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_ges_expense FOREIGN KEY (group_expense_id) REFERENCES group_expense(group_expense_id),
    CONSTRAINT fk_ges_member FOREIGN KEY (matching_member_id) REFERENCES matching_member(matching_member_id),
    CONSTRAINT chk_ges_share_amount CHECK (share_amount >= 0),
    CONSTRAINT chk_ges_settlement_status CHECK (settlement_status IN ('UNSETTLED','SETTLED'))
);

CREATE UNIQUE INDEX uq_ges_expense_member_active
    ON group_expense_share (group_expense_id, matching_member_id)
    WHERE is_deleted = FALSE;

CREATE TABLE group_settlement (
    group_settlement_id UUID PRIMARY KEY,
    group_trip_id UUID NOT NULL,
    from_matching_member_id UUID NOT NULL,
    to_matching_member_id UUID NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    proof_url VARCHAR(500),
    submitted_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    confirmed_by UUID,
    reject_reason TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_gset_trip FOREIGN KEY (group_trip_id) REFERENCES group_trip(group_trip_id),
    CONSTRAINT fk_gset_from FOREIGN KEY (from_matching_member_id) REFERENCES matching_member(matching_member_id),
    CONSTRAINT fk_gset_to FOREIGN KEY (to_matching_member_id) REFERENCES matching_member(matching_member_id),
    CONSTRAINT fk_gset_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES matching_member(matching_member_id),
    CONSTRAINT chk_gset_parties CHECK (from_matching_member_id <> to_matching_member_id),
    CONSTRAINT chk_gset_amount CHECK (amount > 0),
    CONSTRAINT chk_gset_status CHECK (status IN ('PENDING','PROOF_SUBMITTED','CONFIRMED','REJECTED')),
    CONSTRAINT chk_gset_proof CHECK (status <> 'PROOF_SUBMITTED' OR proof_url IS NOT NULL)
);

-- ----------------------------------------------------------------------------
-- Checklist
-- ----------------------------------------------------------------------------

CREATE TABLE group_checklist_item (
    group_checklist_item_id UUID PRIMARY KEY,
    matching_group_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    item_scope VARCHAR(20) NOT NULL,
    item_type_code VARCHAR(50),
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    note TEXT,
    assignee_matching_member_id UUID,
    status VARCHAR(20) NOT NULL,
    completed_at TIMESTAMP,
    completed_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_gci_group FOREIGN KEY (matching_group_id) REFERENCES matching_group(matching_group_id),
    CONSTRAINT fk_gci_assignee FOREIGN KEY (assignee_matching_member_id) REFERENCES matching_member(matching_member_id),
    CONSTRAINT fk_gci_completed_by FOREIGN KEY (completed_by) REFERENCES matching_member(matching_member_id),
    CONSTRAINT chk_gci_scope CHECK (item_scope IN ('SHARED','PERSONAL')),
    CONSTRAINT chk_gci_type CHECK (item_type_code IS NULL OR item_type_code IN ('CLOTHING','TENT','MEDICAL','ELECTRONICS','OTHER')),
    CONSTRAINT chk_gci_status CHECK (status IN ('TODO','IN_PROGRESS','DONE')),
    CONSTRAINT chk_gci_completion CHECK (
        (status = 'DONE' AND completed_at IS NOT NULL AND completed_by IS NOT NULL)
        OR (status <> 'DONE' AND completed_at IS NULL AND completed_by IS NULL)
    )
);

CREATE INDEX ix_gci_group_scope_status ON group_checklist_item (matching_group_id, item_scope, status);


CREATE TABLE group_post (
    group_post_id UUID PRIMARY KEY,
    matching_group_id UUID NOT NULL,
    posted_by UUID NOT NULL,
    title VARCHAR(200),
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SHOW',
    post_type VARCHAR(30) NOT NULL DEFAULT 'DISCUSSION',
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    pinned_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_gp_group FOREIGN KEY (matching_group_id) REFERENCES matching_group(matching_group_id),
    CONSTRAINT fk_gp_posted_by FOREIGN KEY (posted_by) REFERENCES matching_member(matching_member_id),
    CONSTRAINT chk_gp_status CHECK (status IN ('SHOW','HIDDEN'))
);

CREATE INDEX ix_gp_group_created ON group_post (matching_group_id, created_at DESC);
CREATE INDEX idx_group_post_pinned_created ON group_post (matching_group_id, is_pinned DESC, created_at DESC);

CREATE TABLE group_post_image (
    group_post_id UUID NOT NULL,
    image_url VARCHAR(500) NOT NULL,

    CONSTRAINT fk_gpi_group_post FOREIGN KEY (group_post_id) REFERENCES group_post(group_post_id) ON DELETE CASCADE
);

CREATE INDEX ix_gpi_post_id ON group_post_image (group_post_id);

CREATE TABLE group_post_comment (
    group_post_comment_id UUID PRIMARY KEY,
    group_post_id UUID NOT NULL,
    answered_by UUID NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SHOW',
    parent_comment_id UUID,
    reply_to_comment_id UUID,
    reply_to_member_id UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_gpc_post FOREIGN KEY (group_post_id) REFERENCES group_post(group_post_id),
    CONSTRAINT fk_gpc_answered_by FOREIGN KEY (answered_by) REFERENCES matching_member(matching_member_id),
    CONSTRAINT fk_gpc_parent_comment FOREIGN KEY (parent_comment_id) REFERENCES group_post_comment(group_post_comment_id) ON DELETE CASCADE,
    CONSTRAINT fk_gpc_reply_to_comment FOREIGN KEY (reply_to_comment_id) REFERENCES group_post_comment(group_post_comment_id) ON DELETE SET NULL,
    CONSTRAINT fk_gpc_reply_to_member FOREIGN KEY (reply_to_member_id) REFERENCES matching_member(matching_member_id) ON DELETE SET NULL,
    CONSTRAINT chk_gpc_status CHECK (status IN ('SHOW','HIDDEN'))
);

CREATE INDEX ix_gpc_post_created ON group_post_comment (group_post_id, created_at);
CREATE INDEX ix_gpc_parent_comment ON group_post_comment (parent_comment_id, created_at ASC);
CREATE INDEX ix_gpc_reply_to_comment ON group_post_comment (reply_to_comment_id);



CREATE TABLE group_vote (
    group_vote_id UUID PRIMARY KEY,
    matching_group_id UUID NOT NULL,
    vote_type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    reason TEXT NOT NULL,
    created_by_member UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    opens_at TIMESTAMP NOT NULL,
    closes_at TIMESTAMP NOT NULL,
    eligible_voter_count INTEGER NOT NULL,
    closed_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_gv_group FOREIGN KEY (matching_group_id) REFERENCES matching_group(matching_group_id),
    CONSTRAINT fk_gv_created_by FOREIGN KEY (created_by_member) REFERENCES matching_member(matching_member_id),
    CONSTRAINT chk_gv_type CHECK (vote_type IN ('LEADER_ELECTION','GROUP_DISSOLUTION','OTHER')),
    CONSTRAINT chk_gv_status CHECK (status IN ('OPEN','CLOSED')),
    CONSTRAINT chk_gv_eligible_voter_count CHECK (eligible_voter_count > 0)
);

CREATE UNIQUE INDEX uq_gv_group_type_open
    ON group_vote (matching_group_id, vote_type)
    WHERE status = 'OPEN' AND is_deleted = FALSE;

CREATE TABLE group_vote_option (
    group_vote_option_id UUID PRIMARY KEY,
    group_vote_id UUID NOT NULL,
    option_order INTEGER NOT NULL,
    option_label VARCHAR(200) NOT NULL,
    candidate_matching_member_id UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_gvo_vote FOREIGN KEY (group_vote_id) REFERENCES group_vote(group_vote_id),
    CONSTRAINT fk_gvo_candidate FOREIGN KEY (candidate_matching_member_id) REFERENCES matching_member(matching_member_id),
    CONSTRAINT uq_gvo_vote_order UNIQUE (group_vote_id, option_order),
    CONSTRAINT chk_gvo_order CHECK (option_order > 0)
);

ALTER TABLE group_vote ADD COLUMN winning_option_id UUID;
ALTER TABLE group_vote ADD CONSTRAINT fk_gv_winning_option
    FOREIGN KEY (winning_option_id) REFERENCES group_vote_option(group_vote_option_id);

CREATE TABLE group_vote_ballot (
    group_vote_ballot_id UUID PRIMARY KEY,
    group_vote_id UUID NOT NULL,
    group_vote_option_id UUID NOT NULL,
    voter_matching_member_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_gvb_vote FOREIGN KEY (group_vote_id) REFERENCES group_vote(group_vote_id),
    CONSTRAINT fk_gvb_option FOREIGN KEY (group_vote_option_id) REFERENCES group_vote_option(group_vote_option_id),
    CONSTRAINT fk_gvb_voter FOREIGN KEY (voter_matching_member_id) REFERENCES matching_member(matching_member_id),
    CONSTRAINT uq_gvb_vote_voter UNIQUE (group_vote_id, voter_matching_member_id)
);

CREATE INDEX ix_gvb_vote_option ON group_vote_ballot (group_vote_id, group_vote_option_id);



CREATE TABLE moment (
    moment_id UUID PRIMARY KEY,
    author_user_id UUID NOT NULL,
    matching_group_id UUID,
    author_matching_member_id UUID,
    caption TEXT,
    captured_at TIMESTAMP,
    place_name VARCHAR(200),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    visibility VARCHAR(20) NOT NULL DEFAULT 'GROUP_ONLY',
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    hidden_by_user_id UUID,
    hidden_reason TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_moment_author_user FOREIGN KEY (author_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_moment_matching_group FOREIGN KEY (matching_group_id) REFERENCES matching_group(matching_group_id) ON DELETE SET NULL,
    CONSTRAINT fk_moment_author_member FOREIGN KEY (author_matching_member_id) REFERENCES matching_member(matching_member_id) ON DELETE SET NULL,
    CONSTRAINT fk_moment_hidden_by FOREIGN KEY (hidden_by_user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT chk_moment_visibility CHECK (visibility IN ('GROUP_ONLY', 'PUBLIC_PROFILE', 'ONLY_ME')),
    CONSTRAINT chk_moment_status CHECK (status IN ('VISIBLE', 'HIDDEN'))
);

CREATE INDEX ix_moment_group ON moment (matching_group_id) WHERE is_deleted = FALSE;
CREATE INDEX ix_moment_author ON moment (author_user_id) WHERE is_deleted = FALSE;
CREATE INDEX ix_moment_created_at ON moment (created_at DESC) WHERE is_deleted = FALSE;

CREATE TABLE moment_media (
    moment_media_id UUID PRIMARY KEY,
    moment_id UUID NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_moment_media_moment FOREIGN KEY (moment_id) REFERENCES moment(moment_id) ON DELETE CASCADE,
    CONSTRAINT uq_moment_media_order UNIQUE (moment_id, sort_order)
);

CREATE INDEX ix_moment_media_moment ON moment_media (moment_id);



CREATE TABLE group_peer_review (
    group_peer_review_id UUID PRIMARY KEY,
    group_trip_id UUID NOT NULL,
    reviewer_matching_member_id UUID NOT NULL,
    reviewee_matching_member_id UUID NOT NULL,
    actual_endurance_rating SMALLINT NOT NULL,
    punctuality_responsibility_rating SMALLINT NOT NULL,
    financial_fairness_rating SMALLINT NOT NULL,
    comment TEXT,
    moderation_status VARCHAR(20) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_gpr_trip FOREIGN KEY (group_trip_id) REFERENCES group_trip(group_trip_id),
    CONSTRAINT fk_gpr_reviewer FOREIGN KEY (reviewer_matching_member_id) REFERENCES matching_member(matching_member_id),
    CONSTRAINT fk_gpr_reviewee FOREIGN KEY (reviewee_matching_member_id) REFERENCES matching_member(matching_member_id),
    CONSTRAINT uq_gpr_trip_reviewer_reviewee UNIQUE (group_trip_id, reviewer_matching_member_id, reviewee_matching_member_id),
    CONSTRAINT chk_gpr_parties CHECK (reviewer_matching_member_id <> reviewee_matching_member_id),
    CONSTRAINT chk_gpr_endurance CHECK (actual_endurance_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_gpr_punctuality CHECK (punctuality_responsibility_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_gpr_fairness CHECK (financial_fairness_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_gpr_moderation_status CHECK (moderation_status IN ('VISIBLE','HIDDEN','REPORTED'))
);

CREATE TABLE group_emergency_contact (
    group_emergency_contact_id UUID PRIMARY KEY,
    matching_group_id UUID NOT NULL,
    contact_type VARCHAR(30) NOT NULL,
    contact_name VARCHAR(200) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    note TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_gec_group FOREIGN KEY (matching_group_id) REFERENCES matching_group(matching_group_id),
    CONSTRAINT chk_gec_contact_type CHECK (contact_type IN ('NATIONWIDE', 'OTHER'))
);

CREATE INDEX ix_gec_group_active ON group_emergency_contact (matching_group_id, is_active);



CREATE TABLE notification (
    notification_id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    event_type VARCHAR(50) NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    action_url VARCHAR(500),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users(user_id),
    CONSTRAINT chk_notification_reference_type CHECK (
        reference_type IS NULL OR reference_type IN
        ('TOUR','BLOG','MATCHING_GROUP','CONVERSATION','GROUP_TRIP','GROUP_EXPENSE','GROUP_VOTE','SOS',
         'VENDOR_APPLICATION','VENDOR','REPORT','USER')
    )
);

CREATE INDEX ix_notification_recipient_unread ON notification (recipient_id, is_read, created_at);

CREATE TABLE blog (
    blog_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    cover_image_url VARCHAR(500),
    view_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_blog_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT chk_blog_status CHECK (status IN ('DRAFT','PUBLISHED','HIDDEN'))
);

CREATE TABLE blog_comment (
    blog_comment_id UUID PRIMARY KEY,
    blog_id UUID NOT NULL,
    user_id UUID NOT NULL,
    parent_comment_id UUID,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_bc_blog FOREIGN KEY (blog_id) REFERENCES blog(blog_id),
    CONSTRAINT fk_bc_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_bc_parent FOREIGN KEY (parent_comment_id) REFERENCES blog_comment(blog_comment_id),
    CONSTRAINT chk_bc_status CHECK (status IN ('VISIBLE','HIDDEN'))
);

CREATE TABLE report_content (
    report_content_id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL,
    blog_id UUID,
    blog_comment_id UUID,
    tour_id UUID,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    resolution_notes VARCHAR(500),
    resolved_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_rc_reporter FOREIGN KEY (reporter_id) REFERENCES users(user_id),
    CONSTRAINT fk_rc_blog FOREIGN KEY (blog_id) REFERENCES blog(blog_id),
    CONSTRAINT fk_rc_comment FOREIGN KEY (blog_comment_id) REFERENCES blog_comment(blog_comment_id),
    CONSTRAINT fk_rc_tour FOREIGN KEY (tour_id) REFERENCES tour(tour_id),
    CONSTRAINT fk_rc_resolved_by FOREIGN KEY (resolved_by) REFERENCES users(user_id),
    CONSTRAINT chk_rc_status CHECK (status IN ('PENDING','RESOLVED','REJECTED')),
    CONSTRAINT chk_rc_single_target CHECK (
        (CASE WHEN blog_id IS NOT NULL THEN 1 ELSE 0 END
       + CASE WHEN blog_comment_id IS NOT NULL THEN 1 ELSE 0 END
       + CASE WHEN tour_id IS NOT NULL THEN 1 ELSE 0 END) = 1
    )
);
