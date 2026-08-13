-- Required identity data for a clean TrekSphere installation.
-- No demo or transactional data belongs in this migration.

INSERT INTO role (role_id, role_name, description) VALUES
    ('a85194bd-7aa5-4d85-9400-1d145000675c', 'ADMIN', 'System administrator'),
    ('d16abaf9-b60a-4d43-bcdd-8ce760b17041', 'TREKKER', 'Trekking service customer'),
    ('6162afa8-8a00-4477-9e5e-5ce2ddb3258d', 'VENDOR_MANAGER', 'Tour vendor manager'),
    ('8e546541-f1fc-4329-be3e-1226990612b2', 'VENDOR_STAFF', 'Tour vendor staff'),
    ('c63b1988-91dd-4407-899c-18b7e049c89c', 'COORDINATOR', 'Tour coordinator');

-- Local-only administrator account.
-- Login: admin@treksphere.com
-- Password: Pass123@ (stored as BCrypt below)
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
) VALUES (
    'b70fbe4a-6199-416a-aae1-536b8c497b3e',
    'admin@treksphere.com',
    'System Admin',
    '$2a$12$sonrfvUcqMSzbmj5hiexsObseflEPbuKFR8waE33GySrb8aXEbYTu',
    '0901000000',
    'ACTIVE',
    TRUE,
    'LOCAL',
    FALSE
);

INSERT INTO user_role (user_id, role_id) VALUES (
    'b70fbe4a-6199-416a-aae1-536b8c497b3e',
    'a85194bd-7aa5-4d85-9400-1d145000675c'
);

