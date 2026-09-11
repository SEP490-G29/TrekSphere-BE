package com.sep.treksphere.matching.mapper;

import com.sep.treksphere.matching.dto.request.GroupPostCommentCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostCommentUpdateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostUpdateRequest;
import com.sep.treksphere.matching.dto.response.GroupPostCommentResponse;
import com.sep.treksphere.matching.dto.response.GroupPostResponse;
import com.sep.treksphere.matching.entity.GroupPost;
import com.sep.treksphere.matching.entity.GroupPostComment;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupPostMapper {

    @Mapping(target = "matchingGroupId", source = "matchingGroup.matchingGroupId")
    @Mapping(target = "postedByMatchingMemberId", source = "postedBy.matchingMemberId")
    @Mapping(target = "postedByUserId", source = "postedBy.user.userId")
    @Mapping(target = "postedByFullName", source = "postedBy.user.fullName")
    @Mapping(target = "postedByAvatarUrl", source = "postedBy.user.avatarUrl")
    @Mapping(target = "postedByRole", source = "postedBy.role")
    @Mapping(target = "commentCount", ignore = true)
    GroupPostResponse toPostResponse(GroupPost post);

    List<GroupPostResponse> toPostResponseList(List<GroupPost> posts);

    @Mapping(target = "groupPostId", source = "groupPost.groupPostId")
    @Mapping(target = "parentCommentId", source = "parentComment.groupPostCommentId")
    @Mapping(target = "replyToCommentId", source = "replyToComment.groupPostCommentId")
    @Mapping(target = "replyToUserId", source = "replyToMember.user.userId")
    @Mapping(target = "replyToFullName", source = "replyToMember.user.fullName")
    @Mapping(target = "answeredByMatchingMemberId", source = "answeredBy.matchingMemberId")
    @Mapping(target = "answeredByUserId", source = "answeredBy.user.userId")
    @Mapping(target = "answeredByFullName", source = "answeredBy.user.fullName")
    @Mapping(target = "answeredByAvatarUrl", source = "answeredBy.user.avatarUrl")
    @Mapping(target = "answeredByRole", source = "answeredBy.role")
    @Mapping(target = "replies", ignore = true)
    GroupPostCommentResponse toCommentResponse(GroupPostComment comment);

    List<GroupPostCommentResponse> toCommentResponseList(List<GroupPostComment> comments);

    @Mapping(target = "groupPostId", ignore = true)
    @Mapping(target = "matchingGroup", ignore = true)
    @Mapping(target = "postedBy", ignore = true)
    @Mapping(target = "status", ignore = true)
    GroupPost toPostEntity(GroupPostCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "groupPostId", ignore = true)
    @Mapping(target = "matchingGroup", ignore = true)
    @Mapping(target = "postedBy", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updatePostEntityFromRequest(GroupPostUpdateRequest request, @MappingTarget GroupPost post);

    @Mapping(target = "groupPostCommentId", ignore = true)
    @Mapping(target = "groupPost", ignore = true)
    @Mapping(target = "answeredBy", ignore = true)
    @Mapping(target = "parentComment", ignore = true)
    @Mapping(target = "replyToComment", ignore = true)
    @Mapping(target = "replyToMember", ignore = true)
    @Mapping(target = "replies", ignore = true)
    @Mapping(target = "status", ignore = true)
    GroupPostComment toCommentEntity(GroupPostCommentCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "groupPostCommentId", ignore = true)
    @Mapping(target = "groupPost", ignore = true)
    @Mapping(target = "answeredBy", ignore = true)
    @Mapping(target = "parentComment", ignore = true)
    @Mapping(target = "replyToComment", ignore = true)
    @Mapping(target = "replyToMember", ignore = true)
    @Mapping(target = "replies", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateCommentEntityFromRequest(GroupPostCommentUpdateRequest request, @MappingTarget GroupPostComment comment);
}
