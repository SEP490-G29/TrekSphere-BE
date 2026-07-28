package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.MatchingGroupResponse;
import com.sep.treksphere.entity.MatchingGroup;
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
}
