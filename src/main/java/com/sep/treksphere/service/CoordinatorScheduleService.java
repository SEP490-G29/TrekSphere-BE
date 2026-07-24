package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.CoordinatorScheduleFilterRequest;
import com.sep.treksphere.dto.response.CoordinatorScheduleResponse;
import com.sep.treksphere.dto.response.PaginationResponse;

public interface CoordinatorScheduleService {

    PaginationResponse<CoordinatorScheduleResponse> getMySchedules(
            String email,
            CoordinatorScheduleFilterRequest request
    );
}
