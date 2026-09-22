package com.sep.treksphere.vendor.statistics;

import com.sep.treksphere.tour.Tour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface VendorTourStatisticsRepository extends Repository<Tour, UUID> {

    @Query(value = """
            SELECT
                (SELECT COUNT(*) FROM tour t
                  WHERE t.vendor_id = :vendorId AND t.is_deleted = FALSE) AS "totalTours",
                (SELECT COUNT(*) FROM tour t
                  WHERE t.vendor_id = :vendorId AND t.status = 'DRAFT' AND t.is_deleted = FALSE) AS "draftTours",
                (SELECT COUNT(*) FROM tour t
                  WHERE t.vendor_id = :vendorId AND t.status = 'PUBLISHED' AND t.is_deleted = FALSE) AS "publishedTours",
                (SELECT COUNT(*) FROM tour t
                  WHERE t.vendor_id = :vendorId AND t.status = 'HIDDEN' AND t.is_deleted = FALSE) AS "hiddenTours",
                (SELECT COUNT(*) FROM tour_schedule ts
                  JOIN tour t ON t.tour_id = ts.tour_id
                  WHERE t.vendor_id = :vendorId AND t.is_deleted = FALSE
                    AND ts.is_deleted = FALSE AND ts.status = 'OPEN'
                    AND ts.departure_date > CURRENT_DATE) AS "futureOpenSchedules",
                (SELECT COUNT(*) FROM matching_group mg
                  JOIN tour t ON t.tour_id = mg.tour_id
                  WHERE t.vendor_id = :vendorId AND t.is_deleted = FALSE
                    AND mg.is_deleted = FALSE
                    AND mg.status IN ('OPEN','FULL','CLOSED','IN_PROGRESS','COMPLETED')) AS "matchingGroupCount",
                COALESCE((SELECT AVG(mg.current_size * 100.0 / NULLIF(mg.max_size, 0))
                  FROM matching_group mg JOIN tour t ON t.tour_id = mg.tour_id
                  WHERE t.vendor_id = :vendorId AND t.is_deleted = FALSE
                    AND mg.is_deleted = FALSE
                    AND mg.status IN ('OPEN','FULL','CLOSED','IN_PROGRESS','COMPLETED')), 0) AS "averageFillRate",
                COALESCE((SELECT COUNT(*) FILTER (WHERE mg.status IN ('FULL','IN_PROGRESS','COMPLETED')) * 100.0
                    / NULLIF(COUNT(*), 0)
                  FROM matching_group mg JOIN tour t ON t.tour_id = mg.tour_id
                  WHERE t.vendor_id = :vendorId AND t.is_deleted = FALSE
                    AND mg.is_deleted = FALSE
                    AND mg.status IN ('OPEN','FULL','CLOSED','IN_PROGRESS','COMPLETED')), 0) AS "fullGroupRate"
            """, nativeQuery = true)
    VendorStatisticsOverviewProjection getOverview(@Param("vendorId") UUID vendorId);

    @Query(value = """
            SELECT stats.*
            FROM (
                SELECT t.tour_id AS "tourId",
                       t.tour_name AS "tourName",
                       t.status AS status,
                       t.created_at AS "createdAt",
                       t.published_at AS "publishedAt",
                       (SELECT COUNT(*) FROM tour_schedule ts
                         WHERE ts.tour_id = t.tour_id AND ts.is_deleted = FALSE AND ts.status = 'OPEN') AS "openScheduleCount",
                       (SELECT COUNT(*) FROM tour_schedule ts
                         WHERE ts.tour_id = t.tour_id AND ts.is_deleted = FALSE AND ts.status = 'CLOSED') AS "closedScheduleCount",
                       (SELECT COUNT(*) FROM tour_schedule ts
                         WHERE ts.tour_id = t.tour_id AND ts.is_deleted = FALSE AND ts.status = 'CANCELLED') AS "cancelledScheduleCount",
                       (SELECT COUNT(*) FROM matching_group mg
                         WHERE mg.tour_id = t.tour_id AND mg.is_deleted = FALSE
                           AND mg.status IN ('OPEN','FULL','CLOSED','IN_PROGRESS','COMPLETED')) AS "matchingGroupCount",
                       COALESCE((SELECT AVG(mg.current_size * 100.0 / NULLIF(mg.max_size, 0))
                         FROM matching_group mg
                         WHERE mg.tour_id = t.tour_id AND mg.is_deleted = FALSE
                           AND mg.status IN ('OPEN','FULL','CLOSED','IN_PROGRESS','COMPLETED')), 0) AS "averageFillRate",
                       COALESCE((SELECT COUNT(*) FILTER (WHERE mg.status IN ('FULL','IN_PROGRESS','COMPLETED')) * 100.0
                           / NULLIF(COUNT(*), 0)
                         FROM matching_group mg
                         WHERE mg.tour_id = t.tour_id AND mg.is_deleted = FALSE
                           AND mg.status IN ('OPEN','FULL','CLOSED','IN_PROGRESS','COMPLETED')), 0) AS "fullGroupRate"
                FROM tour t
                WHERE t.vendor_id = :vendorId
                  AND t.is_deleted = FALSE
                  AND (CAST(:status AS varchar) IS NULL OR t.status = :status)
                  AND (CAST(:keyword AS varchar) IS NULL
                       OR LOWER(t.tour_name) LIKE LOWER(CONCAT('%', CAST(:keyword AS varchar), '%'))
                       OR LOWER(t.location) LIKE LOWER(CONCAT('%', CAST(:keyword AS varchar), '%')))
            ) stats
            ORDER BY
              CASE WHEN :sortBy = 'createdAt' AND :sortDir = 'asc' THEN stats."createdAt" END ASC,
              CASE WHEN :sortBy = 'createdAt' AND :sortDir = 'desc' THEN stats."createdAt" END DESC,
              CASE WHEN :sortBy = 'publishedAt' AND :sortDir = 'asc' THEN stats."publishedAt" END ASC NULLS LAST,
              CASE WHEN :sortBy = 'publishedAt' AND :sortDir = 'desc' THEN stats."publishedAt" END DESC NULLS LAST,
              CASE WHEN :sortBy = 'matchingGroupCount' AND :sortDir = 'asc' THEN stats."matchingGroupCount" END ASC,
              CASE WHEN :sortBy = 'matchingGroupCount' AND :sortDir = 'desc' THEN stats."matchingGroupCount" END DESC,
              CASE WHEN :sortBy = 'averageFillRate' AND :sortDir = 'asc' THEN stats."averageFillRate" END ASC,
              CASE WHEN :sortBy = 'averageFillRate' AND :sortDir = 'desc' THEN stats."averageFillRate" END DESC,
              stats."tourId"
            """,
            countQuery = """
            SELECT COUNT(*) FROM tour t
            WHERE t.vendor_id = :vendorId
              AND t.is_deleted = FALSE
              AND (CAST(:status AS varchar) IS NULL OR t.status = :status)
              AND (CAST(:keyword AS varchar) IS NULL
                   OR LOWER(t.tour_name) LIKE LOWER(CONCAT('%', CAST(:keyword AS varchar), '%'))
                   OR LOWER(t.location) LIKE LOWER(CONCAT('%', CAST(:keyword AS varchar), '%')))
            """,
            nativeQuery = true)
    Page<VendorTourStatisticProjection> getTourStatistics(
            @Param("vendorId") UUID vendorId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("sortBy") String sortBy,
            @Param("sortDir") String sortDir,
            Pageable pageable);
}
