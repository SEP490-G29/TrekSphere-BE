package com.sep.treksphere.migration;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm tra seed data (V2..V6) dựng đúng trên một database trống, để bắt lỗi
 * vi phạm CHECK/FK trước khi migration chạy trên database thật.
 */
class SeedDataVerificationTest {

    @Test
    void seedDataMatchesExpectedShape() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder().start()) {
            Flyway.configure()
                    .dataSource(postgres.getPostgresDatabase())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            JdbcTemplate jdbc = new JdbcTemplate(postgres.getPostgresDatabase());

            // Số lượng bản ghi theo từng bảng
            assertEquals(3, count(jdbc, "role"));
            assertEquals(30, count(jdbc, "permission"));
            assertEquals(41, count(jdbc, "role_permission"));
            assertEquals(8, count(jdbc, "users"));
            assertEquals(3, count(jdbc, "vendor"));
            assertEquals(4, count(jdbc, "vendor_application"));
            assertEquals(12, count(jdbc, "tour"));
            assertEquals(19, count(jdbc, "tour_schedule"));
            assertEquals(13, count(jdbc, "tour_image"));
            assertEquals(13, count(jdbc, "tour_checkpoint"));
            assertEquals(7, count(jdbc, "tour_participation_policy"));
            assertEquals(10, count(jdbc, "blog"));
            assertEquals(21, count(jdbc, "blog_comment"));

            // Phân bổ trạng thái tour
            assertEquals(9, countWhere(jdbc, "tour", "status = 'PUBLISHED'"));
            assertEquals(2, countWhere(jdbc, "tour", "status = 'DRAFT'"));
            assertEquals(1, countWhere(jdbc, "tour", "status = 'HIDDEN'"));

            // Mọi tour PUBLISHED đều phải có published_at
            assertEquals(0, countWhere(jdbc, "tour",
                    "status = 'PUBLISHED' AND published_at IS NULL"));

            // Phân bổ trạng thái blog
            assertEquals(8, countWhere(jdbc, "blog", "status = 'PUBLISHED'"));
            assertEquals(1, countWhere(jdbc, "blog", "status = 'DRAFT'"));
            assertEquals(1, countWhere(jdbc, "blog", "status = 'HIDDEN'"));

            // Mỗi vendor manager phải giữ cả VENDOR lẫn TREKKER
            List<Map<String, Object>> managerRoles = jdbc.queryForList("""
                    SELECT u.email, string_agg(r.role_name, ',' ORDER BY r.role_name) AS roles
                    FROM vendor v
                    JOIN users u ON u.user_id = v.manager_id
                    JOIN user_role ur ON ur.user_id = u.user_id
                    JOIN role r ON r.role_id = ur.role_id
                    GROUP BY u.email
                    """);
            assertEquals(3, managerRoles.size());
            managerRoles.forEach(row -> assertEquals("TREKKER,VENDOR", row.get("roles")));

            // VENDOR không được giữ quyền kiểm duyệt TOUR_HIDE_UNHIDE
            assertEquals(0, countRolePermission(jdbc, "VENDOR", "TOUR", "HIDE_UNHIDE"));
            assertEquals(1, countRolePermission(jdbc, "ADMIN", "TOUR", "HIDE_UNHIDE"));
            assertEquals(1, countRolePermission(jdbc, "VENDOR", "TOUR", "PUBLISH"));

            // Số quyền cấp cho từng role
            assertEquals(15, countRoleGrants(jdbc, "ADMIN"));
            assertEquals(10, countRoleGrants(jdbc, "VENDOR"));
            assertEquals(16, countRoleGrants(jdbc, "TREKKER"));

            // preferred_areas / skills phải là mảng JSON hợp lệ
            assertEquals(0, countWhere(jdbc, "users",
                    "jsonb_typeof(preferred_areas) <> 'array' OR jsonb_typeof(skills) <> 'array'"));

            // Tài khoản admin đăng nhập được bằng hash BCrypt đã seed
            assertEquals(1, countWhere(jdbc, "users",
                    "email = 'admin@treksphere.com' AND password_hash LIKE '$2a$%' AND status = 'ACTIVE'"));

            // Comment trả lời phải trỏ tới comment cha cùng blog
            assertEquals(0, jdbc.queryForObject("""
                    SELECT COUNT(*) FROM blog_comment c
                    JOIN blog_comment p ON p.blog_comment_id = c.parent_comment_id
                    WHERE c.blog_id <> p.blog_id
                    """, Integer.class));
            assertTrue(countWhere(jdbc, "blog_comment", "parent_comment_id IS NOT NULL") >= 4);

            // Đơn vendor_application không phải DRAFT phải điền đủ trường bắt buộc
            assertEquals(0, countWhere(jdbc, "vendor_application", """
                    application_status <> 'DRAFT' AND (
                        company_name IS NULL OR contact_email IS NULL OR contact_phone IS NULL
                        OR tax_code IS NULL OR business_license_url IS NULL
                        OR business_description IS NULL OR business_address IS NULL
                        OR legal_representative_name IS NULL
                        OR legal_representative_position IS NULL)
                    """));

            // Lịch khởi hành OPEN đều nằm trong tương lai
            assertEquals(0, countWhere(jdbc, "tour_schedule",
                    "status = 'OPEN' AND departure_date <= CURRENT_DATE"));
        }
    }

    private int count(JdbcTemplate jdbc, String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private int countWhere(JdbcTemplate jdbc, String table, String predicate) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + predicate, Integer.class);
    }

    private int countRoleGrants(JdbcTemplate jdbc, String role) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM role_permission rp
                JOIN role r ON r.role_id = rp.role_id
                WHERE r.role_name = ?
                """, Integer.class, role);
    }

    private int countRolePermission(JdbcTemplate jdbc, String role, String resource, String action) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM role_permission rp
                JOIN role r ON r.role_id = rp.role_id
                JOIN permission p ON p.permission_id = rp.permission_id
                WHERE r.role_name = ? AND p.resource = ? AND p.action = ?
                """, Integer.class, role, resource, action);
    }
}
