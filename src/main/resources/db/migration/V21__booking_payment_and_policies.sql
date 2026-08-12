-- V20__booking_payment_and_policies.sql
--
-- Expand phase for the booking/payment migration:
--   * booking hold and group capacity metadata
--   * vendor-configurable full-payment/deposit policy
--   * Vendor-direct PSP checkout, webhook inbox and refunds
--   * immutable booking status audit and booking-time policy snapshots
--   * one-to-one participation policy for every tour
--
-- Real funds never enter an account controlled by TrekSphere. Each Vendor owns
-- the PSP payment channel and its linked bank account; TrekSphere only creates
-- checkout requests and consumes signed webhooks. Consequently this schema has
-- no split, commission, wallet, withdrawal, settlement, or Vendor payout.
--
-- The legacy P2P columns on booking are intentionally kept in V20. The current
-- application still maps them while Hibernate ddl-auto is set to validate. They
-- can be removed in a later contract migration after all reads/writes have moved
-- to the new transaction tables.

-- ---------------------------------------------------------------------------
-- 1. Tour policies
-- ---------------------------------------------------------------------------

ALTER TABLE tour
    ADD COLUMN non_refundable_cost DECIMAL(12,2) NOT NULL DEFAULT 0;

ALTER TABLE tour
    ADD CONSTRAINT chk_tour_non_refundable_cost
        CHECK (non_refundable_cost >= 0);

CREATE TABLE tour_participation_policy (
    tour_id UUID PRIMARY KEY,
    policy_version INTEGER NOT NULL DEFAULT 1,
    min_age SMALLINT,
    max_age SMALLINT,
    fitness_level VARCHAR(20) NOT NULL DEFAULT 'ANY',
    health_requirements TEXT,
    restricted_medical_conditions TEXT,
    required_experience TEXT,
    required_skills TEXT,
    required_equipment TEXT,
    required_documents TEXT,
    requires_health_declaration BOOLEAN NOT NULL DEFAULT TRUE,
    requires_medical_certificate BOOLEAN NOT NULL DEFAULT FALSE,
    guardian_required_under_age SMALLINT,
    additional_rules JSONB NOT NULL DEFAULT '{}'::JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_tpp_tour
        FOREIGN KEY (tour_id) REFERENCES tour(tour_id),
    CONSTRAINT chk_tpp_policy_version
        CHECK (policy_version > 0),
    CONSTRAINT chk_tpp_age_range
        CHECK (
            (min_age IS NULL OR min_age BETWEEN 0 AND 120)
            AND (max_age IS NULL OR max_age BETWEEN 0 AND 120)
            AND (min_age IS NULL OR max_age IS NULL OR min_age <= max_age)
        ),
    CONSTRAINT chk_tpp_fitness_level
        CHECK (fitness_level IN ('ANY', 'BASIC', 'MODERATE', 'HIGH', 'EXTREME')),
    CONSTRAINT chk_tpp_guardian_age
        CHECK (guardian_required_under_age IS NULL OR guardian_required_under_age BETWEEN 1 AND 18),
    CONSTRAINT chk_tpp_additional_rules_object
        CHECK (jsonb_typeof(additional_rules) = 'object')
);

COMMENT ON TABLE tour_participation_policy IS
    'Current 1:1 tour participation rules. A copy is frozen in booking_policy_snapshot when a booking is created.';
COMMENT ON COLUMN tour_participation_policy.restricted_medical_conditions IS
    'Participation restrictions only; actual participant health data must not be stored here.';

