package com.sep.treksphere.matching.controller;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.GroupExpenseCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupExpenseFilterRequest;
import com.sep.treksphere.matching.dto.request.GroupExpenseUpdateRequest;
import com.sep.treksphere.matching.dto.response.GroupExpenseResponse;
import com.sep.treksphere.matching.dto.response.GroupExpenseSummaryResponse;
import com.sep.treksphere.matching.service.GroupExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matching-groups/{groupId}/expenses")
@RequiredArgsConstructor
@Tag(name = "Group Expense", description = "APIs for managing group trip expenses and shares")
public class GroupExpenseController {

    private final GroupExpenseService groupExpenseService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create an expense for group trip")
    public ResponseEntity<ApiResponse<GroupExpenseResponse>> createExpense(
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupExpenseCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        GroupExpenseResponse response = groupExpenseService.createExpense(groupId, request, userDetails.getUser().getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.GROUP_EXPENSE_CREATED_SUCCESS));
    }

    @PutMapping("/{expenseId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update an expense for group trip")
    public ResponseEntity<ApiResponse<GroupExpenseResponse>> updateExpense(
            @PathVariable UUID groupId,
            @PathVariable UUID expenseId,
            @Valid @RequestBody GroupExpenseUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        GroupExpenseResponse response = groupExpenseService.updateExpense(groupId, expenseId, request, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.GROUP_EXPENSE_UPDATED_SUCCESS));
    }

    @DeleteMapping("/{expenseId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Void/Soft-delete an expense")
    public ResponseEntity<ApiResponse<Void>> voidExpense(
            @PathVariable UUID groupId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        groupExpenseService.voidExpense(groupId, expenseId, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, MessageConstant.GROUP_EXPENSE_DELETED_SUCCESS));
    }

    @GetMapping("/{expenseId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get expense detail")
    public ResponseEntity<ApiResponse<GroupExpenseResponse>> getExpenseDetail(
            @PathVariable UUID groupId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        GroupExpenseResponse response = groupExpenseService.getExpenseDetail(groupId, expenseId, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.GROUP_EXPENSE_FETCHED_SUCCESS));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get paginated list of group expenses")
    public ResponseEntity<ApiResponse<PaginationResponse<GroupExpenseResponse>>> getGroupExpenses(
            @PathVariable UUID groupId,
            @Valid @ParameterObject @ModelAttribute GroupExpenseFilterRequest filter,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PaginationResponse<GroupExpenseResponse> response = groupExpenseService.getGroupExpenses(groupId, filter, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.GROUP_EXPENSES_FETCHED_SUCCESS));
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get expense summary of the group")
    public ResponseEntity<ApiResponse<GroupExpenseSummaryResponse>> getExpenseSummary(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        GroupExpenseSummaryResponse response = groupExpenseService.getExpenseSummary(groupId, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.GROUP_EXPENSE_SUMMARY_FETCHED_SUCCESS));
    }
}
