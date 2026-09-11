package com.sep.treksphere.matching.mapper;

import com.sep.treksphere.matching.dto.request.GroupExpenseCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupExpenseUpdateRequest;
import com.sep.treksphere.matching.dto.response.GroupExpenseResponse;
import com.sep.treksphere.matching.dto.response.GroupExpenseShareResponse;
import com.sep.treksphere.matching.dto.response.GroupMemberSummaryResponse;
import com.sep.treksphere.matching.entity.GroupExpense;
import com.sep.treksphere.matching.entity.GroupExpenseShare;
import com.sep.treksphere.matching.entity.MatchingMember;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GroupExpenseMapper {

    @Mapping(target = "groupTripId", source = "groupTrip.groupTripId")
    @Mapping(target = "payer", expression = "java(toMemberSummary(expense.getPaidBy()))")
    @Mapping(target = "shares", expression = "java(mapShares(expense.getShares()))")
    GroupExpenseResponse toResponse(GroupExpense expense);

    @Mapping(target = "member", expression = "java(toMemberSummary(share.getMatchingMember()))")
    GroupExpenseShareResponse toShareResponse(GroupExpenseShare share);

    @Mapping(target = "matchingMemberId", source = "matchingMemberId")
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")
    @Mapping(target = "role", source = "role")
    GroupMemberSummaryResponse toMemberSummary(MatchingMember member);

    @Mapping(target = "groupExpenseId", ignore = true)
    @Mapping(target = "groupTrip", ignore = true)
    @Mapping(target = "paidBy", ignore = true)
    @Mapping(target = "beneficiaryCount", ignore = true)
    @Mapping(target = "shares", ignore = true)
    GroupExpense toEntity(GroupExpenseCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "groupExpenseId", ignore = true)
    @Mapping(target = "groupTrip", ignore = true)
    @Mapping(target = "paidBy", ignore = true)
    @Mapping(target = "beneficiaryCount", ignore = true)
    @Mapping(target = "shares", ignore = true)
    void updateEntityFromRequest(GroupExpenseUpdateRequest request, @MappingTarget GroupExpense expense);

    default List<GroupExpenseShareResponse> mapShares(List<GroupExpenseShare> shares) {
        if (shares == null || shares.isEmpty()) {
            return Collections.emptyList();
        }
        return shares.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .map(this::toShareResponse)
                .toList();
    }
}
