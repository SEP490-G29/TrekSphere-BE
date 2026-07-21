package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.CreateScheduleRequest;
import com.sep.treksphere.dto.request.UpdateScheduleRequest;
import com.sep.treksphere.dto.response.TourScheduleResponse;

import java.util.List;
import java.util.UUID;

public interface TourScheduleService {
    List<TourScheduleResponse> getUpcomingSchedules(UUID tourId);
    TourScheduleResponse createSchedule(String userEmail, UUID tourId, CreateScheduleRequest request);
    TourScheduleResponse updateSchedule(String userEmail, UUID scheduleId, UpdateScheduleRequest request);
    void deleteSchedule(String userEmail, UUID scheduleId);
}

