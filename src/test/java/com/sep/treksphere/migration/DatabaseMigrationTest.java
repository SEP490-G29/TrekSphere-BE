package com.sep.treksphere.migration;

import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.vendor.statistics.VendorTourStatisticsRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationTest {

    @Test
    void migrationsBuildCurrentSchemaAndPreserveDualRoleVendor() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder().start()) {
            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getPostgresDatabase())
                    .locations("classpath:db/migration")
                    .load();
            assertEquals(12, flyway.migrate().migrationsExecuted);

            try (Connection connection = postgres.getPostgresDatabase().getConnection()) {
                assertTrue(hasColumn(connection, "tour", "published_at"));
                assertTrue(hasColumn(connection, "tour_schedule", "cancellation_reason"));
                assertTrue(hasColumn(connection, "message", "attachment_url"));
                assertTrue(hasColumn(connection, "vendor_application", "reviewed_by"));
                assertTrue(hasColumn(connection, "group_join_application", "application_id"));
                assertTrue(hasColumn(connection, "matching_member", "source_application_id"));


                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT COUNT(*)
                        FROM vendor v
                        JOIN user_role ur ON ur.user_id = v.manager_id
                        JOIN role r ON r.role_id = ur.role_id
                        WHERE r.role_name = 'TREKKER'
                        """); ResultSet result = statement.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(2, result.getInt(1));
                }

                assertEquals(0, countPermission(connection, "VENDOR", "TOUR", "HIDE_UNHIDE"));
                assertEquals(1, countPermission(connection, "ADMIN", "TOUR", "HIDE_UNHIDE"));
            }
        }
    }

    @Test
    void statisticsAndRecommendationNativeQueriesRunOnPostgres() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder().start()) {
            Flyway.configure()
                    .dataSource(postgres.getPostgresDatabase())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            JdbcTemplate jdbc = new JdbcTemplate(postgres.getPostgresDatabase());
            NamedParameterJdbcTemplate namedJdbc = new NamedParameterJdbcTemplate(jdbc);
            UUID vendorId = UUID.fromString("2b3c4d5e-0001-4b2c-8d3e-000000000001");
            UUID tourId = UUID.fromString("3c4d5e6f-0001-4c3d-8e4f-000000000001");
            jdbc.update("""
                    INSERT INTO tour (
                        tour_id, vendor_id, tour_name, description, duration_days,
                        min_capacity, max_capacity, difficulty, status, cover_image_url,
                        location, creator_id, is_deleted, published_at
                    ) VALUES (?, ?, 'Fansipan test', 'Tour for native query testing', 2,
                              4, 12, 'MODERATE', 'PUBLISHED', 'https://example.com/cover.jpg',
                              'Sa Pa, Lao Cai', ?, FALSE, CURRENT_TIMESTAMP)
                    """, tourId, vendorId,
                    UUID.fromString("1a2b3c4d-0003-4a1b-9c2d-000000000003"));
            jdbc.update("""
                    INSERT INTO tour_schedule (
                        tour_schedule_id, tour_id, departure_date, return_date,
                        price, status, is_deleted
                    ) VALUES (?, ?, CURRENT_DATE + 30, CURRENT_DATE + 31,
                              2500000, 'OPEN', FALSE)
                    """, UUID.fromString("4d5e6f70-0001-4d4e-8f50-000000000001"), tourId);

            Query statsAnnotation = VendorTourStatisticsRepository.class.getMethod(
                            "getTourStatistics", UUID.class, String.class, String.class,
                            String.class, String.class, Pageable.class)
                    .getAnnotation(Query.class);
            MapSqlParameterSource statsParams = new MapSqlParameterSource()
                    .addValue("vendorId", vendorId)
                    .addValue("keyword", null, Types.VARCHAR)
                    .addValue("status", null, Types.VARCHAR)
                    .addValue("sortBy", "matchingGroupCount")
                    .addValue("sortDir", "desc");
            List<Map<String, Object>> statsRows = namedJdbc.queryForList(
                    statsAnnotation.value(), statsParams);
            assertEquals(1, statsRows.size());
            assertEquals(tourId, statsRows.getFirst().get("tourId"));

            Query recommendationsAnnotation = TourRepository.class.getMethod(
                            "findRecommendedTours", String.class, String.class,
                            Integer.class, boolean.class, Pageable.class)
                    .getAnnotation(Query.class);
            MapSqlParameterSource recommendationParams = new MapSqlParameterSource()
                    .addValue("areasJson", "[\"lao cai\"]")
                    .addValue("preferredDifficulty", "MODERATE", Types.VARCHAR)
                    .addValue("maxDifficultyRank", 2, Types.INTEGER)
                    .addValue("usePreferences", true, Types.BOOLEAN);
            List<Map<String, Object>> recommendationRows = namedJdbc.queryForList(
                    recommendationsAnnotation.value(), recommendationParams);
            assertEquals(1, recommendationRows.size());
            assertEquals(tourId, recommendationRows.getFirst().get("tour_id"));
        }
    }

    private boolean hasColumn(Connection connection, String table, String column) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                )
                """)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    private int countPermission(Connection connection, String role, String resource, String action)
            throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM role_permission rp
                JOIN role r ON r.role_id = rp.role_id
                JOIN permission p ON p.permission_id = rp.permission_id
                WHERE r.role_name = ? AND p.resource = ? AND p.action = ?
                """)) {
            statement.setString(1, role);
            statement.setString(2, resource);
            statement.setString(3, action);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getInt(1);
            }
        }
    }
}
