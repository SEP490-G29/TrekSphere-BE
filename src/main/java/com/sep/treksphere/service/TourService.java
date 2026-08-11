package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.request.CreateTourRequest;
import com.sep.treksphere.dto.request.UpdateTourRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.TourDetailResponse;
import com.sep.treksphere.dto.response.TourSummaryResponse;
import com.sep.treksphere.enums.tour.DifficultyLevel;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TourService {
    PaginationResponse<TourSummaryResponse> getTours(
            String keyword,
            String location,
            DifficultyLevel difficulty,
            LocalDate departureDate,
            LocalDate returnDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    );
    TourDetailResponse getTourById(UUID tourId);

    // Vendor Tour Management
    PaginationResponse<TourSummaryResponse> getVendorTours(String userEmail, BaseFilterRequest request);
    TourDetailResponse getVendorTourById(String userEmail, UUID tourId);
    TourDetailResponse createTour(String userEmail, CreateTourRequest request, MultipartFile coverImage, List<MultipartFile> tourImages);
    TourDetailResponse updateTour(String userEmail, UUID tourId, UpdateTourRequest request, MultipartFile coverImage, List<MultipartFile> tourImages);
    void deleteTour(String userEmail, UUID tourId);

    // Tour Approval Workflow
    TourDetailResponse submitTourForApproval(String userEmail, UUID tourId);
    TourDetailResponse approveTour(String userEmail, UUID tourId);
    TourDetailResponse rejectTour(String userEmail, UUID tourId, String reason);

    // Revert REJECTED → DRAFT (Staff) hoặc REJECTED → PENDING_APPROVAL (Manager)
    TourDetailResponse revertTour(String userEmail, UUID tourId);

    // Khôi phục Tour đã xóa mềm
    TourDetailResponse restoreTour(String userEmail, UUID tourId);

    // Tour Moderation
    TourDetailResponse hideTourForViolation(String userEmail, UUID tourId, String reason);
    TourDetailResponse unhideTour(String userEmail, UUID tourId);
}

