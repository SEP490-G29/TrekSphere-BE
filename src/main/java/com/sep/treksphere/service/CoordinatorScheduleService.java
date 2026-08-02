package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.CoordinatorScheduleFilterRequest;
import com.sep.treksphere.dto.response.CoordinatorScheduleResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.request.LogisticsInfoFilterRequest;
import com.sep.treksphere.dto.response.LogisticsPassengerResponse;
import java.util.UUID;

public interface CoordinatorScheduleService {

    PaginationResponse<CoordinatorScheduleResponse> getMySchedules(
            String email,
            CoordinatorScheduleFilterRequest request
    );

    PaginationResponse<LogisticsPassengerResponse> getLogisticsInfo(
            String email,
            UUID tourSessionId,
            LogisticsInfoFilterRequest request
    );
}
