-- Vendor application drafts may be incomplete. Submission completeness is
-- enforced by both the service and this constraint for all new/changed rows.
ALTER TABLE vendor_application DROP CONSTRAINT chk_va_status;
ALTER TABLE vendor_application
    ADD CONSTRAINT chk_va_status
        CHECK (application_status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED'));

ALTER TABLE vendor_application ALTER COLUMN company_name DROP NOT NULL;
ALTER TABLE vendor_application ALTER COLUMN contact_email DROP NOT NULL;
ALTER TABLE vendor_application ALTER COLUMN contact_phone DROP NOT NULL;
ALTER TABLE vendor_application ALTER COLUMN tax_code DROP NOT NULL;
ALTER TABLE vendor_application ALTER COLUMN business_license_url DROP NOT NULL;

ALTER TABLE vendor_application
    ADD COLUMN business_address VARCHAR(500),
    ADD COLUMN legal_representative_name VARCHAR(255),
    ADD COLUMN legal_representative_position VARCHAR(255),
    ADD COLUMN website_url VARCHAR(500),
    ADD COLUMN reviewed_by UUID,
    ADD COLUMN reviewed_at TIMESTAMP,
    ADD COLUMN vendor_id UUID;

ALTER TABLE vendor_application
    ADD CONSTRAINT fk_va_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(user_id),
    ADD CONSTRAINT fk_va_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(vendor_id),
    ADD CONSTRAINT uq_va_vendor UNIQUE (vendor_id),
    ADD CONSTRAINT chk_va_submission_complete CHECK (
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
    ) NOT VALID;

ALTER TABLE vendor
    ADD COLUMN business_address VARCHAR(500),
    ADD COLUMN legal_representative_name VARCHAR(255),
    ADD COLUMN legal_representative_position VARCHAR(255),
    ADD COLUMN website_url VARCHAR(500);

-- A Vendor is still a Trekker. Add TREKKER to all existing vendor managers.
INSERT INTO user_role (user_id, role_id)
SELECT v.manager_id, r.role_id
FROM vendor v
JOIN role r ON r.role_name = 'TREKKER'
WHERE v.is_deleted = FALSE
ON CONFLICT (user_id, role_id) DO NOTHING;

-- HIDE_UNHIDE is an Admin moderation permission. Vendors use PUBLISH to
-- publish/unpublish their own tours.
DELETE FROM role_permission rp
USING role r, permission p
WHERE rp.role_id = r.role_id
  AND rp.permission_id = p.permission_id
  AND r.role_name = 'VENDOR'
  AND p.resource = 'TOUR'
  AND p.action = 'HIDE_UNHIDE';

UPDATE permission
SET description = 'Admin ẩn/mở lại Tour khi kiểm duyệt nội dung'
WHERE resource = 'TOUR' AND action = 'HIDE_UNHIDE';

CREATE INDEX ix_vendor_application_applicant_status
    ON vendor_application (applicant_id, application_status)
    WHERE is_deleted = FALSE;
