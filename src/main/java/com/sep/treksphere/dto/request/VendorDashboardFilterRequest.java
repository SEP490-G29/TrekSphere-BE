package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.dashboard.GroupBy;
import com.sep.treksphere.enums.dashboard.TimeRange;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class VendorDashboardFilterRequest {

    private TimeRange timeRange = TimeRange.LAST_30_DAYS;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private GroupBy groupBy = GroupBy.DAY;
}
