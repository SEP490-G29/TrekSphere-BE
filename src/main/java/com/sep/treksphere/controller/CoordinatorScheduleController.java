package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.CoordinatorScheduleFilterRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.CoordinatorScheduleResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.CoordinatorScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coordinator/schedules")
@RequiredArgsConstructor
@Tag(name = "Coordinator Schedule Management", description = "Các API liên quan đến lịch trình dẫn đoàn của Hướng dẫn viên")
public class CoordinatorScheduleController {

    private final CoordinatorScheduleService coordinatorScheduleService;

    @GetMapping
    @Operation(summary = "Xem lịch dẫn đoàn được phân công", description = "Lấy danh sách các phiên đi tour được phân công cho Coordinator.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<ApiResponse<PaginationResponse<CoordinatorScheduleResponse>>> getMySchedules(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject CoordinatorScheduleFilterRequest request
    ) {
        PaginationResponse<CoordinatorScheduleResponse> data = coordinatorScheduleService.getMySchedules(
                userDetails.getUsername(),
                request
        );

        ApiResponse<PaginationResponse<CoordinatorScheduleResponse>> response = ApiResponse.success(
                HttpStatus.OK,
                data,
                MessageConstant.COORDINATOR_SCHEDULE_FETCHED
        );

        return ResponseEntity.ok(response);
    }
}