-- The Vendor configures this policy per tour. FULL_OR_DEPOSIT lets the Trekker
-- choose; DEPOSIT_ONLY requires two payment stages; FULL_PAYMENT_ONLY has one.
CREATE TABLE tour_payment_policy (
    tour_id UUID PRIMARY KEY,
    payment_option VARCHAR(30) NOT NULL DEFAULT 'FULL_PAYMENT_ONLY',
    deposit_type VARCHAR(20),
    deposit_value DECIMAL(12,2),
    remaining_due_days_before_departure INTEGER,
    policy_version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_tpayp_tour
        FOREIGN KEY (tour_id) REFERENCES tour(tour_id),
    CONSTRAINT chk_tpayp_payment_option
        CHECK (payment_option IN ('FULL_PAYMENT_ONLY', 'DEPOSIT_ONLY', 'FULL_OR_DEPOSIT')),
    CONSTRAINT chk_tpayp_deposit_type
        CHECK (deposit_type IS NULL OR deposit_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT chk_tpayp_policy_version
        CHECK (policy_version > 0),
    CONSTRAINT chk_tpayp_remaining_due_days
        CHECK (remaining_due_days_before_departure IS NULL OR remaining_due_days_before_departure >= 0),
    CONSTRAINT chk_tpayp_deposit_configuration
        CHECK (
            (
                payment_option = 'FULL_PAYMENT_ONLY'
                AND deposit_type IS NULL
                AND deposit_value IS NULL
                AND remaining_due_days_before_departure IS NULL
            )
            OR
            (
                payment_option IN ('DEPOSIT_ONLY', 'FULL_OR_DEPOSIT')
                AND deposit_type IS NOT NULL
                AND deposit_value IS NOT NULL
                AND remaining_due_days_before_departure IS NOT NULL
                AND (
                    (deposit_type = 'PERCENTAGE' AND deposit_value > 0 AND deposit_value < 100)
                    OR (deposit_type = 'FIXED_AMOUNT' AND deposit_value > 0)
                )
            )
        )
);

COMMENT ON TABLE tour_payment_policy IS
    'Vendor-configured 1:1 tour policy deciding full payment, deposit, or Trekker choice. Snapshotted when booking is created.';

-- Existing tours keep the current behavior: one full payment.
INSERT INTO tour_payment_policy (tour_id, payment_option)
SELECT tour_id, 'FULL_PAYMENT_ONLY'
FROM tour;

-- ---------------------------------------------------------------------------
-- 2. Hold capacity, deadlines, and booking payment plan
-- ---------------------------------------------------------------------------

ALTER TABLE tour_schedule
    ADD COLUMN min_pax_required INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN confirmation_deadline TIMESTAMP,
    ADD COLUMN payment_deadline TIMESTAMP,
    ADD COLUMN held_slots INTEGER NOT NULL DEFAULT 0;

-- Existing schedules are group schedules, so use the tour's configured minimum.
UPDATE tour_schedule ts
SET min_pax_required = GREATEST(1, t.min_capacity)
FROM tour t
WHERE t.tour_id = ts.tour_id;

ALTER TABLE tour_schedule
    ADD CONSTRAINT chk_ts_min_pax_required
        CHECK (min_pax_required > 0),
    ADD CONSTRAINT chk_ts_held_slots
        CHECK (held_slots >= 0),
    ADD CONSTRAINT chk_ts_slot_counts
        CHECK (booked_slots >= 0 AND available_slots >= 0),
    ADD CONSTRAINT chk_ts_deadline_order
        CHECK (
            confirmation_deadline IS NULL
            OR payment_deadline IS NULL
            OR confirmation_deadline <= payment_deadline
        );

ALTER TABLE booking
    ADD COLUMN payment_plan VARCHAR(20) NOT NULL DEFAULT 'FULL_PAYMENT',
    ADD COLUMN hold_expires_at TIMESTAMP,
    ALTER COLUMN booking_status TYPE VARCHAR(40);

ALTER TABLE booking
    ALTER COLUMN hold_expires_at SET DEFAULT (CURRENT_TIMESTAMP + INTERVAL '15 minutes'),
    ADD CONSTRAINT chk_booking_payment_plan
        CHECK (payment_plan IN ('FULL_PAYMENT', 'DEPOSIT'));

CREATE INDEX idx_booking_expiring_hold
    ON booking (hold_expires_at)
    WHERE hold_expires_at IS NOT NULL
      AND is_deleted = FALSE;

COMMENT ON COLUMN booking.payment_status IS
    'Legacy read model only. New payment state is sourced from payment_transaction.';
COMMENT ON COLUMN booking.refund_amount IS
    'Deprecated in V20. New refunds are sourced from refund_transaction; remove this column in the contract migration.';
COMMENT ON COLUMN booking.proof_image_url IS
    'Legacy P2P bank-transfer proof; not used by gateway payments.';

-- ---------------------------------------------------------------------------
-- 3. Vendor-direct PSP payment attempts, webhook inbox, and refunds
-- ---------------------------------------------------------------------------

-- payOS requires a numeric orderCode. Generate it in PostgreSQL instead of
-- using epoch seconds, which can collide under concurrent requests.
CREATE SEQUENCE gateway_order_code_seq
    AS BIGINT
    START WITH 1000000000
    INCREMENT BY 1
    NO CYCLE;

-- Each Vendor owns a provider channel whose destination is the Vendor's bank
-- account. API/checksum keys are encrypted by the application before they are
-- persisted; plaintext credentials must never be stored or returned by an API.
CREATE TABLE vendor_payment_account (
    vendor_payment_account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id UUID NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_channel_id VARCHAR(255) NOT NULL,
    api_key_encrypted TEXT NOT NULL,
    checksum_key_encrypted TEXT NOT NULL,
    onboarding_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_default BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_vpa_vendor
        FOREIGN KEY (vendor_id) REFERENCES vendor(vendor_id),
    CONSTRAINT uq_vpa_provider_channel
        UNIQUE (provider, provider_channel_id),
    CONSTRAINT chk_vpa_onboarding_status
        CHECK (onboarding_status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'REJECTED'))
);

