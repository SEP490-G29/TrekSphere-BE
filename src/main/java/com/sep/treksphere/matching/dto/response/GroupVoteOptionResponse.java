package com.sep.treksphere.matching.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupVoteOptionResponse {

    private UUID groupVoteOptionId;
    private Integer optionOrder;
    private String optionLabel;
    private UUID candidateMemberId;
    private String candidateMemberName;
    private long ballotCount;
}
