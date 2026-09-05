package com.sep.treksphere.tour;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.tour.checkpoint.TourCheckpointResponse;
import com.sep.treksphere.tour.dto.response.PublicTourDetailResponse;
import com.sep.treksphere.tour.schedule.TourScheduleResponse;
import com.sep.treksphere.tour.dto.response.TourSummaryResponse;
import com.sep.treksphere.tour.DifficultyLevel;
import com.sep.treksphere.tour.checkpoint.TourCheckpointService;
import com.sep.treksphere.tour.schedule.TourScheduleService;
import com.sep.treksphere.tour.TourService;
import com.sep.treksphere.tour.recommendation.RecommendedTourResponse;
import com.sep.treksphere.tour.recommendation.TourRecommendationService;
import com.sep.treksphere.common.security.CustomUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tours")
@RequiredArgsConstructor
@Tag(name = "Tour", description = "Các API Tour công khai")
public class TourController {

    private final TourService tourService;
    private final TourCheckpointService tourCheckpointService;
    private final TourScheduleService tourScheduleService;
    private final TourRecommendationService tourRecommendationService;

    @Operation(summary = "Tour được đề xuất cho Trekker hiện tại")
    @GetMapping("/recommended")
    @PreAuthorize("hasRole('TREKKER')")
    public ResponseEntity<ApiResponse<PaginationResponse<RecommendedTourResponse>>> getRecommendedTours(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                tourRecommendationService.getRecommendations(
                        userDetails.getUser().getUserId(), page, size)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<TourSummaryResponse>>> getTours(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String location,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
            @RequestParam(required = false) UUID vendorId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PaginationResponse<TourSummaryResponse> result = tourService.getTours(
                keyword, location, difficulty, departureDate, returnDate, vendorId, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicTourDetailResponse>> getTourById(
            @Parameter(description = "UUID của tour", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, tourService.getTourById(id)));
    }

    @Operation(summary = "Xem danh sách trạm dừng", description = "Xem danh sách trạm dừng theo lộ trình của Tour")
    @GetMapping("/{tourId}/checkpoints")
    public ResponseEntity<ApiResponse<List<TourCheckpointResponse>>> getCheckpointsByTourId(
            @Parameter(description = "UUID của tour", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID tourId) {
        List<TourCheckpointResponse> result = tourCheckpointService.getCheckpointsByTourId(tourId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
    }

    @Operation(summary = "Xem lịch khởi hành sắp tới của một Tour", description = "Xem danh sách các lịch khởi hành sắp tới và còn hiệu lực của một Tour (public)")
    @GetMapping("/{tourId}/schedules")
    public ResponseEntity<ApiResponse<List<TourScheduleResponse>>> getUpcomingSchedules(
            @Parameter(description = "UUID của tour", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID tourId) {
        List<TourScheduleResponse> result = tourScheduleService.getUpcomingSchedules(tourId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
    }

}


