package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.request.CreateTourRequest;
import com.sep.treksphere.dto.request.UpdateTourRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.TourDetailResponse;
import com.sep.treksphere.dto.response.TourSummaryResponse;
import com.sep.treksphere.enums.tour.DifficultyLevel;

import java.util.UUID;

public interface TourService {
    PaginationResponse<TourSummaryResponse> getTours(
            String keyword,
            String location,
            DifficultyLevel difficulty,
            int page,
            int size,
            String sortBy,
            String sortDir
    );
    TourDetailResponse getTourById(UUID tourId);

    // Vendor Tour Management
    PaginationResponse<TourSummaryResponse> getVendorTours(String userEmail, BaseFilterRequest request);
    TourDetailResponse getVendorTourById(String userEmail, UUID tourId);
    TourDetailResponse createTour(String userEmail, CreateTourRequest request);
    TourDetailResponse updateTour(String userEmail, UUID tourId, UpdateTourRequest request);
    void deleteTour(String userEmail, UUID tourId);

    // Tour Approval Workflow
    TourDetailResponse submitTourForApproval(String userEmail, UUID tourId);

    // Tour Moderation
    TourDetailResponse hideTourForViolation(String userEmail, UUID tourId, String reason);
}

