package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.tour.TourSessionStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class CoordinatorScheduleFilterRequest extends BaseFilterRequest {

    private TourSessionStatus status;

    private Boolean isCancelled;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDateTo;
}
