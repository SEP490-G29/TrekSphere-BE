package com.sep.treksphere.dto.response.dashboard;

import com.sep.treksphere.enums.dashboard.GroupBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueChartResponse {

    private GroupBy groupBy;
    private List<RevenueChartPointResponse> chartData;
}
