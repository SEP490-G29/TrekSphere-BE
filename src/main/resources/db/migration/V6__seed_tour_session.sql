-- V6__seed_tour_session.sql
-- Seed data cho tour_session, coordinator_schedule, porter_schedule, session_equipment, session_checkpoint_log, sos_alert

-- 1. Bảng TOUR_SESSION
-- Session 1: Sắp diễn ra (PENDING)
INSERT INTO tour_session (tour_session_id, tour_schedule_id, status, started_at, ended_at)
SELECT gen_random_uuid(), schedule_id, 'PENDING', NULL, NULL
FROM tour_schedule WHERE status = 'OPEN';

-- Session 2: Đang diễn ra (IN_PROGRESS)
INSERT INTO tour_session (tour_session_id, tour_schedule_id, status, started_at, ended_at)
SELECT gen_random_uuid(), schedule_id, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '1 days', NULL
FROM tour_schedule WHERE status = 'CLOSED';

-- Session 3: Đã hoàn thành (COMPLETED)
INSERT INTO tour_session (tour_session_id, tour_schedule_id, status, started_at, ended_at)
SELECT gen_random_uuid(), schedule_id, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '28 days'
FROM tour_schedule WHERE status = 'COMPLETED';

-- 2. Bảng COORDINATOR_SCHEDULE (Điều phối viên dẫn đoàn)
INSERT INTO coordinator_schedule (coordinator_schedule_id, tour_session_id, coordinator_id, is_lead)
SELECT gen_random_uuid(), s.tour_session_id, u.user_id, true
FROM tour_session s, users u
WHERE s.status = 'IN_PROGRESS' AND u.email = 'coordinator1@treksphere.com';

INSERT INTO coordinator_schedule (coordinator_schedule_id, tour_session_id, coordinator_id, is_lead)
SELECT gen_random_uuid(), s.tour_session_id, u.user_id, true
FROM tour_session s, users u
WHERE s.status = 'COMPLETED' AND u.email = 'coordinator2@treksphere.com';

-- 3. Bảng PORTER_SCHEDULE (Porter đi cùng)
INSERT INTO porter_schedule (porter_schedule_id, tour_session_id, porter_id, note)
SELECT gen_random_uuid(), s.tour_session_id, p.porter_id, 'Mang lều và đồ ăn'
FROM tour_session s, porter_profile p
WHERE s.status = 'IN_PROGRESS' AND p.full_name = 'Sùng A Chứ';

-- 4. Bảng SESSION_EQUIPMENT (Đồ đạc mang theo)
INSERT INTO session_equipment (session_equipment_id, tour_session_id, equipment_id, quantity, is_checked, checked_by, note)
SELECT gen_random_uuid(), s.tour_session_id, e.equipment_id, 2, true, u.user_id, 'Đã kiểm tra đủ lều'
FROM tour_session s, vendor_equipment e, users u
WHERE s.status = 'IN_PROGRESS' AND e.equipment_name LIKE 'Lều%' AND u.email = 'coordinator1@treksphere.com';

-- 5. Bảng SESSION_CHECKPOINT_LOG (Lịch sử check-in điểm đến)
INSERT INTO session_checkpoint_log (session_checkpoint_log_id, tour_session_id, checkpoint_id, status, reached_at, note)
SELECT gen_random_uuid(), s.tour_session_id, c.checkpoint_id, 'REACHED', CURRENT_TIMESTAMP, 'Đoàn đã đến Trạm Tôn an toàn'
FROM tour_session s, tour_checkpoint c
WHERE s.status = 'IN_PROGRESS' AND c.checkpoint_name = 'Trạm Tôn';

-- 6. Bảng SOS_ALERT (Báo động khẩn cấp)
INSERT INTO sos_alert (sos_alert_id, tour_session_id, sender_id, latitude, longitude, message, status)
SELECT gen_random_uuid(), s.tour_session_id, u.user_id, 22.3550, 103.7740, 'Khách bị trật chân', 'PENDING'
FROM tour_session s, users u
WHERE s.status = 'IN_PROGRESS' AND u.email = 'trekker1@treksphere.com';
