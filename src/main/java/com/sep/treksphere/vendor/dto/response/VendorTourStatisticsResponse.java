package com.sep.treksphere.vendor.dto.response;

import com.sep.treksphere.common.dto.PaginationResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VendorTourStatisticsResponse {
    private VendorStatisticsOverview overview;
    private PaginationResponse<VendorTourStatisticItem> tours;
}
