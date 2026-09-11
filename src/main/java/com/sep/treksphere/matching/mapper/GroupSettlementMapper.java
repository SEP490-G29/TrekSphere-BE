package com.sep.treksphere.matching.mapper;

import com.sep.treksphere.matching.dto.response.GroupMemberSummaryResponse;
import com.sep.treksphere.matching.dto.response.GroupSettlementResponse;
import com.sep.treksphere.matching.entity.GroupSettlement;
import com.sep.treksphere.matching.entity.MatchingMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GroupSettlementMapper {

    @Mapping(target = "groupTripId", source = "groupTrip.groupTripId")
    @Mapping(target = "fromMember", expression = "java(toMemberSummary(settlement.getFromMatchingMember()))")
    @Mapping(target = "toMember", expression = "java(toMemberSummary(settlement.getToMatchingMember()))")
    @Mapping(target = "confirmedBy", expression = "java(toMemberSummary(settlement.getConfirmedBy()))")
    GroupSettlementResponse toResponse(GroupSettlement settlement);

    List<GroupSettlementResponse> toResponseList(List<GroupSettlement> settlements);

    default GroupMemberSummaryResponse toMemberSummary(MatchingMember member) {
        if (member == null) {
            return null;
        }
        return GroupMemberSummaryResponse.builder()
                .matchingMemberId(member.getMatchingMemberId())
                .userId(member.getUser() != null ? member.getUser().getUserId() : null)
                .fullName(member.getUser() != null ? member.getUser().getFullName() : null)
                .avatarUrl(member.getUser() != null ? member.getUser().getAvatarUrl() : null)
                .role(member.getRole())
                .build();
    }
}
