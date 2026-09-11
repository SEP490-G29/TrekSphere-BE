package com.sep.treksphere.matching.service;

import com.sep.treksphere.matching.dto.request.GroupSettlementProofRequest;
import com.sep.treksphere.matching.dto.request.GroupSettlementRejectRequest;
import com.sep.treksphere.matching.dto.response.GroupSettlementResponse;
import com.sep.treksphere.matching.dto.response.GroupSettlementSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface GroupSettlementService {

    GroupSettlementSummaryResponse getSettlementSummary(UUID groupId, String userEmail);

    List<GroupSettlementResponse> getSettlements(UUID groupId, String userEmail);

    List<GroupSettlementResponse> generateSettlements(UUID groupId, String userEmail);

    GroupSettlementResponse submitProof(UUID groupId, UUID settlementId, GroupSettlementProofRequest request, String userEmail);

    GroupSettlementResponse confirmSettlement(UUID groupId, UUID settlementId, String userEmail);

    GroupSettlementResponse rejectSettlement(UUID groupId, UUID settlementId, GroupSettlementRejectRequest request, String userEmail);
}
