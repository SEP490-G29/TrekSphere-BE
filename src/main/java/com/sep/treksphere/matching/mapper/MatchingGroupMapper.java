package com.sep.treksphere.matching.mapper;

import com.sep.treksphere.blog.service.BlogService;
import com.sep.treksphere.matching.dto.request.CustomJourneyCreateRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.matching.dto.response.*;
import com.sep.treksphere.matching.entity.*;
import com.sep.treksphere.matching.enums.JoinApplicationStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupSourceType;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.user.enums.UserStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MatchingGroupMapper {

    @Mapping(target = "tour", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "currentSize", constant = "1")
    @Mapping(target = "status", constant = "OPEN")
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "customJourney", ignore = true)
    @Mapping(target = "conversation", ignore = true)
    MatchingGroup toEntity(MatchingGroupCreateRequest request);

    @Mapping(target = "matchingGroup", ignore = true)
    @Mapping(target = "isLocked", constant = "false")
    @Mapping(target = "lockedAt", ignore = true)
    @Mapping(target = "checkpoints", ignore = true)
    @Mapping(target = "costItems", ignore = true)
    CustomJourney toEntity(CustomJourneyCreateRequest request);

    @Mapping(target = "sourceType", expression = "java(deriveSourceType(matchingGroup))")
    @Mapping(target = "tourId", source = "tour.tourId")
    @Mapping(target = "tourName", source = "tour.tourName")
    @Mapping(target = "customJourneyId", source = "customJourney.customJourneyId")
    @Mapping(target = "customJourneyTitle", source = "customJourney.title")
    @Mapping(target = "difficulty", expression = "java(deriveDifficulty(matchingGroup))")
    @Mapping(target = "location", expression = "java(deriveLocation(matchingGroup))")
    @Mapping(target = "estimatedCost", expression = "java(deriveEstimatedCost(matchingGroup))")
    @Mapping(target = "coverImageUrl", expression = "java(deriveCoverImageUrl(matchingGroup))")
    @Mapping(target = "ownerId", expression = "java(deriveOwnerId(matchingGroup))")
    @Mapping(target = "ownerName", expression = "java(deriveOwnerName(matchingGroup))")
    @Mapping(target = "ownerAvatarUrl", expression = "java(deriveOwnerAvatarUrl(matchingGroup))")
    @Mapping(target = "leaderName", expression = "java(deriveLeaderName(matchingGroup))")
    @Mapping(target = "leaderAvatarUrl", expression = "java(deriveLeaderAvatarUrl(matchingGroup))")
    MatchingGroupResponse toResponse(MatchingGroup matchingGroup);

    @Mapping(target = "sourceType", expression = "java(deriveSourceType(matchingGroup))")
    @Mapping(target = "tourId", source = "tour.tourId")
    @Mapping(target = "tourName", source = "tour.tourName")
    @Mapping(target = "tourDescription", source = "tour.description")
    @Mapping(target = "tourLocation", source = "tour.location")
    @Mapping(target = "customJourneyId", source = "customJourney.customJourneyId")
    @Mapping(target = "customJourneyTitle", source = "customJourney.title")
    @Mapping(target = "customJourneyDescription", source = "customJourney.description")
    @Mapping(target = "customJourneyStartDate", source = "customJourney.startDate")
    @Mapping(target = "customJourneyEndDate", source = "customJourney.endDate")
    @Mapping(target = "isLocked", source = "customJourney.isLocked")
    @Mapping(target = "checkpoints", expression = "java(toCheckpointResponses(matchingGroup.getCustomJourney()))")
    @Mapping(target = "costItems", expression = "java(toCostItemResponses(matchingGroup.getCustomJourney()))")
    @Mapping(target = "difficulty", expression = "java(deriveDifficulty(matchingGroup))")
    @Mapping(target = "location", expression = "java(deriveLocation(matchingGroup))")
    @Mapping(target = "estimatedCost", expression = "java(deriveEstimatedCost(matchingGroup))")
    @Mapping(target = "coverImageUrl", expression = "java(deriveCoverImageUrl(matchingGroup))")
    @Mapping(target = "ownerId", expression = "java(deriveOwnerId(matchingGroup))")
    @Mapping(target = "ownerName", expression = "java(deriveOwnerName(matchingGroup))")
    @Mapping(target = "ownerAvatarUrl", expression = "java(deriveOwnerAvatarUrl(matchingGroup))")
    @Mapping(target = "leaderId", expression = "java(deriveLeaderId(matchingGroup))")
    @Mapping(target = "leaderName", expression = "java(deriveLeaderName(matchingGroup))")
    @Mapping(target = "leaderAvatarUrl", expression = "java(deriveLeaderAvatarUrl(matchingGroup))")
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "isOwner", ignore = true)
    @Mapping(target = "myMembershipStatus", ignore = true)
    @Mapping(target = "canJoin", ignore = true)
    @Mapping(target = "canLeave", ignore = true)
    @Mapping(target = "hasConversation", expression = "java(matchingGroup.getConversation() != null)")
    MatchingGroupDetailResponse toDetailResponse(MatchingGroup matchingGroup);

    @Mapping(target = "userId", expression = "java(deriveMemberUserId(matchingMember))")
    @Mapping(target = "fullName", expression = "java(deriveMemberFullName(matchingMember))")
    @Mapping(target = "avatarUrl", expression = "java(deriveMemberAvatarUrl(matchingMember))")
    @Mapping(target = "trustScore", expression = "java(deriveMemberTrustScore(matchingMember))")
    MatchingMemberResponse toMemberResponse(MatchingMember matchingMember);

    @Mapping(target = "applicationId", source = "applicationId")
    @Mapping(target = "matchingMemberId", source = "applicationId")
    @Mapping(target = "userId", source = "applicant.userId")
    @Mapping(target = "fullName", source = "applicant.fullName")
    @Mapping(target = "avatarUrl", source = "applicant.avatarUrl")
    @Mapping(target = "trustScore", source = "applicant.trustScore")
    @Mapping(target = "role", constant = "MEMBER")
    @Mapping(target = "status", expression = "java(toJoinStatus(application.getStatus()))")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "rejectReason", source = "rejectReason")
    @Mapping(target = "reviewedAt", source = "reviewedAt")
    @Mapping(target = "withdrawnAt", source = "withdrawnAt")
    MatchingMemberResponse toMemberResponse(GroupJoinApplication application);

    @Mapping(target = "progressUpdatedByName", source = "progressUpdatedBy.user.fullName")
    @Mapping(target = "imageUrls", expression = "java(splitImageUrls(checkpoint.getImageUrl()))")
    CustomJourneyCheckpointResponse toCheckpointResponse(CustomJourneyCheckpoint checkpoint);

    CustomJourneyCostItemResponse toCostItemResponse(CustomJourneyCostItem costItem);

    @Mapping(target = "matchingGroupId", source = "matchingGroup.matchingGroupId")
    @Mapping(target = "groupName", source = "matchingGroup.groupName")
    @Mapping(target = "groupStatus", source = "matchingGroup.status")
    @Mapping(target = "coverImageUrl", expression = "java(deriveCoverImageUrl(matchingMember.getMatchingGroup()))")
    @Mapping(target = "sourceType", expression = "java(deriveSourceType(matchingMember.getMatchingGroup()))")
    @Mapping(target = "tourId", source = "matchingGroup.tour.tourId")
    @Mapping(target = "tourName", source = "matchingGroup.tour.tourName")
    @Mapping(target = "customJourneyId", source = "matchingGroup.customJourney.customJourneyId")
    @Mapping(target = "customJourneyTitle", source = "matchingGroup.customJourney.title")
    @Mapping(target = "difficulty", expression = "java(deriveDifficulty(matchingMember.getMatchingGroup()))")
    @Mapping(target = "location", expression = "java(deriveLocation(matchingMember.getMatchingGroup()))")
    @Mapping(target = "ownerId", expression = "java(deriveOwnerId(matchingMember.getMatchingGroup()))")
    @Mapping(target = "ownerName", expression = "java(deriveOwnerName(matchingMember.getMatchingGroup()))")
    @Mapping(target = "ownerAvatarUrl", expression = "java(deriveOwnerAvatarUrl(matchingMember.getMatchingGroup()))")
    @Mapping(target = "currentSize", source = "matchingGroup.currentSize")
    @Mapping(target = "maxSize", source = "matchingGroup.maxSize")
    @Mapping(target = "targetDate", source = "matchingGroup.targetDate")
    @Mapping(target = "matchingDeadline", source = "matchingGroup.matchingDeadline")
    @Mapping(target = "canCancel", ignore = true)
    @Mapping(target = "canWithdraw", ignore = true)
    MyMatchingJoinRequestResponse toMyJoinRequestResponse(MatchingMember matchingMember);

    @Mapping(target = "applicationId", source = "applicationId")
    @Mapping(target = "matchingMemberId", source = "applicationId")
    @Mapping(target = "matchingGroupId", source = "matchingGroup.matchingGroupId")
    @Mapping(target = "groupName", source = "matchingGroup.groupName")
    @Mapping(target = "groupStatus", source = "matchingGroup.status")
    @Mapping(target = "coverImageUrl", expression = "java(deriveCoverImageUrl(application.getMatchingGroup()))")
    @Mapping(target = "sourceType", expression = "java(deriveSourceType(application.getMatchingGroup()))")
    @Mapping(target = "tourId", source = "matchingGroup.tour.tourId")
    @Mapping(target = "tourName", source = "matchingGroup.tour.tourName")
    @Mapping(target = "customJourneyId", source = "matchingGroup.customJourney.customJourneyId")
    @Mapping(target = "customJourneyTitle", source = "matchingGroup.customJourney.title")
    @Mapping(target = "difficulty", expression = "java(deriveDifficulty(application.getMatchingGroup()))")
    @Mapping(target = "location", expression = "java(deriveLocation(application.getMatchingGroup()))")
    @Mapping(target = "ownerId", source = "matchingGroup.owner.userId")
    @Mapping(target = "ownerName", source = "matchingGroup.owner.fullName")
    @Mapping(target = "ownerAvatarUrl", source = "matchingGroup.owner.avatarUrl")
    @Mapping(target = "currentSize", source = "matchingGroup.currentSize")
    @Mapping(target = "maxSize", source = "matchingGroup.maxSize")
    @Mapping(target = "targetDate", source = "matchingGroup.targetDate")
    @Mapping(target = "matchingDeadline", source = "matchingGroup.matchingDeadline")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "rejectReason", source = "rejectReason")
    @Mapping(target = "status", expression = "java(toJoinStatus(application.getStatus()))")
    @Mapping(target = "reviewedAt", source = "reviewedAt")
    @Mapping(target = "withdrawnAt", source = "withdrawnAt")
    @Mapping(target = "canCancel", ignore = true)
    @Mapping(target = "canWithdraw", ignore = true)
    MyMatchingJoinRequestResponse toMyJoinRequestResponse(GroupJoinApplication application);

    default JoinStatus toJoinStatus(JoinApplicationStatus appStatus) {
        if (appStatus == null) return null;
        return switch (appStatus) {
            case PENDING -> JoinStatus.PENDING;
            case ACCEPTED -> JoinStatus.ACCEPTED;
            case REJECTED -> JoinStatus.REJECTED;
            case WITHDRAWN -> JoinStatus.WITHDRAWN;
        };
    }

    default String deriveCoverImageUrl(MatchingGroup mg) {
        if (mg == null) return null;
        if (mg.getCoverImageUrl() != null && !mg.getCoverImageUrl().isBlank()) {
            return mg.getCoverImageUrl();
        }
        if (mg.getTour() != null) {
            return mg.getTour().getCoverImageUrl();
        }
        return null;
    }

    default MatchingGroupSourceType deriveSourceType(MatchingGroup mg) {
        if (mg == null) return null;
        if (mg.getTour() != null) return MatchingGroupSourceType.TOUR;
        if (mg.getCustomJourney() != null) return MatchingGroupSourceType.CUSTOM_JOURNEY;
        return null;
    }

    default String deriveDifficulty(MatchingGroup mg) {
        if (mg == null) return null;
        if (mg.getTour() != null && mg.getTour().getDifficulty() != null) {
            return mg.getTour().getDifficulty().name();
        }
        if (mg.getCustomJourney() != null && mg.getCustomJourney().getDifficulty() != null) {
            return mg.getCustomJourney().getDifficulty().name();
        }
        return null;
    }

    default String deriveLocation(MatchingGroup mg) {
        if (mg == null) return null;
        if (mg.getTour() != null) {
            return mg.getTour().getLocation();
        }
        if (mg.getCustomJourney() != null && mg.getCustomJourney().getCheckpoints() != null) {
            return mg.getCustomJourney().getCheckpoints().stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                    .map(CustomJourneyCheckpoint::getLocationName)
                    .filter(loc -> loc != null && !loc.isBlank())
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    default BigDecimal deriveEstimatedCost(MatchingGroup mg) {
        if (mg == null) return null;
        if (mg.getCustomJourney() != null && mg.getCustomJourney().getCostItems() != null) {
            BigDecimal sum = mg.getCustomJourney().getCostItems().stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                    .map(CustomJourneyCostItem::getEstimatedAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(BigDecimal.ZERO) > 0) {
                return sum;
            }
        }
        return null;
    }

    default List<String> splitImageUrls(String rawUrls) {
        if (rawUrls == null || rawUrls.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(rawUrls.split(","));
    }

    default List<CustomJourneyCheckpointResponse> toCheckpointResponses(CustomJourney customJourney) {
        if (customJourney == null || customJourney.getCheckpoints() == null) {
            return Collections.emptyList();
        }
        return customJourney.getCheckpoints().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .map(this::toCheckpointResponse)
                .toList();
    }

    default List<CustomJourneyCostItemResponse> toCostItemResponses(CustomJourney customJourney) {
        if (customJourney == null || customJourney.getCostItems() == null) {
            return Collections.emptyList();
        }
        return customJourney.getCostItems().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .map(this::toCostItemResponse)
                .toList();
    }

    default java.util.UUID deriveOwnerId(MatchingGroup mg) {
        if (mg == null || mg.getOwner() == null) return null;
        if (mg.getOwner().getStatus() == UserStatus.LOCKED) {
            return null;
        }
        return mg.getOwner().getUserId();
    }

    default String deriveOwnerName(MatchingGroup mg) {
        if (mg == null || mg.getOwner() == null) return null;
        if (mg.getOwner().getStatus() == UserStatus.LOCKED) {
            return BlogService.SYSTEM_USER_ANONYMOUS_NAME;
        }
        return mg.getOwner().getFullName();
    }

    default String deriveOwnerAvatarUrl(MatchingGroup mg) {
        if (mg == null || mg.getOwner() == null) return null;
        if (mg.getOwner().getStatus() == UserStatus.LOCKED) {
            return null;
        }
        return mg.getOwner().getAvatarUrl();
    }

    default java.util.UUID deriveMemberUserId(MatchingMember mm) {
        if (mm == null || mm.getUser() == null) return null;
        if (mm.getUser().getStatus() == UserStatus.LOCKED) {
            return null;
        }
        return mm.getUser().getUserId();
    }

    default String deriveMemberFullName(MatchingMember mm) {
        if (mm == null || mm.getUser() == null) return null;
        if (mm.getUser().getStatus() == UserStatus.LOCKED) {
            return BlogService.SYSTEM_USER_ANONYMOUS_NAME;
        }
        return mm.getUser().getFullName();
    }

    default String deriveMemberAvatarUrl(MatchingMember mm) {
        if (mm == null || mm.getUser() == null) return null;
        if (mm.getUser().getStatus() == UserStatus.LOCKED) {
            return null;
        }
        return mm.getUser().getAvatarUrl();
    }

    default Short deriveMemberTrustScore(MatchingMember mm) {
        if (mm == null || mm.getUser() == null) return null;
        if (mm.getUser().getStatus() == UserStatus.LOCKED) {
            return null;
        }
        return mm.getUser().getTrustScore();
    }

    default java.util.UUID deriveLeaderId(MatchingGroup mg) {
        if (mg == null || mg.getMembers() == null) return deriveOwnerId(mg);
        for (MatchingMember m : mg.getMembers()) {
            if (m.getRole() == MatchingRole.LEADER
                    && m.getStatus() == JoinStatus.ACCEPTED
                    && !Boolean.TRUE.equals(m.getIsDeleted())
                    && m.getUser() != null) {
                return deriveMemberUserId(m);
            }
        }
        return deriveOwnerId(mg);
    }

    default String deriveLeaderName(MatchingGroup mg) {
        if (mg == null || mg.getMembers() == null) return deriveOwnerName(mg);
        for (MatchingMember m : mg.getMembers()) {
            if (m.getRole() == MatchingRole.LEADER
                    && m.getStatus() == JoinStatus.ACCEPTED
                    && !Boolean.TRUE.equals(m.getIsDeleted())
                    && m.getUser() != null) {
                return deriveMemberFullName(m);
            }
        }
        return deriveOwnerName(mg);
    }

    default String deriveLeaderAvatarUrl(MatchingGroup mg) {
        if (mg == null || mg.getMembers() == null) return deriveOwnerAvatarUrl(mg);
        for (MatchingMember m : mg.getMembers()) {
            if (m.getRole() == MatchingRole.LEADER
                    && m.getStatus() == JoinStatus.ACCEPTED
                    && !Boolean.TRUE.equals(m.getIsDeleted())
                    && m.getUser() != null) {
                return deriveMemberAvatarUrl(m);
            }
        }
        return deriveOwnerAvatarUrl(mg);
    }
}

