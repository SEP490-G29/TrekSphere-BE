package com.sep.treksphere.repository;

import com.sep.treksphere.tour.entity.Tour;
import com.sep.treksphere.tour.repository.TourRepository;
import com.sep.treksphere.tour.repository.TourRecommendationRepository;
import com.sep.treksphere.vendor.repository.VendorStatisticsOverviewProjection;
import com.sep.treksphere.vendor.repository.VendorTourStatisticProjection;
import com.sep.treksphere.vendor.repository.VendorTourStatisticsRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
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
    private static final UUID TREKKER_ID = UUID.fromString(
            "1a2b3c4d-0001-4a1b-9c2d-000000000001");
    private static final UUID SECOND_TOUR_ID = UUID.fromString(
            "3c4d5e6f-0002-4c3d-8e4f-000000000002");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private VendorTourStatisticsRepository statisticsRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private TourRecommendationRepository recommendationRepository;

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
                    min_capacity, max_capacity, price, difficulty, status, cover_image_url,
                    location, creator_id, is_deleted, published_at
                ) VALUES (?, ?, 'Fansipan repository test', 'Tour for repository testing', 2,
                          4, 12, 2500000, 'MODERATE', 'PUBLISHED', 'https://example.com/cover.jpg',
                          'Sa Pa, Lao Cai', ?, FALSE, CURRENT_TIMESTAMP)
                """, TOUR_ID, VENDOR_ID,
                UUID.fromString("1a2b3c4d-0003-4a1b-9c2d-000000000003"));
        jdbc.update("""
                INSERT INTO tour_schedule (
                    tour_schedule_id, tour_id, departure_date, return_date,
                    status, is_deleted
                ) VALUES (?, ?, CURRENT_DATE + 30, CURRENT_DATE + 31,
                          'OPEN', FALSE)
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
        Page<Tour> page = recommendationRepository.findPersonalizedRecommendations(
                TREKKER_ID,
                "[\"lao cai\"]",
                "[]",
                "MODERATE",
                null,
                2,
                PageRequest.of(0, 1));
        assertFalse(page.isEmpty());
        assertEquals(TOUR_ID, page.getContent().getFirst().getTourId());
        assertEquals(1L, page.getTotalElements());
    }

    @Test
    void onlyCompletedGroupsContributeToTrekkerHistory() {
        seedSecondTour("Ta Xua, Son La", "HARD");
        insertMatchingGroup(
                UUID.fromString("5e6f7081-0001-4e5f-8061-000000000001"),
                TOUR_ID,
                "COMPLETED");
        insertMatchingGroup(
                UUID.fromString("5e6f7081-0002-4e5f-8061-000000000002"),
                SECOND_TOUR_ID,
                "OPEN");

        assertEquals(List.of("Sa Pa, Lao Cai"),
                recommendationRepository.findCompletedTourLocations(TREKKER_ID));
        assertEquals(List.of(com.sep.treksphere.tour.enums.DifficultyLevel.MODERATE),
                recommendationRepository.findCompletedTourDifficulties(TREKKER_ID));
    }

    @Test
    void ranksAnUnseenTourAheadOfAnAlreadyCompletedTour() {
        seedSecondTour("Ba Vi, Ha Noi", "MODERATE");
        insertMatchingGroup(
                UUID.fromString("5e6f7081-0003-4e5f-8061-000000000003"),
                TOUR_ID,
                "COMPLETED");

        Page<Tour> page = recommendationRepository.findPersonalizedRecommendations(
                TREKKER_ID,
                "[\"lao cai\"]",
                "[]",
                "MODERATE",
                null,
                2,
                PageRequest.of(0, 10));

        assertEquals(2L, page.getTotalElements());
        assertEquals(SECOND_TOUR_ID, page.getContent().getFirst().getTourId());
        assertEquals(TOUR_ID, page.getContent().get(1).getTourId());
    }

    @Test
    void recentBehaviorRaisesARelevantTourInTheRanking() {
        seedSecondTour("Ba Vi, Ha Noi", "MODERATE");
        jdbc.update("""
                INSERT INTO tour_behavior_event (
                    behavior_event_id, user_id, tour_id, event_type,
                    source, occurred_at
                ) VALUES (?, ?, ?, 'CLICK', 'SEARCH', CURRENT_TIMESTAMP)
                """, UUID.fromString("6f708192-0001-4f60-8172-000000000001"),
                TREKKER_ID, SECOND_TOUR_ID);

        Page<Tour> page = recommendationRepository.findPersonalizedRecommendations(
                TREKKER_ID,
                "[]",
                "[]",
                null,
                null,
                2,
                PageRequest.of(0, 10));

        assertEquals(SECOND_TOUR_ID, page.getContent().getFirst().getTourId());
    }

    @Test
    void searchToursSortsByPriceAscendingAndDescending() {
        seedSecondTour("Ba Vi, Ha Noi", "MODERATE");
        UUID cheapTourId = UUID.fromString("3c4d5e6f-0003-4c3d-8e4f-000000000003");
        seedTourWithPrice(cheapTourId, "Da Lat, Lam Dong", "EASY", new BigDecimal("500000"));

        Sort ascByPrice = Sort.by(Sort.Direction.ASC, "price");
        Page<Tour> ascPage = tourRepository.searchTours(
                com.sep.treksphere.tour.enums.TourStatus.PUBLISHED,
                null, null, null, null, null, null,
                PageRequest.of(0, 10, ascByPrice));
        assertEquals(cheapTourId, ascPage.getContent().get(0).getTourId());
        assertEquals(TOUR_ID, ascPage.getContent().get(1).getTourId());
        assertEquals(SECOND_TOUR_ID, ascPage.getContent().get(2).getTourId());

        Sort descByPrice = Sort.by(Sort.Direction.DESC, "price");
        Page<Tour> descPage = tourRepository.searchTours(
                com.sep.treksphere.tour.enums.TourStatus.PUBLISHED,
                null, null, null, null, null, null,
                PageRequest.of(0, 10, descByPrice));
        assertEquals(SECOND_TOUR_ID, descPage.getContent().get(0).getTourId());
        assertEquals(TOUR_ID, descPage.getContent().get(1).getTourId());
        assertEquals(cheapTourId, descPage.getContent().get(2).getTourId());
    }

    private void seedTourWithPrice(UUID tourId, String location, String difficulty, BigDecimal price) {
        jdbc.update("""
                INSERT INTO tour (
                    tour_id, vendor_id, tour_name, description, duration_days,
                    min_capacity, max_capacity, price, difficulty, status, cover_image_url,
                    location, creator_id, is_deleted, published_at
                ) VALUES (?, ?, 'Priced repository tour', 'Tour seeded with a specific price', 2,
                          4, 12, ?, ?, 'PUBLISHED', 'https://example.com/priced-cover.jpg',
                          ?, ?, FALSE, CURRENT_TIMESTAMP)
                """, tourId, VENDOR_ID, price, difficulty, location,
                UUID.fromString("1a2b3c4d-0003-4a1b-9c2d-000000000003"));
    }

    private void seedSecondTour(String location, String difficulty) {
        jdbc.update("""
                INSERT INTO tour (
                    tour_id, vendor_id, tour_name, description, duration_days,
                    min_capacity, max_capacity, price, difficulty, status, cover_image_url,
                    location, creator_id, is_deleted, published_at
                ) VALUES (?, ?, 'Second repository tour', 'Second recommendation candidate', 2,
                          4, 12, 2800000, ?, 'PUBLISHED', 'https://example.com/second-cover.jpg',
                          ?, ?, FALSE, CURRENT_TIMESTAMP)
                """, SECOND_TOUR_ID, VENDOR_ID, difficulty, location,
                UUID.fromString("1a2b3c4d-0003-4a1b-9c2d-000000000003"));
        jdbc.update("""
                INSERT INTO tour_schedule (
                    tour_schedule_id, tour_id, departure_date, return_date,
                    status, is_deleted
                ) VALUES (?, ?, CURRENT_DATE + 40, CURRENT_DATE + 42,
                          'OPEN', FALSE)
                """, UUID.fromString("4d5e6f70-0002-4d4e-8f50-000000000002"), SECOND_TOUR_ID);
    }

    private void insertMatchingGroup(UUID groupId, UUID tourId, String status) {
        jdbc.update("""
                INSERT INTO matching_group (
                    matching_group_id, tour_id, owner_id, group_name,
                    max_size, current_size, target_date, matching_deadline,
                    status, is_deleted
                ) VALUES (?, ?, ?, 'Recommendation history group',
                          6, 1, CURRENT_DATE + 20, CURRENT_TIMESTAMP + INTERVAL '10 days',
                          ?, FALSE)
                """, groupId, tourId, TREKKER_ID, status);
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
