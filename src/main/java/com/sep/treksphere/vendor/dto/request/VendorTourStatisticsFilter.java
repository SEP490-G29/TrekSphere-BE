package com.sep.treksphere.vendor.dto.request;

import com.sep.treksphere.tour.enums.TourStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorTourStatisticsFilter {
    private String keyword;
    private TourStatus status;
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}