CREATE UNIQUE INDEX ux_vpa_default_provider
    ON vendor_payment_account (vendor_id, provider)
    WHERE is_default = TRUE AND is_deleted = FALSE;

CREATE INDEX idx_vpa_vendor_active
    ON vendor_payment_account (vendor_id, onboarding_status)
    WHERE is_deleted = FALSE;

CREATE TABLE payment_transaction (
    payment_transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    vendor_payment_account_id UUID NOT NULL,
    payment_stage VARCHAR(20) NOT NULL,
    attempt_number SMALLINT NOT NULL DEFAULT 1,
    provider VARCHAR(30) NOT NULL,
    gateway_order_code BIGINT DEFAULT nextval('gateway_order_code_seq'),
    gateway_payment_link_id VARCHAR(255),
    gateway_reference VARCHAR(255),
    checkout_url VARCHAR(1000),
    qr_code TEXT,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    gateway_fee_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    expired_at TIMESTAMP,
    paid_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    failure_code VARCHAR(100),
    failure_message VARCHAR(500),
    gateway_metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_pt_booking
        FOREIGN KEY (booking_id) REFERENCES booking(booking_id),
    CONSTRAINT fk_pt_vendor_payment_account
        FOREIGN KEY (vendor_payment_account_id)
        REFERENCES vendor_payment_account(vendor_payment_account_id),
    CONSTRAINT uq_pt_id_booking
        UNIQUE (payment_transaction_id, booking_id),
    CONSTRAINT uq_pt_stage_attempt
        UNIQUE (booking_id, payment_stage, attempt_number),
    CONSTRAINT chk_pt_stage
        CHECK (payment_stage IN ('FULL', 'DEPOSIT', 'REMAINING')),
    CONSTRAINT chk_pt_attempt_number
        CHECK (attempt_number > 0),
    CONSTRAINT chk_pt_amount
        CHECK (amount > 0),
    CONSTRAINT chk_pt_gateway_fee
        CHECK (gateway_fee_amount >= 0),
    CONSTRAINT chk_pt_gateway_order
        CHECK (
            provider <> 'PAYOS'
            OR (
                gateway_order_code IS NOT NULL
                AND currency = 'VND'
                AND amount = TRUNC(amount)
            )
        ),
    CONSTRAINT chk_pt_paid_amount
        CHECK (
            paid_amount >= 0
            AND paid_amount <= amount
            AND (status <> 'PAID' OR paid_amount = amount)
        ),
    CONSTRAINT chk_pt_status
        CHECK (status IN ('CREATED', 'PENDING', 'PROCESSING', 'PAID', 'FAILED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_pt_currency
        CHECK (currency = UPPER(currency)),
    CONSTRAINT chk_pt_gateway_metadata_object
        CHECK (jsonb_typeof(gateway_metadata) = 'object')
);

CREATE UNIQUE INDEX ux_pt_provider_order_code
    ON payment_transaction (provider, gateway_order_code)
    WHERE gateway_order_code IS NOT NULL
      AND is_deleted = FALSE;

CREATE UNIQUE INDEX ux_pt_provider_payment_link
    ON payment_transaction (provider, gateway_payment_link_id)
    WHERE gateway_payment_link_id IS NOT NULL
      AND is_deleted = FALSE;

-- Prevent concurrent duplicate links for the same stage while allowing retry
-- after the old link becomes FAILED, CANCELLED, or EXPIRED.
CREATE UNIQUE INDEX ux_pt_one_live_stage
    ON payment_transaction (booking_id, payment_stage)
    WHERE status IN ('CREATED', 'PENDING', 'PROCESSING', 'PAID')
      AND is_deleted = FALSE;

CREATE INDEX idx_pt_booking_created
    ON payment_transaction (booking_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_pt_payment_link_expiry
    ON payment_transaction (expired_at)
    WHERE status IN ('CREATED', 'PENDING', 'PROCESSING')
      AND is_deleted = FALSE;

-- PSPs retry webhooks until receiving a 2xx response. gateway_event_key is the
-- provider event id, or a deterministic composition of payment id + reference
-- when the provider has no standalone event id.
CREATE TABLE payment_webhook_event (
    payment_webhook_event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_transaction_id UUID,
    provider VARCHAR(30) NOT NULL,
    gateway_event_key VARCHAR(600) NOT NULL,
    gateway_order_code BIGINT,
    gateway_payment_link_id VARCHAR(255),
    gateway_reference VARCHAR(255),
    signature VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    error_message VARCHAR(500),
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,

    CONSTRAINT fk_pwe_payment
        FOREIGN KEY (payment_transaction_id) REFERENCES payment_transaction(payment_transaction_id),
    CONSTRAINT uq_pwe_provider_event
        UNIQUE (provider, gateway_event_key),
    CONSTRAINT chk_pwe_processing_status
        CHECK (processing_status IN ('RECEIVED', 'PROCESSED', 'IGNORED', 'FAILED')),
    CONSTRAINT chk_pwe_payload_object
        CHECK (jsonb_typeof(payload) = 'object')
);

CREATE INDEX idx_pwe_payment_received
    ON payment_webhook_event (payment_transaction_id, received_at DESC)
    WHERE payment_transaction_id IS NOT NULL;

CREATE INDEX idx_pwe_unprocessed
    ON payment_webhook_event (received_at)
    WHERE processing_status IN ('RECEIVED', 'FAILED');

CREATE TABLE refund_transaction (
    refund_transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_transaction_id UUID NOT NULL,
    booking_id UUID NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    reason_detail VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by UUID,
    refund_method VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    destination_bin VARCHAR(20),
    destination_account_number VARCHAR(50),
    destination_account_name VARCHAR(255),
    gateway_refund_id VARCHAR(255),
    gateway_metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_at TIMESTAMP,
    completed_at TIMESTAMP,
    failure_code VARCHAR(100),
    failure_message VARCHAR(500),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_rt_payment_booking
        FOREIGN KEY (payment_transaction_id, booking_id)
        REFERENCES payment_transaction(payment_transaction_id, booking_id),
    CONSTRAINT fk_rt_booking
        FOREIGN KEY (booking_id) REFERENCES booking(booking_id),
    CONSTRAINT fk_rt_approver
        FOREIGN KEY (approved_by) REFERENCES users(user_id),
    CONSTRAINT chk_rt_amount
        CHECK (amount > 0),
    CONSTRAINT chk_rt_reason
        CHECK (reason IN (
            'TREKKER_CANCEL',
            'VENDOR_CANCEL',
            'INSUFFICIENT_PAX',
            'NO_SHOW',
            'PAYMENT_ADJUSTMENT',
            'OTHER'
        )),
    CONSTRAINT chk_rt_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'REFUNDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_rt_refund_method
        CHECK (refund_method IN ('GATEWAY_REFUND', 'MANUAL')),
    CONSTRAINT chk_rt_manual_destination
        CHECK (
            refund_method <> 'MANUAL'
            OR (
                destination_bin IS NOT NULL
                AND destination_account_number IS NOT NULL
                AND destination_account_name IS NOT NULL
            )
        ),
    CONSTRAINT chk_rt_gateway_metadata_object
        CHECK (jsonb_typeof(gateway_metadata) = 'object')
);

CREATE UNIQUE INDEX ux_rt_gateway_refund
    ON refund_transaction (gateway_refund_id)
    WHERE gateway_refund_id IS NOT NULL
      AND is_deleted = FALSE;

CREATE INDEX idx_rt_payment_status
    ON refund_transaction (payment_transaction_id, status)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_rt_booking_created
    ON refund_transaction (booking_id, created_at DESC)
    WHERE is_deleted = FALSE;

-- Serialize refund requests on the paid link and prevent active/completed
-- refunds from exceeding the amount confirmed by the PSP.
CREATE FUNCTION validate_refund_against_paid_amount()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_paid_amount DECIMAL(12,2);
    v_payment_status VARCHAR(20);
    v_existing_refunds DECIMAL(12,2);
BEGIN
    IF NEW.is_deleted = TRUE
       OR NEW.status NOT IN ('PENDING', 'PROCESSING', 'REFUNDED') THEN
        RETURN NEW;
    END IF;

    SELECT paid_amount, status
    INTO v_paid_amount, v_payment_status
    FROM payment_transaction
    WHERE payment_transaction_id = NEW.payment_transaction_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Payment transaction % does not exist', NEW.payment_transaction_id;
    END IF;

    IF v_payment_status <> 'PAID' THEN
        RAISE EXCEPTION 'Payment transaction % is not paid', NEW.payment_transaction_id;
    END IF;

    SELECT COALESCE(SUM(amount), 0)
    INTO v_existing_refunds
    FROM refund_transaction
    WHERE payment_transaction_id = NEW.payment_transaction_id
      AND refund_transaction_id <> NEW.refund_transaction_id
      AND status IN ('PENDING', 'PROCESSING', 'REFUNDED')
      AND is_deleted = FALSE;

    IF v_existing_refunds + NEW.amount > v_paid_amount THEN
        RAISE EXCEPTION
            'Refund total % exceeds paid amount % for payment transaction %',
            v_existing_refunds + NEW.amount,
            v_paid_amount,
            NEW.payment_transaction_id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_refund_against_paid_amount
BEFORE INSERT OR UPDATE OF payment_transaction_id, amount, status, is_deleted
ON refund_transaction
FOR EACH ROW
EXECUTE FUNCTION validate_refund_against_paid_amount();

-- ---------------------------------------------------------------------------
-- 4. Booking status audit and immutable policy snapshot
-- ---------------------------------------------------------------------------

CREATE TABLE booking_status_log (
    booking_status_log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    old_status VARCHAR(40),
    new_status VARCHAR(40) NOT NULL,
    changed_by UUID,
    change_source VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    reason VARCHAR(255),
    correlation_id VARCHAR(255),
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bsl_booking
        FOREIGN KEY (booking_id) REFERENCES booking(booking_id),
    CONSTRAINT fk_bsl_user
        FOREIGN KEY (changed_by) REFERENCES users(user_id),
    CONSTRAINT chk_bsl_source
        CHECK (change_source IN ('TREKKER', 'VENDOR', 'STAFF', 'SCHEDULER', 'WEBHOOK', 'SYSTEM', 'MIGRATION')),
    CONSTRAINT chk_bsl_metadata_object
        CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE INDEX idx_bsl_booking_created
    ON booking_status_log (booking_id, created_at DESC);

-- The service can enrich trigger-created audit rows inside the same transaction:
--   SET LOCAL treksphere.changed_by = '<user UUID>';
--   SET LOCAL treksphere.change_source = 'VENDOR';
--   SET LOCAL treksphere.change_reason = 'Vendor accepted booking';
--   SET LOCAL treksphere.correlation_id = '<request/event id>';
CREATE FUNCTION audit_booking_status_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_changed_by UUID;
    v_change_source VARCHAR(20);
BEGIN
    IF OLD.booking_status IS NOT DISTINCT FROM NEW.booking_status THEN
        RETURN NEW;
    END IF;

    v_changed_by := NULLIF(current_setting('treksphere.changed_by', TRUE), '')::UUID;
    v_change_source := COALESCE(
        NULLIF(current_setting('treksphere.change_source', TRUE), ''),
        'SYSTEM'
    );

    INSERT INTO booking_status_log (
        booking_id,
        old_status,
        new_status,
        changed_by,
        change_source,
        reason,
        correlation_id
    )
    VALUES (
        NEW.booking_id,
        OLD.booking_status,
        NEW.booking_status,
        v_changed_by,
        v_change_source,
        LEFT(NULLIF(current_setting('treksphere.change_reason', TRUE), ''), 255),
        LEFT(NULLIF(current_setting('treksphere.correlation_id', TRUE), ''), 255)
    );

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_audit_booking_status_change
AFTER UPDATE OF booking_status
ON booking
FOR EACH ROW
EXECUTE FUNCTION audit_booking_status_change();

INSERT INTO booking_status_log (
    booking_id,
    old_status,
    new_status,
    change_source,
    reason,
    created_at
)
SELECT
    booking_id,
    NULL,
    booking_status,
    'MIGRATION',
    'Initial state captured by V20',
    COALESCE(created_at, CURRENT_TIMESTAMP)
FROM booking;

-- Audit rows are append-only. A correction must be represented by a new row.
CREATE FUNCTION reject_booking_status_log_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Booking status logs are append-only';
END;
$$;

CREATE TRIGGER trg_booking_status_log_immutable
BEFORE UPDATE OR DELETE
ON booking_status_log
FOR EACH ROW
EXECUTE FUNCTION reject_booking_status_log_mutation();

CREATE TABLE booking_policy_snapshot (
    booking_id UUID PRIMARY KEY,
    policy_json JSONB NOT NULL,
    participation_policy_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    payment_policy_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    non_refundable_cost DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bps_booking
        FOREIGN KEY (booking_id) REFERENCES booking(booking_id),
    CONSTRAINT chk_bps_cancellation_policy_array
        CHECK (jsonb_typeof(policy_json) = 'array'),
    CONSTRAINT chk_bps_participation_policy_object
        CHECK (jsonb_typeof(participation_policy_json) = 'object'),
    CONSTRAINT chk_bps_payment_policy_object
        CHECK (jsonb_typeof(payment_policy_json) = 'object'),
    CONSTRAINT chk_bps_non_refundable_cost
        CHECK (non_refundable_cost >= 0)
);

COMMENT ON COLUMN booking_policy_snapshot.policy_json IS
    'Immutable snapshot of all active vendor cancellation-policy tiers at booking time.';
COMMENT ON COLUMN booking_policy_snapshot.participation_policy_json IS
    'Immutable snapshot of the tour participation policy at booking time.';
COMMENT ON COLUMN booking_policy_snapshot.payment_policy_json IS
    'Immutable snapshot of the Vendor-configured full-payment/deposit policy at booking time.';

CREATE FUNCTION snapshot_booking_policies()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO booking_policy_snapshot (
        booking_id,
        policy_json,
        participation_policy_json,
        payment_policy_json,
        non_refundable_cost,
        created_at
    )
    SELECT
        NEW.booking_id,
        COALESCE(
            (
                SELECT jsonb_agg(
                    jsonb_build_object(
                        'cancellationPolicyId', cp.cancellation_policy_id,
                        'cancelBeforeDays', cp.cancel_before_days,
                        'refundPercentage', cp.refund_percentage,
                        'description', cp.description
                    )
                    ORDER BY cp.cancel_before_days DESC
                )
                FROM cancellation_policy cp
                WHERE cp.vendor_id = t.vendor_id
                  AND cp.is_active = TRUE
                  AND cp.is_deleted = FALSE
            ),
            '[]'::JSONB
        ),
        COALESCE(
            (
                SELECT jsonb_build_object(
                    'policyVersion', tpp.policy_version,
                    'minAge', tpp.min_age,
                    'maxAge', tpp.max_age,
                    'fitnessLevel', tpp.fitness_level,
                    'healthRequirements', tpp.health_requirements,
                    'restrictedMedicalConditions', tpp.restricted_medical_conditions,
                    'requiredExperience', tpp.required_experience,
                    'requiredSkills', tpp.required_skills,
                    'requiredEquipment', tpp.required_equipment,
                    'requiredDocuments', tpp.required_documents,
                    'requiresHealthDeclaration', tpp.requires_health_declaration,
                    'requiresMedicalCertificate', tpp.requires_medical_certificate,
                    'guardianRequiredUnderAge', tpp.guardian_required_under_age,
                    'additionalRules', tpp.additional_rules
                )
                FROM tour_participation_policy tpp
                WHERE tpp.tour_id = t.tour_id
                  AND tpp.is_active = TRUE
                  AND tpp.is_deleted = FALSE
            ),
            '{}'::JSONB
        ),
        COALESCE(
            (
                SELECT jsonb_build_object(
                    'policyVersion', tpayp.policy_version,
                    'paymentOption', tpayp.payment_option,
                    'depositType', tpayp.deposit_type,
                    'depositValue', tpayp.deposit_value,
                    'remainingDueDaysBeforeDeparture', tpayp.remaining_due_days_before_departure
                )
                FROM tour_payment_policy tpayp
                WHERE tpayp.tour_id = t.tour_id
                  AND tpayp.is_active = TRUE
                  AND tpayp.is_deleted = FALSE
            ),
            '{}'::JSONB
        ),
        t.non_refundable_cost,
        COALESCE(NEW.created_at, CURRENT_TIMESTAMP)
    FROM tour_schedule ts
    JOIN tour t ON t.tour_id = ts.tour_id
    WHERE ts.schedule_id = NEW.tour_schedule_id
    ON CONFLICT (booking_id) DO NOTHING;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_snapshot_booking_policies
AFTER INSERT
ON booking
FOR EACH ROW
EXECUTE FUNCTION snapshot_booking_policies();

-- Backfill snapshots for bookings that existed before the trigger.
INSERT INTO booking_policy_snapshot (
    booking_id,
    policy_json,
    participation_policy_json,
    payment_policy_json,
    non_refundable_cost,
    created_at
)
SELECT
    b.booking_id,
    COALESCE(
        (
            SELECT jsonb_agg(
                jsonb_build_object(
                    'cancellationPolicyId', cp.cancellation_policy_id,
                    'cancelBeforeDays', cp.cancel_before_days,
                    'refundPercentage', cp.refund_percentage,
                    'description', cp.description
                )
                ORDER BY cp.cancel_before_days DESC
            )
            FROM cancellation_policy cp
            WHERE cp.vendor_id = t.vendor_id
              AND cp.is_active = TRUE
              AND cp.is_deleted = FALSE
        ),
        '[]'::JSONB
    ),
    COALESCE(
        (
            SELECT jsonb_build_object(
                'policyVersion', tpp.policy_version,
                'minAge', tpp.min_age,
                'maxAge', tpp.max_age,
                'fitnessLevel', tpp.fitness_level,
                'healthRequirements', tpp.health_requirements,
                'restrictedMedicalConditions', tpp.restricted_medical_conditions,
                'requiredExperience', tpp.required_experience,
                'requiredSkills', tpp.required_skills,
                'requiredEquipment', tpp.required_equipment,
                'requiredDocuments', tpp.required_documents,
                'requiresHealthDeclaration', tpp.requires_health_declaration,
                'requiresMedicalCertificate', tpp.requires_medical_certificate,
                'guardianRequiredUnderAge', tpp.guardian_required_under_age,
                'additionalRules', tpp.additional_rules
            )
            FROM tour_participation_policy tpp
            WHERE tpp.tour_id = t.tour_id
              AND tpp.is_active = TRUE
              AND tpp.is_deleted = FALSE
        ),
        '{}'::JSONB
    ),
    COALESCE(
        (
            SELECT jsonb_build_object(
                'policyVersion', tpayp.policy_version,
                'paymentOption', tpayp.payment_option,
                'depositType', tpayp.deposit_type,
                'depositValue', tpayp.deposit_value,
                'remainingDueDaysBeforeDeparture', tpayp.remaining_due_days_before_departure
            )
            FROM tour_payment_policy tpayp
            WHERE tpayp.tour_id = t.tour_id
              AND tpayp.is_active = TRUE
              AND tpayp.is_deleted = FALSE
        ),
        '{}'::JSONB
    ),
    t.non_refundable_cost,
    COALESCE(b.created_at, CURRENT_TIMESTAMP)
FROM booking b
JOIN tour_schedule ts ON ts.schedule_id = b.tour_schedule_id
JOIN tour t ON t.tour_id = ts.tour_id
ON CONFLICT (booking_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 5. Activate the booking/payment workflow
-- ---------------------------------------------------------------------------

ALTER TABLE booking
    ALTER COLUMN payment_status TYPE VARCHAR(30),
    ADD COLUMN confirmation_expires_at TIMESTAMP,
    ADD COLUMN remaining_due_at TIMESTAMP,
    ADD COLUMN booking_request_key VARCHAR(255),
    ADD COLUMN booking_request_hash VARCHAR(64),
    ADD COLUMN voucher_state VARCHAR(20) NOT NULL DEFAULT 'NONE';

UPDATE booking
SET booking_status = CASE
        WHEN booking_status = 'PENDING' AND payment_status = 'PAID' THEN 'PENDING_CONFIRMATION'
        WHEN booking_status = 'PENDING' THEN 'PAYMENT_PENDING'
        ELSE booking_status
    END,
    payment_status = CASE
        WHEN payment_status = 'PENDING' THEN 'UNPAID'
        ELSE payment_status
    END,
    voucher_state = CASE
        WHEN voucher_id IS NULL THEN 'NONE'
        WHEN booking_status = 'CANCELLED' THEN 'RELEASED'
        ELSE 'CONSUMED'
    END;

ALTER TABLE booking
    ADD CONSTRAINT chk_booking_status_workflow
        CHECK (booking_status IN (
            'PAYMENT_PENDING', 'PENDING_CONFIRMATION', 'CONFIRMED', 'IN_PROGRESS',
            'COMPLETED', 'EXPIRED', 'REJECTED', 'CANCELLED'
        )),
    ADD CONSTRAINT chk_booking_payment_status_read_model
        CHECK (payment_status IN (
            'UNPAID', 'PARTIALLY_PAID', 'PAID', 'REFUND_PENDING',
            'PARTIALLY_REFUNDED', 'REFUNDED'
        )),
    ADD CONSTRAINT chk_booking_voucher_state
        CHECK (voucher_state IN ('NONE', 'RESERVED', 'CONSUMED', 'RELEASED'));

ALTER TABLE voucher
    ADD COLUMN reserved_count INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_voucher_reserved_count CHECK (reserved_count >= 0),
    ADD CONSTRAINT chk_voucher_usage_capacity CHECK (used_count + reserved_count <= max_usage);

CREATE INDEX idx_booking_confirmation_expiry
    ON booking (confirmation_expires_at)
    WHERE booking_status = 'PENDING_CONFIRMATION'
      AND confirmation_expires_at IS NOT NULL
      AND is_deleted = FALSE;

CREATE INDEX idx_booking_remaining_due
    ON booking (remaining_due_at)
    WHERE payment_plan = 'DEPOSIT'
      AND payment_status = 'PARTIALLY_PAID'
      AND remaining_due_at IS NOT NULL
      AND is_deleted = FALSE;

CREATE UNIQUE INDEX ux_booking_user_request_key
    ON booking (user_id, booking_request_key)
    WHERE booking_request_key IS NOT NULL
      AND is_deleted = FALSE;

COMMENT ON COLUMN booking.voucher_state IS
    'Reservation lifecycle for voucher capacity: NONE, RESERVED, CONSUMED, or RELEASED.';
COMMENT ON COLUMN booking.confirmation_expires_at IS
    'Vendor response SLA after a successful initial payment.';
COMMENT ON COLUMN booking.remaining_due_at IS
    'Deadline for the remaining balance when payment_plan is DEPOSIT.';
COMMENT ON COLUMN booking.booking_request_key IS
    'Client-supplied idempotency key scoped to the Trekker.';
COMMENT ON COLUMN booking.booking_request_hash IS
    'SHA-256 of the original booking request; prevents reusing an idempotency key with a different payload.';

CREATE FUNCTION ensure_default_tour_payment_policy()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO tour_payment_policy (tour_id, payment_option)
    VALUES (NEW.tour_id, 'FULL_PAYMENT_ONLY')
    ON CONFLICT (tour_id) DO NOTHING;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_default_tour_payment_policy
AFTER INSERT ON tour
FOR EACH ROW
EXECUTE FUNCTION ensure_default_tour_payment_policy();

-- ---------------------------------------------------------------------------
-- 6. Structured participation limits and legacy cleanup
-- ---------------------------------------------------------------------------

ALTER TABLE tour_participation_policy
    ADD COLUMN min_height_cm DECIMAL(5,2),
    ADD COLUMN max_height_cm DECIMAL(5,2),
    ADD COLUMN min_weight_kg DECIMAL(5,2),
    ADD COLUMN max_weight_kg DECIMAL(5,2),
    ADD CONSTRAINT chk_tpp_height_range CHECK (
        (min_height_cm IS NULL OR min_height_cm > 0)
        AND (max_height_cm IS NULL OR max_height_cm > 0)
        AND (min_height_cm IS NULL OR max_height_cm IS NULL OR min_height_cm <= max_height_cm)
    ),
    ADD CONSTRAINT chk_tpp_weight_range CHECK (
        (min_weight_kg IS NULL OR min_weight_kg > 0)
        AND (max_weight_kg IS NULL OR max_weight_kg > 0)
        AND (min_weight_kg IS NULL OR max_weight_kg IS NULL OR min_weight_kg <= max_weight_kg)
    );

ALTER TABLE booking
    ADD COLUMN participation_policy_accepted_at TIMESTAMP;

COMMENT ON COLUMN booking.participation_policy_accepted_at IS
    'Timestamp when the Trekker accepted the tour participation-policy snapshot.';

CREATE FUNCTION enrich_booking_participation_snapshot()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE booking_policy_snapshot bps
    SET participation_policy_json = bps.participation_policy_json || jsonb_strip_nulls(
        jsonb_build_object(
            'minHeightCm', tpp.min_height_cm,
            'maxHeightCm', tpp.max_height_cm,
            'minWeightKg', tpp.min_weight_kg,
            'maxWeightKg', tpp.max_weight_kg
        )
    )
    FROM tour_schedule ts
    JOIN tour_participation_policy tpp ON tpp.tour_id = ts.tour_id
    WHERE bps.booking_id = NEW.booking_id
      AND ts.schedule_id = NEW.tour_schedule_id
      AND tpp.is_active = TRUE
      AND tpp.is_deleted = FALSE;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_z_enrich_booking_participation_snapshot
AFTER INSERT ON booking
FOR EACH ROW
EXECUTE FUNCTION enrich_booking_participation_snapshot();

UPDATE booking_policy_snapshot bps
SET participation_policy_json = bps.participation_policy_json || jsonb_strip_nulls(
    jsonb_build_object(
        'minHeightCm', tpp.min_height_cm,
        'maxHeightCm', tpp.max_height_cm,
        'minWeightKg', tpp.min_weight_kg,
        'maxWeightKg', tpp.max_weight_kg
    )
)
FROM booking b
JOIN tour_schedule ts ON ts.schedule_id = b.tour_schedule_id
JOIN tour_participation_policy tpp ON tpp.tour_id = ts.tour_id
WHERE bps.booking_id = b.booking_id
  AND tpp.is_active = TRUE
  AND tpp.is_deleted = FALSE;

ALTER TABLE vendor
    DROP COLUMN IF EXISTS bank_account,
    DROP COLUMN IF EXISTS bank_name,
    DROP COLUMN IF EXISTS payment_qr_url;

-- ---------------------------------------------------------------------------
-- 7. Align persisted tour status with the Java enum
-- ---------------------------------------------------------------------------

UPDATE tour
SET status = 'APPROVED'
WHERE status = 'PUBLISHED';

ALTER TABLE tour
    ADD CONSTRAINT chk_tour_status
        CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'HIDDEN'));
