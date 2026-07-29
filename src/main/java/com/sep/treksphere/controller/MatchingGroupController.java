package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.dto.response.MatchingGroupResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.MatchingGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matching-groups")
@RequiredArgsConstructor
@Tag(name = "Matching Group", description = "Các API liên quan đến ghép nhóm đồng hành")
public class MatchingGroupController {

    private final MatchingGroupService matchingGroupService;

    @Operation(
        summary = "Tìm kiếm các nhóm ghép bạn đồng hành",
        description = "Lấy danh sách các nhóm ghép bạn đồng hành đang mở (OPEN) có phân trang. Cho phép lọc theo tour và ngày khởi hành dự kiến."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<MatchingGroupResponse>>> getMatchingGroups(
            @Valid @ParameterObject @ModelAttribute MatchingGroupFilterRequest filter) {
        PaginationResponse<MatchingGroupResponse> result = matchingGroupService.getMatchingGroups(filter);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_GROUPS_FETCHED_SUCCESS));
    }

    @Operation(
        summary = "Xem chi tiết nhóm ghép bạn đồng hành",
        description = "Lấy thông tin chi tiết của nhóm ghép bao gồm danh sách thành viên đã được duyệt (ACCEPTED)."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MatchingGroupDetailResponse>> getMatchingGroupById(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID id) {
        MatchingGroupDetailResponse result = matchingGroupService.getMatchingGroupById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_GROUP_FETCHED_SUCCESS));
    }

    @Operation(
        summary = "Tạo một nhóm ghép bạn đồng hành mới",
        description = "Cho phép Trekker tạo nhóm ghép bạn đồng hành mới cho một Tour và ngày đi mong muốn."
    )
    @PostMapping
    @PreAuthorize("hasRole('TREKKER')")
    public ResponseEntity<ApiResponse<MatchingGroupDetailResponse>> createMatchingGroup(
            @Valid @RequestBody MatchingGroupCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingGroupDetailResponse result = matchingGroupService.createMatchingGroup(request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, result, MessageConstant.MATCHING_GROUP_CREATED_SUCCESS));
    }
}

