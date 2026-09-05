package com.sep.treksphere.matching.mapper;

import com.sep.treksphere.matching.dto.response.*;
import com.sep.treksphere.matching.entity.*;
import com.sep.treksphere.matching.enums.MatchingGroupSourceType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MatchingGroupMapper {

    @Mapping(target = "sourceType", expression = "java(deriveSourceType(matchingGroup))")
    @Mapping(target = "tourId", source = "tour.tourId")
    @Mapping(target = "tourName", source = "tour.tourName")
    @Mapping(target = "customJourneyId", source = "customJourney.customJourneyId")
    @Mapping(target = "customJourneyTitle", source = "customJourney.title")
    @Mapping(target = "difficulty", expression = "java(deriveDifficulty(matchingGroup))")
    @Mapping(target = "location", expression = "java(deriveLocation(matchingGroup))")
    @Mapping(target = "estimatedCost", expression = "java(deriveEstimatedCost(matchingGroup))")
    @Mapping(target = "ownerId", source = "owner.userId")
    @Mapping(target = "ownerName", source = "owner.fullName")
    @Mapping(target = "ownerAvatarUrl", source = "owner.avatarUrl")
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

    CustomJourneyCheckpointResponse toCheckpointResponse(CustomJourneyCheckpoint checkpoint);

    CustomJourneyCostItemResponse toCostItemResponse(CustomJourneyCostItem costItem);

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
            return mg.getCustomJourney().getCostItems().stream()
                    .map(CustomJourneyCostItem::getEstimatedAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return null;
    }

    default List<CustomJourneyCheckpointResponse> toCheckpointResponses(CustomJourney customJourney) {
        if (customJourney == null || customJourney.getCheckpoints() == null) {
            return Collections.emptyList();
        }
        return customJourney.getCheckpoints().stream()
                .map(this::toCheckpointResponse)
                .toList();
    }

    default List<CustomJourneyCostItemResponse> toCostItemResponses(CustomJourney customJourney) {
        if (customJourney == null || customJourney.getCostItems() == null) {
            return Collections.emptyList();
        }
        return customJourney.getCostItems().stream()
                .map(this::toCostItemResponse)
                .toList();
    }
}
