ALTER TABLE refund_transaction
    DROP CONSTRAINT chk_rt_status,
    ALTER COLUMN status TYPE VARCHAR(30);

ALTER TABLE refund_transaction
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_retry_at TIMESTAMP,
    ADD COLUMN due_at TIMESTAMP;

UPDATE refund_transaction
SET due_at = COALESCE(requested_at, created_at, CURRENT_TIMESTAMP) + INTERVAL '48 hours'
WHERE due_at IS NULL;

ALTER TABLE refund_transaction
    ALTER COLUMN due_at SET NOT NULL,
    ALTER COLUMN due_at SET DEFAULT (CURRENT_TIMESTAMP + INTERVAL '48 hours'),
    ADD CONSTRAINT chk_rt_attempt_count CHECK (attempt_count >= 0),
    ADD CONSTRAINT chk_rt_status CHECK (status IN (
        'PENDING', 'PROCESSING', 'AWAITING_VENDOR_FUNDS', 'OVERDUE',
        'REFUNDED', 'FAILED', 'CANCELLED'
    ));

CREATE INDEX idx_rt_automatic_processing
    ON refund_transaction (status, next_retry_at, requested_at)
    WHERE is_deleted = FALSE
      AND status IN ('PENDING', 'FAILED', 'AWAITING_VENDOR_FUNDS', 'OVERDUE');

CREATE INDEX idx_rt_refund_due
    ON refund_transaction (due_at)
    WHERE is_deleted = FALSE
      AND status IN ('PENDING', 'FAILED', 'AWAITING_VENDOR_FUNDS');

ALTER TABLE vendor_payment_account
    ADD COLUMN refund_hold BOOLEAN NOT NULL DEFAULT FALSE;

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
            'PENDING', 'PROCESSING', 'AWAITING_VENDOR_FUNDS', 'OVERDUE', 'REFUNDED'
       ) THEN
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
      AND status IN ('PENDING', 'PROCESSING', 'AWAITING_VENDOR_FUNDS', 'OVERDUE', 'REFUNDED')
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
