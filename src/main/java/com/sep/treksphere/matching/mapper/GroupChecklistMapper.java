package com.sep.treksphere.matching.mapper;

import com.sep.treksphere.matching.dto.request.GroupChecklistItemCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupChecklistItemUpdateRequest;
import com.sep.treksphere.matching.dto.response.GroupChecklistItemResponse;
import com.sep.treksphere.matching.entity.GroupChecklistItem;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupChecklistMapper {

    @Mapping(target = "matchingGroupId", source = "matchingGroup.matchingGroupId")
    @Mapping(target = "assigneeMatchingMemberId", source = "assigneeMatchingMember.matchingMemberId")
    @Mapping(target = "assigneeUserId", source = "assigneeMatchingMember.user.userId")
    @Mapping(target = "assigneeFullName", source = "assigneeMatchingMember.user.fullName")
    @Mapping(target = "assigneeAvatarUrl", source = "assigneeMatchingMember.user.avatarUrl")
    @Mapping(target = "completedByMatchingMemberId", source = "completedBy.matchingMemberId")
    @Mapping(target = "completedByUserId", source = "completedBy.user.userId")
    @Mapping(target = "completedByFullName", source = "completedBy.user.fullName")
    GroupChecklistItemResponse toResponse(GroupChecklistItem entity);

    List<GroupChecklistItemResponse> toResponseList(List<GroupChecklistItem> list);

    @Mapping(target = "groupChecklistItemId", ignore = true)
    @Mapping(target = "matchingGroup", ignore = true)
    @Mapping(target = "assigneeMatchingMember", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "completedBy", ignore = true)
    GroupChecklistItem toEntity(GroupChecklistItemCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "groupChecklistItemId", ignore = true)
    @Mapping(target = "matchingGroup", ignore = true)
    @Mapping(target = "assigneeMatchingMember", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "completedBy", ignore = true)
    void updateEntityFromRequest(GroupChecklistItemUpdateRequest request, @MappingTarget GroupChecklistItem entity);
}
