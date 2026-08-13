-- Manual-first refund workflow with optional payOS payout automation.
-- Kênh Thu and Kênh Chi use separate credentials and must never be mixed.

-- The V21 trigger references status in UPDATE OF, so it must be recreated after
-- the status column and constraints are upgraded.
DROP TRIGGER IF EXISTS trg_validate_refund_against_paid_amount
    ON refund_transaction;

ALTER TABLE refund_transaction
    DROP CONSTRAINT chk_rt_status,
    DROP CONSTRAINT chk_rt_refund_method,
    DROP CONSTRAINT chk_rt_manual_destination,
    ALTER COLUMN status TYPE VARCHAR(30),
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_retry_at TIMESTAMP,
    ADD COLUMN due_at TIMESTAMP,
    ADD COLUMN manual_bank_reference VARCHAR(100),
    ADD COLUMN manual_receipt_url VARCHAR(500),
    ADD COLUMN manual_submitted_at TIMESTAMP,
    ADD COLUMN admin_reviewed_at TIMESTAMP,
    ADD COLUMN admin_review_note VARCHAR(500);

UPDATE refund_transaction
SET due_at = COALESCE(requested_at, created_at, CURRENT_TIMESTAMP) + INTERVAL '48 hours'
WHERE due_at IS NULL;

-- Older manual refunds stored the bank reference in gateway_refund_id, which is
-- globally unique and intended only for provider payout IDs.
UPDATE refund_transaction
SET manual_bank_reference = gateway_refund_id,
    gateway_refund_id = NULL
WHERE refund_method = 'MANUAL'
  AND gateway_refund_id IS NOT NULL;

-- Compatibility for databases that were prepared with the previous status name.
UPDATE refund_transaction
SET status = 'AWAITING_VENDOR_ACTION'
WHERE status = 'AWAITING_VENDOR_FUNDS';

-- Existing retryable refunds must not be sent using collection credentials.
UPDATE refund_transaction
SET refund_method = 'MANUAL'
WHERE status IN ('PENDING', 'FAILED', 'AWAITING_VENDOR_ACTION', 'OVERDUE');

ALTER TABLE refund_transaction
    ALTER COLUMN due_at SET NOT NULL,
    ALTER COLUMN due_at SET DEFAULT (CURRENT_TIMESTAMP + INTERVAL '48 hours'),
    ADD CONSTRAINT chk_rt_attempt_count CHECK (attempt_count >= 0),
    ADD CONSTRAINT chk_rt_status CHECK (status IN (
        'PENDING', 'AWAITING_VENDOR_ACTION', 'PROCESSING', 'MANUAL_REVIEW',
        'OVERDUE', 'REFUNDED', 'FAILED', 'CANCELLED'
    )),
    ADD CONSTRAINT chk_rt_refund_method CHECK (refund_method IN (
        'PAYOUT', 'MANUAL', 'GATEWAY_REFUND'
    ));

ALTER TABLE vendor_payment_account
    ADD COLUMN refund_hold BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN payout_provider_channel_id VARCHAR(255),
    ADD COLUMN payout_api_key_encrypted TEXT,
    ADD COLUMN payout_checksum_key_encrypted TEXT,
    ADD COLUMN payout_status VARCHAR(20),
    ADD COLUMN payout_account_number VARCHAR(50),
    ADD COLUMN payout_account_name VARCHAR(255),
    ADD CONSTRAINT chk_vpa_payout_status
        CHECK (payout_status IS NULL OR payout_status IN (
            'PENDING', 'ACTIVE', 'SUSPENDED', 'REJECTED'
        )),
    ADD CONSTRAINT chk_vpa_payout_credentials_complete
        CHECK (
            (payout_provider_channel_id IS NULL
                AND payout_api_key_encrypted IS NULL
                AND payout_checksum_key_encrypted IS NULL
                AND payout_status IS NULL)
            OR
            (payout_provider_channel_id IS NOT NULL
                AND payout_api_key_encrypted IS NOT NULL
                AND payout_checksum_key_encrypted IS NOT NULL
                AND payout_status IS NOT NULL)
        );

CREATE INDEX idx_rt_automatic_processing
    ON refund_transaction (status, next_retry_at, requested_at)
    WHERE is_deleted = FALSE
      AND status IN ('PENDING', 'FAILED');

CREATE INDEX idx_rt_refund_due
    ON refund_transaction (due_at)
    WHERE is_deleted = FALSE
      AND status IN ('PENDING', 'FAILED', 'AWAITING_VENDOR_ACTION');

CREATE OR REPLACE FUNCTION validate_refund_against_paid_amount()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_paid_amount DECIMAL(12,2);
    v_payment_status VARCHAR(20);
    v_existing_refunds DECIMAL(12,2);
BEGIN
    IF NEW.is_deleted = TRUE
       OR NEW.status NOT IN (
            'PENDING', 'AWAITING_VENDOR_ACTION', 'PROCESSING', 'MANUAL_REVIEW',
            'OVERDUE', 'REFUNDED'
       ) THEN
        RETURN NEW;
    END IF;

    SELECT paid_amount, status
    INTO v_paid_amount, v_payment_status
    FROM payment_transaction
    WHERE payment_transaction_id = NEW.payment_transaction_id
    FOR UPDATE;

    IF NOT FOUND OR v_payment_status <> 'PAID' THEN
        RAISE EXCEPTION 'Payment transaction % is missing or not paid',
            NEW.payment_transaction_id;
    END IF;

    SELECT COALESCE(SUM(amount), 0)
    INTO v_existing_refunds
    FROM refund_transaction
    WHERE payment_transaction_id = NEW.payment_transaction_id
      AND refund_transaction_id <> NEW.refund_transaction_id
      AND status IN (
          'PENDING', 'AWAITING_VENDOR_ACTION', 'PROCESSING', 'MANUAL_REVIEW',
          'OVERDUE', 'REFUNDED'
      )
      AND is_deleted = FALSE;

    IF v_existing_refunds + NEW.amount > v_paid_amount THEN
        RAISE EXCEPTION 'Refund total exceeds paid amount for payment transaction %',
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
