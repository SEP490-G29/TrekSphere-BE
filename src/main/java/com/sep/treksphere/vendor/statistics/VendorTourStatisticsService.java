package com.sep.treksphere.vendor.statistics;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.tour.TourStatus;
import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.vendor.VendorAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VendorTourStatisticsService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "createdAt", "publishedAt", "matchingGroupCount", "averageFillRate");

    private final VendorTourStatisticsRepository statisticsRepository;
    private final VendorAccessService vendorAccessService;

    @Transactional(readOnly = true)
    public VendorTourStatisticsResponse getStatistics(String email, VendorTourStatisticsFilter filter) {
        Vendor vendor = vendorAccessService.resolveByManagerEmail(email);
        String sortBy = SORT_FIELDS.contains(filter.getSortBy()) ? filter.getSortBy() : "createdAt";
        String sortDir = "asc".equalsIgnoreCase(filter.getSortDir()) ? "asc" : "desc";
        int pageNumber = Math.max(filter.getPage(), 0);
        int pageSize = Math.min(Math.max(filter.getSize(), 1), 100);
        String keyword = StringUtils.hasText(filter.getKeyword()) ? filter.getKeyword().trim() : null;
        String status = filter.getStatus() == null ? null : filter.getStatus().name();

        VendorStatisticsOverviewProjection overview = statisticsRepository.getOverview(vendor.getVendorId());
        Page<VendorTourStatisticProjection> page = statisticsRepository.getTourStatistics(
                vendor.getVendorId(), keyword, status, sortBy, sortDir,
                PageRequest.of(pageNumber, pageSize));

        PaginationResponse<VendorTourStatisticItem> tours = PaginationResponse.<VendorTourStatisticItem>builder()
                .content(page.getContent().stream().map(this::toItem).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
        return VendorTourStatisticsResponse.builder()
                .overview(toOverview(overview))
                .tours(tours)
                .build();
    }

    private VendorStatisticsOverview toOverview(VendorStatisticsOverviewProjection value) {
        return VendorStatisticsOverview.builder()
                .totalTours(orZero(value.getTotalTours()))
                .draftTours(orZero(value.getDraftTours()))
                .publishedTours(orZero(value.getPublishedTours()))
                .hiddenTours(orZero(value.getHiddenTours()))
                .futureOpenSchedules(orZero(value.getFutureOpenSchedules()))
                .matchingGroupCount(orZero(value.getMatchingGroupCount()))
                .averageFillRate(round(value.getAverageFillRate()))
                .fullGroupRate(round(value.getFullGroupRate()))
                .build();
    }

    private VendorTourStatisticItem toItem(VendorTourStatisticProjection value) {
        return VendorTourStatisticItem.builder()
                .tourId(value.getTourId())
                .tourName(value.getTourName())
                .status(TourStatus.valueOf(value.getStatus()))
                .createdAt(value.getCreatedAt())
                .publishedAt(value.getPublishedAt())
                .openScheduleCount(orZero(value.getOpenScheduleCount()))
                .closedScheduleCount(orZero(value.getClosedScheduleCount()))
                .cancelledScheduleCount(orZero(value.getCancelledScheduleCount()))
                .matchingGroupCount(orZero(value.getMatchingGroupCount()))
                .averageFillRate(round(value.getAverageFillRate()))
                .fullGroupRate(round(value.getFullGroupRate()))
                .build();
    }

    private long orZero(Long value) {
        return value == null ? 0 : value;
    }

    private double round(Double value) {
        return BigDecimal.valueOf(value == null ? 0 : value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
