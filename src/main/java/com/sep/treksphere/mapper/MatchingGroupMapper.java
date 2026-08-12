package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.dto.response.MatchingGroupResponse;
import com.sep.treksphere.dto.response.MatchingMemberResponse;
import com.sep.treksphere.dto.response.MyMatchingJoinRequestResponse;
import com.sep.treksphere.entity.MatchingGroup;
import com.sep.treksphere.entity.MatchingMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MatchingGroupMapper {

    @Mapping(target = "tourId", source = "tour.tourId")
    @Mapping(target = "tourName", source = "tour.tourName")
    @Mapping(target = "ownerId", source = "owner.userId")
    @Mapping(target = "ownerName", source = "owner.fullName")
    @Mapping(target = "ownerAvatarUrl", source = "owner.avatarUrl")
    MatchingGroupResponse toResponse(MatchingGroup matchingGroup);

    @Mapping(target = "tourId", source = "tour.tourId")
    @Mapping(target = "tourName", source = "tour.tourName")
    @Mapping(target = "ownerId", source = "owner.userId")
    @Mapping(target = "ownerName", source = "owner.fullName")
    @Mapping(target = "ownerAvatarUrl", source = "owner.avatarUrl")
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "isOwner", ignore = true)
    @Mapping(target = "myMembershipStatus", ignore = true)
    @Mapping(target = "canJoin", ignore = true)
    @Mapping(target = "canLeave", ignore = true)
    @Mapping(target = "hasConversation", expression = "java(matchingGroup.getConversation() != null)")
    MatchingGroupDetailResponse toDetailResponse(MatchingGroup matchingGroup);

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")
    MatchingMemberResponse toMemberResponse(MatchingMember matchingMember);

    @Mapping(target = "matchingGroupId", source = "matchingGroup.matchingGroupId")
    @Mapping(target = "groupName", source = "matchingGroup.groupName")
    @Mapping(target = "groupStatus", source = "matchingGroup.status")
    @Mapping(target = "tourId", source = "matchingGroup.tour.tourId")
    @Mapping(target = "tourName", source = "matchingGroup.tour.tourName")
    @Mapping(target = "ownerId", source = "matchingGroup.owner.userId")
    @Mapping(target = "ownerName", source = "matchingGroup.owner.fullName")
    @Mapping(target = "ownerAvatarUrl", source = "matchingGroup.owner.avatarUrl")
    @Mapping(target = "currentSize", source = "matchingGroup.currentSize")
    @Mapping(target = "maxSize", source = "matchingGroup.maxSize")
    @Mapping(target = "targetDate", source = "matchingGroup.targetDate")
    @Mapping(target = "matchingDeadline", source = "matchingGroup.matchingDeadline")
    @Mapping(target = "canCancel", ignore = true)
    MyMatchingJoinRequestResponse toMyJoinRequestResponse(MatchingMember matchingMember);
}
