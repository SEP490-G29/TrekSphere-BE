package com.sep.treksphere.repository;

import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.vendor.statistics.VendorStatisticsOverviewProjection;
import com.sep.treksphere.vendor.statistics.VendorTourStatisticProjection;
import com.sep.treksphere.vendor.statistics.VendorTourStatisticsRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TourQueryRepositoryTest {

    private static final EmbeddedPostgres POSTGRES = startPostgres();
    private static final UUID VENDOR_ID = UUID.fromString(
            "2b3c4d5e-0001-4b2c-8d3e-000000000001");
    private static final UUID TOUR_ID = UUID.fromString(
            "3c4d5e6f-0001-4c3d-8e4f-000000000001");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private VendorTourStatisticsRepository statisticsRepository;

    @Autowired
    private TourRepository tourRepository;

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @BeforeEach
    void seedTour() {
        jdbc.update("""
                INSERT INTO tour (
                    tour_id, vendor_id, tour_name, description, duration_days,
                    min_capacity, max_capacity, difficulty, status, cover_image_url,
                    location, creator_id, is_deleted, published_at
                ) VALUES (?, ?, 'Fansipan repository test', 'Tour for repository testing', 2,
                          4, 12, 'MODERATE', 'PUBLISHED', 'https://example.com/cover.jpg',
                          'Sa Pa, Lao Cai', ?, FALSE, CURRENT_TIMESTAMP)
                """, TOUR_ID, VENDOR_ID,
                UUID.fromString("1a2b3c4d-0003-4a1b-9c2d-000000000003"));
        jdbc.update("""
                INSERT INTO tour_schedule (
                    tour_schedule_id, tour_id, departure_date, return_date,
                    price, status, is_deleted
                ) VALUES (?, ?, CURRENT_DATE + 30, CURRENT_DATE + 31,
                          2500000, 'OPEN', FALSE)
                """, UUID.fromString("4d5e6f70-0001-4d4e-8f50-000000000001"), TOUR_ID);
    }

    @Test
    void mapsStatisticsProjectionsAndPagination() {
        VendorStatisticsOverviewProjection overview = statisticsRepository.getOverview(VENDOR_ID);
        assertEquals(1L, overview.getTotalTours());
        assertEquals(1L, overview.getPublishedTours());

        Page<VendorTourStatisticProjection> page = statisticsRepository.getTourStatistics(
                VENDOR_ID, null, null, "matchingGroupCount", "desc", PageRequest.of(0, 10));
        assertEquals(1L, page.getTotalElements());
        assertEquals(TOUR_ID, page.getContent().getFirst().getTourId());
        assertEquals(0.0, page.getContent().getFirst().getAverageFillRate());
    }

    @Test
    void mapsPersonalizedRecommendationToTourEntity() {
        Page<Tour> page = tourRepository.findRecommendedTours(
                "[\"lao cai\"]", "MODERATE", 2, true, PageRequest.of(0, 10));
        assertFalse(page.isEmpty());
        assertEquals(TOUR_ID, page.getContent().getFirst().getTourId());
        assertEquals(1L, page.getTotalElements());
    }

    @AfterAll
    static void stopPostgres() throws IOException {
        POSTGRES.close();
    }

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
