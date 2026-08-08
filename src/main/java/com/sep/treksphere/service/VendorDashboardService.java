package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.VendorDashboardFilterRequest;
import com.sep.treksphere.dto.response.dashboard.*;

import java.util.List;
import java.util.UUID;

public interface VendorDashboardService {

    VendorDashboardOverviewResponse getOverview(String email, VendorDashboardFilterRequest request);

    RevenueChartResponse getRevenueChart(String email, VendorDashboardFilterRequest request);

    List<TopSellingTourResponse> getTopTours(String email, VendorDashboardFilterRequest request, int limit);

    List<UpcomingScheduleResponse> getUpcomingSchedules(String email, int limit, Integer daysAhead);

    List<UnderCapacityAlertResponse> getUnderCapacityAlerts(String email, Integer alertDaysThreshold);

    ScheduleManifestResponse getScheduleManifest(String email, UUID scheduleId);
}
