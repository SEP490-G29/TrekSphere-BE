-- Sample accounts for local/dev testing only.
-- Password for every account below: Pass123@ (same BCrypt hash as the seeded admin in V2).

-- ============================
-- TREKKER accounts (2)
-- ============================
-- Login: trekker1@treksphere.com
-- Login: trekker2@treksphere.com
INSERT INTO users (
    user_id,
    email,
    full_name,
    password_hash,
    phone,
    status,
    email_verified,
    provider,
    is_deleted
) VALUES
    ('1a2b3c4d-0001-4a1b-9c2d-000000000001', 'trekker1@treksphere.com', 'Nguyen Van A', '$2a$12$4J3OyTUEsWYCKFGpZrW0oe1UJe6mC1RmBNrTduveEVj5/85pq7O2W', '0901000001', 'ACTIVE', TRUE, 'LOCAL', FALSE),
    ('1a2b3c4d-0002-4a1b-9c2d-000000000002', 'trekker2@treksphere.com', 'Tran Thi B', '$2a$12$4J3OyTUEsWYCKFGpZrW0oe1UJe6mC1RmBNrTduveEVj5/85pq7O2W', '0901000002', 'ACTIVE', TRUE, 'LOCAL', FALSE);

INSERT INTO user_role (user_id, role_id) VALUES
    ('1a2b3c4d-0001-4a1b-9c2d-000000000001', 'd16abaf9-b60a-4d43-bcdd-8ce760b17041'),
    ('1a2b3c4d-0002-4a1b-9c2d-000000000002', 'd16abaf9-b60a-4d43-bcdd-8ce760b17041');

-- ============================
-- VENDOR accounts (2) — each with an ACTIVE vendor profile
-- ============================
-- Login: vendor1@treksphere.com (manages "TrekViet Adventures")
-- Login: vendor2@treksphere.com (manages "Mountain Trails Co")
INSERT INTO users (
    user_id,
    email,
    full_name,
    password_hash,
    phone,
    status,
    email_verified,
    provider,
    is_deleted
) VALUES
    ('1a2b3c4d-0003-4a1b-9c2d-000000000003', 'vendor1@treksphere.com', 'Le Van C', '$2a$12$4J3OyTUEsWYCKFGpZrW0oe1UJe6mC1RmBNrTduveEVj5/85pq7O2W', '0901000003', 'ACTIVE', TRUE, 'LOCAL', FALSE),
    ('1a2b3c4d-0004-4a1b-9c2d-000000000004', 'vendor2@treksphere.com', 'Pham Thi D', '$2a$12$4J3OyTUEsWYCKFGpZrW0oe1UJe6mC1RmBNrTduveEVj5/85pq7O2W', '0901000004', 'ACTIVE', TRUE, 'LOCAL', FALSE);

INSERT INTO user_role (user_id, role_id) VALUES
    ('1a2b3c4d-0003-4a1b-9c2d-000000000003', '6162afa8-8a00-4477-9e5e-5ce2ddb3258d'),
    ('1a2b3c4d-0004-4a1b-9c2d-000000000004', '6162afa8-8a00-4477-9e5e-5ce2ddb3258d');

INSERT INTO vendor (
    vendor_id,
    manager_id,
    company_name,
    description,
    contact_email,
    contact_phone,
    tax_code,
    business_license_url,
    status,
    is_deleted
) VALUES
    ('2b3c4d5e-0001-4b2c-8d3e-000000000001', '1a2b3c4d-0003-4a1b-9c2d-000000000003', 'TrekViet Adventures', 'Sample vendor seeded for local testing.', 'contact@trekviet.example.com', '0281000001', 'TAXSAMPLE0001', 'https://example.com/licenses/trekviet.pdf', 'ACTIVE', FALSE),
    ('2b3c4d5e-0002-4b2c-8d3e-000000000002', '1a2b3c4d-0004-4a1b-9c2d-000000000004', 'Mountain Trails Co', 'Sample vendor seeded for local testing.', 'contact@mountaintrails.example.com', '0281000002', 'TAXSAMPLE0002', 'https://example.com/licenses/mountaintrails.pdf', 'ACTIVE', FALSE);
