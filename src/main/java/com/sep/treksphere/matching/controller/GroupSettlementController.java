package com.sep.treksphere.matching.controller;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.GroupSettlementProofRequest;
import com.sep.treksphere.matching.dto.request.GroupSettlementRejectRequest;
import com.sep.treksphere.matching.dto.response.GroupSettlementResponse;
import com.sep.treksphere.matching.dto.response.GroupSettlementSummaryResponse;
import com.sep.treksphere.matching.service.GroupSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matching-groups/{groupId}/settlements")
@RequiredArgsConstructor
@Tag(name = "Group Settlement", description = "APIs for group debt settlement, greedy netting suggestions, and settlement verification")
public class GroupSettlementController {

    private final GroupSettlementService groupSettlementService;

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get settlement summary and greedy netting suggestions for group")
    public ResponseEntity<ApiResponse<GroupSettlementSummaryResponse>> getSettlementSummary(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        GroupSettlementSummaryResponse response = groupSettlementService.getSettlementSummary(groupId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.SETTLEMENT_SUMMARY_FETCHED_SUCCESS));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get list of settlement transactions for group")
    public ResponseEntity<ApiResponse<List<GroupSettlementResponse>>> getSettlements(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<GroupSettlementResponse> response = groupSettlementService.getSettlements(groupId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.SETTLEMENTS_FETCHED_SUCCESS));
    }

    @PostMapping("/generate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Leader generates settlement transactions based on greedy netting algorithm")
    public ResponseEntity<ApiResponse<List<GroupSettlementResponse>>> generateSettlements(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<GroupSettlementResponse> response = groupSettlementService.generateSettlements(groupId, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.SETTLEMENTS_GENERATED_SUCCESS));
    }

    @PostMapping("/{settlementId}/submit-proof")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Debtor submits payment proof for settlement transaction")
    public ResponseEntity<ApiResponse<GroupSettlementResponse>> submitProof(
            @PathVariable UUID groupId,
            @PathVariable UUID settlementId,
            @Valid @RequestBody GroupSettlementProofRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        GroupSettlementResponse response = groupSettlementService.submitProof(groupId, settlementId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.SETTLEMENT_PROOF_SUBMITTED_SUCCESS));
    }

    @PostMapping("/{settlementId}/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Payee confirms receiving payment for settlement transaction")
    public ResponseEntity<ApiResponse<GroupSettlementResponse>> confirmSettlement(
            @PathVariable UUID groupId,
            @PathVariable UUID settlementId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        GroupSettlementResponse response = groupSettlementService.confirmSettlement(groupId, settlementId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.SETTLEMENT_CONFIRMED_SUCCESS));
    }

    @PostMapping("/{settlementId}/reject")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Payee rejects settlement transaction with reason")
    public ResponseEntity<ApiResponse<GroupSettlementResponse>> rejectSettlement(
            @PathVariable UUID groupId,
            @PathVariable UUID settlementId,
            @Valid @RequestBody GroupSettlementRejectRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        GroupSettlementResponse response = groupSettlementService.rejectSettlement(groupId, settlementId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.SETTLEMENT_REJECTED_SUCCESS));
    }
}
