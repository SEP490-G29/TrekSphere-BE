package com.sep.treksphere.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorDashboardOverviewResponse {

    private BigDecimal totalRevenue;
    private Double revenueChangePercentage;

    private Long totalTravelers;
    private Double travelersChangePercentage;

    private Double avgOccupancyRate;
    private Double occupancyRateChangePercentage;

    private Double cancellationRate;
    private Double cancellationRateChangePercentage;
}
