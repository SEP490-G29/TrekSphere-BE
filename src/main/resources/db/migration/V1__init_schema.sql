
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
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255)
);

CREATE TABLE user_role (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES role(role_id)
);


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
    bank_account VARCHAR(50),
    bank_name VARCHAR(100),
    payment_qr_url VARCHAR(500),
    status VARCHAR(10) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_vendor_manager FOREIGN KEY (manager_id) REFERENCES users(user_id)
);

CREATE TABLE vendor_application (
    vendor_application_id UUID PRIMARY KEY,
    applicant_id UUID NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    tax_code VARCHAR(50) UNIQUE NOT NULL,
    business_license_url VARCHAR(500) NOT NULL,
    business_description TEXT,
    application_status VARCHAR(10) NOT NULL,
    rejection_reason TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_va_applicant FOREIGN KEY (applicant_id) REFERENCES users(user_id)
);

CREATE TABLE vendor_staff (
    vendor_staff_id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    user_id UUID NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_vs_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(vendor_id),
    CONSTRAINT fk_vs_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE vendor_equipment (
    equipment_id UUID PRIMARY KEY,
    vendor_id UUID,
    equipment_name VARCHAR(255) NOT NULL,
    description TEXT,
    total_quantity INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_ve_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(vendor_id)
);

CREATE TABLE porter_profile (
    porter_id UUID PRIMARY KEY,
    vendor_id UUID,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    gender VARCHAR(10),
    date_of_birth DATE,
    address VARCHAR(255),
    avatar_url VARCHAR(500),
    joined_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_pp_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(vendor_id)
);

CREATE TABLE voucher (
    voucher_id UUID PRIMARY KEY,
    vendor_id UUID,
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    min_order_value DECIMAL(12,2) NOT NULL DEFAULT 0,
    max_usage INTEGER NOT NULL,
    used_count INTEGER NOT NULL DEFAULT 0,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    status VARCHAR(10) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_voucher_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(vendor_id)
);

CREATE TABLE tour (
    tour_id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    tour_name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    duration_days INTEGER NOT NULL,
    base_price DECIMAL(12,2) NOT NULL,
    min_capacity INTEGER NOT NULL DEFAULT 1,
    max_capacity INTEGER NOT NULL,
    total_distance_km DECIMAL(5,2),
    difficulty VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    cover_image_url VARCHAR(500),
    location VARCHAR(255) NOT NULL,
    highlights TEXT,
    includes TEXT,
    excludes TEXT,
    creator_id UUID NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_tour_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(vendor_id),
    CONSTRAINT fk_tour_creator FOREIGN KEY (creator_id) REFERENCES users(user_id)
);

CREATE TABLE tour_image (
    image_id UUID PRIMARY KEY,
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
    schedule_id UUID PRIMARY KEY,
    tour_id UUID NOT NULL,
    departure_date DATE NOT NULL,
    return_date DATE NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    booked_slots INTEGER NOT NULL DEFAULT 0,
    available_slots INTEGER NOT NULL,
    status VARCHAR(10) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_ts_tour FOREIGN KEY (tour_id) REFERENCES tour(tour_id)
);

CREATE TABLE tour_checkpoint (
    checkpoint_id UUID PRIMARY KEY,
    tour_id UUID,
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
    
    CONSTRAINT fk_tc_tour FOREIGN KEY (tour_id) REFERENCES tour(tour_id)
);

CREATE TABLE tour_session (
    tour_session_id UUID PRIMARY KEY,
    tour_schedule_id UUID UNIQUE,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_tse_schedule FOREIGN KEY (tour_schedule_id) REFERENCES tour_schedule(schedule_id)
);

CREATE TABLE session_checkpoint_log (
    session_checkpoint_log_id UUID PRIMARY KEY,
    tour_session_id UUID,
    checkpoint_id UUID,
    status VARCHAR(20) NOT NULL,
    reached_at TIMESTAMP,
    actual_latitude DECIMAL(10,7),
    actual_longitude DECIMAL(10,7),
    note TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_scl_session FOREIGN KEY (tour_session_id) REFERENCES tour_session(tour_session_id),
    CONSTRAINT fk_scl_checkpoint FOREIGN KEY (checkpoint_id) REFERENCES tour_checkpoint(checkpoint_id)
);

CREATE TABLE sos_alert (
    sos_alert_id UUID PRIMARY KEY,
    tour_session_id UUID,
    sender_id UUID,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    message TEXT,
    status VARCHAR(20) NOT NULL,
    resolved_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_sos_session FOREIGN KEY (tour_session_id) REFERENCES tour_session(tour_session_id),
    CONSTRAINT fk_sos_sender FOREIGN KEY (sender_id) REFERENCES users(user_id),
    CONSTRAINT fk_sos_resolved FOREIGN KEY (resolved_by) REFERENCES users(user_id)
);

CREATE TABLE session_equipment (
    session_equipment_id UUID PRIMARY KEY,
    tour_session_id UUID,
    equipment_id UUID NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    is_checked BOOLEAN NOT NULL DEFAULT FALSE,
    checked_by UUID,
    note TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_se_session FOREIGN KEY (tour_session_id) REFERENCES tour_session(tour_session_id),
    CONSTRAINT fk_se_equipment FOREIGN KEY (equipment_id) REFERENCES vendor_equipment(equipment_id),
    CONSTRAINT fk_se_checked FOREIGN KEY (checked_by) REFERENCES users(user_id)
);

CREATE TABLE porter_schedule (
    porter_schedule_id UUID PRIMARY KEY,
    tour_session_id UUID,
    porter_id UUID,
    note TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_ps_session FOREIGN KEY (tour_session_id) REFERENCES tour_session(tour_session_id),
    CONSTRAINT fk_ps_porter FOREIGN KEY (porter_id) REFERENCES porter_profile(porter_id)
);

CREATE TABLE coordinator_schedule (
    coordinator_schedule_id UUID PRIMARY KEY,
    tour_session_id UUID,
    coordinator_id UUID,
    is_lead BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_cs_session FOREIGN KEY (tour_session_id) REFERENCES tour_session(tour_session_id),
    CONSTRAINT fk_cs_coordinator FOREIGN KEY (coordinator_id) REFERENCES users(user_id)
);

CREATE TABLE booking (
    booking_id UUID PRIMARY KEY,
    booking_code VARCHAR(50) UNIQUE NOT NULL,
    user_id UUID NOT NULL,
    tour_schedule_id UUID NOT NULL,
    voucher_id UUID,
    number_of_participants INTEGER NOT NULL,
    original_price DECIMAL(12,2) NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    booking_status VARCHAR(10) NOT NULL,
    proof_image_url VARCHAR(500),
    cancellation_reason TEXT,
    refund_amount DECIMAL(12,2),
    cancelled_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_booking_schedule FOREIGN KEY (tour_schedule_id) REFERENCES tour_schedule(schedule_id),
    CONSTRAINT fk_booking_voucher FOREIGN KEY (voucher_id) REFERENCES voucher(voucher_id)
);

CREATE TABLE booking_participant (
    participant_id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    id_number VARCHAR(20) NOT NULL,
    address VARCHAR(255),
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    special_requirements TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_bp_booking FOREIGN KEY (booking_id) REFERENCES booking(booking_id)
);

CREATE TABLE cancellation_policy (
    cancellation_policy_id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    cancel_before_days INTEGER NOT NULL,
    refund_percentage INTEGER NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_cp_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(vendor_id)
);

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
    
    CONSTRAINT fk_blog_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE blog_comment (
    blog_comment_id UUID PRIMARY KEY,
    blog_id UUID NOT NULL,
    user_id UUID NOT NULL,
    parent_comment_id UUID,
    content TEXT NOT NULL,
    status VARCHAR(10) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_bc_blog FOREIGN KEY (blog_id) REFERENCES blog(blog_id),
    CONSTRAINT fk_bc_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_bc_parent FOREIGN KEY (parent_comment_id) REFERENCES blog_comment(blog_comment_id)
);

CREATE TABLE review (
    review_id UUID PRIMARY KEY,
    tour_id UUID,
    user_id UUID,
    booking_id UUID UNIQUE,
    rating INTEGER NOT NULL,
    content TEXT,
    status VARCHAR(10) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_review_tour FOREIGN KEY (tour_id) REFERENCES tour(tour_id),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_review_booking FOREIGN KEY (booking_id) REFERENCES booking(booking_id)
);

CREATE TABLE report_content (
    report_content_id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL,
    blog_id UUID,
    blog_comment_id UUID,
    review_id UUID,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(10) NOT NULL,
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
    CONSTRAINT fk_rc_review FOREIGN KEY (review_id) REFERENCES review(review_id)
);

CREATE TABLE matching_group (
    matching_group_id UUID PRIMARY KEY,
    tour_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    group_name VARCHAR(255) NOT NULL,
    description TEXT,
    max_size INTEGER NOT NULL,
    current_size INTEGER NOT NULL DEFAULT 1,
    target_date DATE NOT NULL,
    matching_deadline TIMESTAMP NOT NULL,
    status VARCHAR(10) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_mg_tour FOREIGN KEY (tour_id) REFERENCES tour(tour_id),
    CONSTRAINT fk_mg_owner FOREIGN KEY (owner_id) REFERENCES users(user_id)
);

CREATE TABLE matching_member (
    matching_member_id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(10) NOT NULL,
    status VARCHAR(10) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_mm_group FOREIGN KEY (group_id) REFERENCES matching_group(matching_group_id),
    CONSTRAINT fk_mm_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

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
    deleted_by VARCHAR(255)
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
    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(conversation_id),
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users(user_id)
);

CREATE TABLE notification (
    notification_id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    event_type VARCHAR(50) NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users(user_id)
);

