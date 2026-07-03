package com.sep.treksphere.controller;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.TourDetailResponse;
import com.sep.treksphere.dto.response.TourSummaryResponse;
import com.sep.treksphere.enums.tour.DifficultyLevel;
import com.sep.treksphere.service.TourService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tours")
@RequiredArgsConstructor
@Tag(name = "Tour", description = "Các API dành cho Tour (public - không cần đăng nhập)")
public class TourController {

    private final TourService tourService;

    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<TourSummaryResponse>>> getTours(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String location,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PaginationResponse<TourSummaryResponse> result = tourService.getTours(
                keyword, location, difficulty, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TourDetailResponse>> getTourById(
            @Parameter(description = "UUID của tour", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, tourService.getTourById(id)));
    }
}
