package com.sep.treksphere.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopSellingTourResponse {

    private UUID tourId;
    private String tourName;
    private Long totalTravelers;
    private BigDecimal totalRevenue;
}
