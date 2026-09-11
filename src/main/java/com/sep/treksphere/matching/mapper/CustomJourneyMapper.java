package com.sep.treksphere.matching.mapper;

import com.sep.treksphere.matching.dto.request.CustomJourneyActivityCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyActivityUpdateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointUpdateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyUpdateRequest;
import com.sep.treksphere.matching.dto.response.CustomJourneyActivityResponse;
import com.sep.treksphere.matching.dto.response.CustomJourneyCheckpointResponse;
import com.sep.treksphere.matching.dto.response.CustomJourneyCostItemResponse;
import com.sep.treksphere.matching.dto.response.CustomJourneyDetailResponse;
import com.sep.treksphere.matching.entity.CustomJourney;
import com.sep.treksphere.matching.entity.CustomJourneyActivity;
import com.sep.treksphere.matching.entity.CustomJourneyCheckpoint;
import com.sep.treksphere.matching.entity.CustomJourneyCostItem;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomJourneyMapper {

    @Mapping(target = "matchingGroupId", source = "matchingGroup.matchingGroupId")
    @Mapping(target = "checkpoints", expression = "java(mapCheckpoints(customJourney.getCheckpoints()))")
    @Mapping(target = "activities", expression = "java(mapActivities(customJourney.getActivities()))")
    @Mapping(target = "costItems", expression = "java(mapCostItems(customJourney.getCostItems()))")
    CustomJourneyDetailResponse toDetailResponse(CustomJourney customJourney);

    CustomJourneyCheckpointResponse toCheckpointResponse(CustomJourneyCheckpoint checkpoint);

    @Mapping(target = "customJourneyId", source = "customJourney.customJourneyId")
    @Mapping(target = "customJourneyCheckpointId", source = "checkpoint.customJourneyCheckpointId")
    @Mapping(target = "checkpointTitle", source = "checkpoint.title")
    CustomJourneyActivityResponse toActivityResponse(CustomJourneyActivity activity);

    CustomJourneyCostItemResponse toCostItemResponse(CustomJourneyCostItem costItem);

    @Mapping(target = "customJourney", ignore = true)
    @Mapping(target = "customJourneyCheckpointId", ignore = true)
    CustomJourneyCheckpoint toCheckpointEntity(CustomJourneyCheckpointCreateRequest request);

    @Mapping(target = "customJourney", ignore = true)
    @Mapping(target = "checkpoint", ignore = true)
    @Mapping(target = "customJourneyActivityId", ignore = true)
    CustomJourneyActivity toActivityEntity(CustomJourneyActivityCreateRequest request);

    @Mapping(target = "customJourney", ignore = true)
    @Mapping(target = "customJourneyCostItemId", ignore = true)
    CustomJourneyCostItem toCostItemEntity(com.sep.treksphere.matching.dto.request.CustomJourneyCostItemCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "customJourneyCostItemId", ignore = true)
    @Mapping(target = "customJourney", ignore = true)
    void updateCostItemFromRequest(com.sep.treksphere.matching.dto.request.CustomJourneyCostItemUpdateRequest request, @MappingTarget CustomJourneyCostItem costItem);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "customJourneyId", ignore = true)
    @Mapping(target = "matchingGroup", ignore = true)
    @Mapping(target = "isLocked", ignore = true)
    @Mapping(target = "lockedAt", ignore = true)
    @Mapping(target = "checkpoints", ignore = true)
    @Mapping(target = "activities", ignore = true)
    @Mapping(target = "costItems", ignore = true)
    void updateEntityFromRequest(CustomJourneyUpdateRequest request, @MappingTarget CustomJourney customJourney);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "customJourneyCheckpointId", ignore = true)
    @Mapping(target = "customJourney", ignore = true)
    void updateCheckpointFromRequest(CustomJourneyCheckpointUpdateRequest request, @MappingTarget CustomJourneyCheckpoint checkpoint);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "customJourneyActivityId", ignore = true)
    @Mapping(target = "customJourney", ignore = true)
    @Mapping(target = "checkpoint", ignore = true)
    void updateActivityFromRequest(CustomJourneyActivityUpdateRequest request, @MappingTarget CustomJourneyActivity activity);

    default List<CustomJourneyCheckpointResponse> mapCheckpoints(Set<CustomJourneyCheckpoint> checkpoints) {
        if (checkpoints == null || checkpoints.isEmpty()) {
            return Collections.emptyList();
        }
        return checkpoints.stream()
                .filter(c -> Boolean.FALSE.equals(c.getIsDeleted()))
                .sorted(Comparator.comparing(
                        (CustomJourneyCheckpoint c) -> c.getDayNo() != null ? c.getDayNo() : 0)
                        .thenComparing(CustomJourneyCheckpoint::getCheckpointOrder))
                .map(this::toCheckpointResponse)
                .toList();
    }

    default List<CustomJourneyActivityResponse> mapActivities(Set<CustomJourneyActivity> activities) {
        if (activities == null || activities.isEmpty()) {
            return Collections.emptyList();
        }
        return activities.stream()
                .filter(a -> Boolean.FALSE.equals(a.getIsDeleted()))
                .sorted(Comparator.comparing(
                        (CustomJourneyActivity a) -> a.getDayNo() != null ? a.getDayNo() : 0)
                        .thenComparing(a -> a.getTimeSlot() != null ? a.getTimeSlot().ordinal() : 0)
                        .thenComparing(CustomJourneyActivity::getActivityOrder))
                .map(this::toActivityResponse)
                .toList();
    }

    default List<CustomJourneyCostItemResponse> mapCostItems(Set<CustomJourneyCostItem> costItems) {
        if (costItems == null || costItems.isEmpty()) {
            return Collections.emptyList();
        }
        return costItems.stream()
                .filter(c -> Boolean.FALSE.equals(c.getIsDeleted()))
                .map(this::toCostItemResponse)
                .toList();
    }
}
