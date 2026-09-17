package com.sep.treksphere.matching.mapper;

import com.sep.treksphere.matching.dto.request.MomentCreateRequest;
import com.sep.treksphere.matching.dto.request.MomentUpdateRequest;
import com.sep.treksphere.matching.dto.response.MomentMapResponse;
import com.sep.treksphere.matching.dto.response.MomentMediaResponse;
import com.sep.treksphere.matching.dto.response.MomentResponse;
import com.sep.treksphere.matching.entity.Moment;
import com.sep.treksphere.matching.entity.MomentMedia;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MomentMapper {

    @Mapping(target = "authorUserId", expression = "java(deriveAuthorUserId(entity))")
    @Mapping(target = "authorName", expression = "java(deriveAuthorName(entity))")
    @Mapping(target = "authorAvatarUrl", expression = "java(deriveAuthorAvatarUrl(entity))")
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
    @Mapping(target = "authorName", expression = "java(deriveAuthorName(entity))")
    @Mapping(target = "authorAvatarUrl", expression = "java(deriveAuthorAvatarUrl(entity))")
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

    default java.util.UUID deriveAuthorUserId(Moment entity) {
        if (entity == null || entity.getAuthorUser() == null) return null;
        if (entity.getAuthorUser().getStatus() == com.sep.treksphere.user.UserStatus.LOCKED) {
            return null;
        }
        return entity.getAuthorUser().getUserId();
    }

    default String deriveAuthorName(Moment entity) {
        if (entity == null || entity.getAuthorUser() == null) return null;
        if (entity.getAuthorUser().getStatus() == com.sep.treksphere.user.UserStatus.LOCKED) {
            return com.sep.treksphere.blog.BlogService.SYSTEM_USER_ANONYMOUS_NAME;
        }
        return entity.getAuthorUser().getFullName();
    }

    default String deriveAuthorAvatarUrl(Moment entity) {
        if (entity == null || entity.getAuthorUser() == null) return null;
        if (entity.getAuthorUser().getStatus() == com.sep.treksphere.user.UserStatus.LOCKED) {
            return null;
        }
        return entity.getAuthorUser().getAvatarUrl();
    }
}
