package com.sep.treksphere.vendor.statistics;

import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.vendor.VendorAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** Bao phủ method duy nhất của `VendorTourStatisticsService`: `getStatistics`. */
@ExtendWith(MockitoExtension.class)
class VendorTourStatisticsServiceTest {

    @Mock private VendorTourStatisticsRepository statisticsRepository;
    @Mock private VendorAccessService vendorAccessService;

    @InjectMocks
    private VendorTourStatisticsService statisticsService;

    private static final String MANAGER_EMAIL = "manager@example.com";

    private Vendor vendor;

    @BeforeEach
    void setUp() {
        vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());
    }

    private VendorStatisticsOverviewProjection overviewProjection() {
        return new VendorStatisticsOverviewProjection() {
            public Long getTotalTours() { return 10L; }
            public Long getDraftTours() { return 2L; }
            public Long getPublishedTours() { return 7L; }
            public Long getHiddenTours() { return 1L; }
            public Long getFutureOpenSchedules() { return 4L; }
            public Long getMatchingGroupCount() { return 3L; }
            public Double getAverageFillRate() { return 62.34; }
            public Double getFullGroupRate() { return null; }
        };
    }

    private VendorTourStatisticProjection tourProjection() {
        UUID tourId = UUID.randomUUID();
        return new VendorTourStatisticProjection() {
            public UUID getTourId() { return tourId; }
            public String getTourName() { return "Fansipan Trek"; }
            public String getStatus() { return "PUBLISHED"; }
            public LocalDateTime getCreatedAt() { return LocalDateTime.now(); }
            public LocalDateTime getPublishedAt() { return LocalDateTime.now(); }
            public Long getOpenScheduleCount() { return 2L; }
            public Long getClosedScheduleCount() { return 1L; }
            public Long getCancelledScheduleCount() { return null; }
            public Long getMatchingGroupCount() { return 3L; }
            public Double getAverageFillRate() { return 55.0; }
            public Double getFullGroupRate() { return null; }
        };
    }

    @Test
    @DisplayName("getStatistics: trả về overview + danh sách tour đã phân trang, làm tròn tỉ lệ và null -> 0")
    void getStatistics_Success() {
        VendorTourStatisticsFilter filter = new VendorTourStatisticsFilter();
        when(vendorAccessService.resolveByManagerEmail(MANAGER_EMAIL)).thenReturn(vendor);
        when(statisticsRepository.getOverview(vendor.getVendorId())).thenReturn(overviewProjection());
        Page<VendorTourStatisticProjection> page = new PageImpl<>(List.of(tourProjection()));
        when(statisticsRepository.getTourStatistics(eq(vendor.getVendorId()), any(), any(), any(), any(), any()))
                .thenReturn(page);

        VendorTourStatisticsResponse response = statisticsService.getStatistics(MANAGER_EMAIL, filter);

        assertThat(response.getOverview().getTotalTours()).isEqualTo(10);
        assertThat(response.getOverview().getAverageFillRate()).isEqualTo(62.3);
        assertThat(response.getOverview().getFullGroupRate()).isEqualTo(0.0);
        assertThat(response.getTours().getContent()).hasSize(1);
        assertThat(response.getTours().getContent().get(0).getTourName()).isEqualTo("Fansipan Trek");
        assertThat(response.getTours().getContent().get(0).getCancelledScheduleCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("getStatistics: sortBy không hợp lệ -> tự động dùng createdAt")
    void getStatistics_InvalidSortField_FallsBackToCreatedAt() {
        VendorTourStatisticsFilter filter = new VendorTourStatisticsFilter();
        filter.setSortBy("not-a-real-field");
        when(vendorAccessService.resolveByManagerEmail(MANAGER_EMAIL)).thenReturn(vendor);
        when(statisticsRepository.getOverview(vendor.getVendorId())).thenReturn(overviewProjection());
        when(statisticsRepository.getTourStatistics(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        statisticsService.getStatistics(MANAGER_EMAIL, filter);

        org.mockito.Mockito.verify(statisticsRepository).getTourStatistics(
                eq(vendor.getVendorId()), any(), any(), eq("createdAt"), eq("desc"), any());
    }
}
