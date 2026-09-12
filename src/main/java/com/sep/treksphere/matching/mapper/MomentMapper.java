package com.sep.treksphere.matching.mapper;

import com.sep.treksphere.matching.dto.request.MomentCreateRequest;
import com.sep.treksphere.matching.dto.request.MomentUpdateRequest;
import com.sep.treksphere.matching.dto.response.MomentMapResponse;
import com.sep.treksphere.matching.dto.response.MomentMediaResponse;
import com.sep.treksphere.matching.dto.response.MomentResponse;
import com.sep.treksphere.matching.entity.Moment;
import com.sep.treksphere.matching.entity.MomentMedia;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MomentMapper {

    @Mapping(target = "authorUserId", source = "authorUser.userId")
    @Mapping(target = "authorName", source = "authorUser.fullName")
    @Mapping(target = "authorAvatarUrl", source = "authorUser.avatarUrl")
    @Mapping(target = "matchingGroupId", source = "matchingGroup.matchingGroupId")
    @Mapping(target = "groupName", source = "matchingGroup.groupName")
    @Mapping(target = "authorMatchingMemberId", source = "authorMatchingMember.matchingMemberId")
    @Mapping(target = "hiddenByUserId", source = "hiddenByUser.userId")
    @Mapping(target = "mediaList", source = "mediaList")
    MomentResponse toResponse(Moment entity);

    List<MomentResponse> toResponseList(List<Moment> list);

    MomentMediaResponse toMediaResponse(MomentMedia media);

    List<MomentMediaResponse> toMediaResponseList(List<MomentMedia> list);

    @Mapping(target = "momentId", source = "momentId")
    @Mapping(target = "placeName", source = "placeName")
    @Mapping(target = "latitude", source = "latitude")
    @Mapping(target = "longitude", source = "longitude")
    @Mapping(target = "capturedAt", source = "capturedAt")
    @Mapping(target = "authorName", source = "authorUser.fullName")
    @Mapping(target = "authorAvatarUrl", source = "authorUser.avatarUrl")
    @Mapping(target = "matchingGroupId", source = "matchingGroup.matchingGroupId")
    @Mapping(target = "groupName", source = "matchingGroup.groupName")
    @Mapping(target = "thumbnailUrl", expression = "java(entity.getMediaList() != null && !entity.getMediaList().isEmpty() ? entity.getMediaList().get(0).getImageUrl() : null)")
    MomentMapResponse toMapResponse(Moment entity);

    List<MomentMapResponse> toMapResponseList(List<Moment> list);

    @Mapping(target = "momentId", ignore = true)
    @Mapping(target = "authorUser", ignore = true)
    @Mapping(target = "matchingGroup", ignore = true)
    @Mapping(target = "authorMatchingMember", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "hiddenByUser", ignore = true)
    @Mapping(target = "hiddenReason", ignore = true)
    @Mapping(target = "mediaList", ignore = true)
    Moment toEntity(MomentCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "momentId", ignore = true)
    @Mapping(target = "authorUser", ignore = true)
    @Mapping(target = "matchingGroup", ignore = true)
    @Mapping(target = "authorMatchingMember", ignore = true)
    @Mapping(target = "visibility", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "hiddenByUser", ignore = true)
    @Mapping(target = "hiddenReason", ignore = true)
    @Mapping(target = "mediaList", ignore = true)
    void updateEntityFromRequest(MomentUpdateRequest request, @MappingTarget Moment entity);
}
