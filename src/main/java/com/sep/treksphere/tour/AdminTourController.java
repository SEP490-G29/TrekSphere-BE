package com.sep.treksphere.tour;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.tour.dto.request.HideTourRequest;
import com.sep.treksphere.tour.dto.response.TourDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tours")
@RequiredArgsConstructor
@Tag(name = "Admin Tour Moderation", description = "Ẩn và mở lại Tour bởi Admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('TOUR_HIDE_UNHIDE')")
public class AdminTourController {

    private final TourService tourService;

    @Operation(summary = "Ẩn Tour vi phạm")
    @PutMapping("/{id}/hide")
    public ResponseEntity<ApiResponse<TourDetailResponse>> hideTour(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody HideTourRequest request) {
        TourDetailResponse response = tourService.hideTourForViolation(
                userDetails.getUser().getUserId(), id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, response, MessageConstant.TOUR_HIDDEN_SUCCESSFULLY));
    }

    @Operation(summary = "Mở lại Tour đã bị ẩn")
    @PutMapping("/{id}/unhide")
    public ResponseEntity<ApiResponse<TourDetailResponse>> unhideTour(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        TourDetailResponse response = tourService.unhideTour(userDetails.getUser().getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, response, MessageConstant.TOUR_UNHIDDEN_SUCCESSFULLY));
    }
}
