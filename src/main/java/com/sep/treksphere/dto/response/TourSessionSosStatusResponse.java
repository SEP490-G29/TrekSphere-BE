package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tour.SosAlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourSessionSosStatusResponse {

    private UUID tourSessionId;
    private boolean hasSosAlert;
    private boolean hasActiveSosAlert;
    private boolean resolved;
    private SosAlertStatus status;
    private SosAlertResponse sosAlert;
}
