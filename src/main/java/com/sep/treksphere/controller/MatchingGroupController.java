package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.MatchingGroupResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.service.MatchingGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
