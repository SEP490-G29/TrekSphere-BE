package com.sep.treksphere.matching.mapper;

import com.sep.treksphere.matching.dto.request.GroupPostCommentCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostCommentUpdateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostUpdateRequest;
import com.sep.treksphere.matching.dto.response.GroupPostCommentResponse;
import com.sep.treksphere.matching.dto.response.GroupPostResponse;
import com.sep.treksphere.matching.entity.GroupPost;
import com.sep.treksphere.matching.entity.GroupPostComment;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupPostMapper {

    @Mapping(target = "matchingGroupId", source = "matchingGroup.matchingGroupId")
    @Mapping(target = "postedByMatchingMemberId", source = "postedBy.matchingMemberId")
    @Mapping(target = "postedByUserId", expression = "java(derivePostedByUserId(post))")
    @Mapping(target = "postedByFullName", expression = "java(derivePostedByFullName(post))")
    @Mapping(target = "postedByAvatarUrl", expression = "java(derivePostedByAvatarUrl(post))")
    @Mapping(target = "postedByRole", source = "postedBy.role")
    @Mapping(target = "commentCount", ignore = true)
    GroupPostResponse toPostResponse(GroupPost post);

    List<GroupPostResponse> toPostResponseList(List<GroupPost> posts);

    @Mapping(target = "groupPostId", source = "groupPost.groupPostId")
    @Mapping(target = "parentCommentId", source = "parentComment.groupPostCommentId")
    @Mapping(target = "replyToCommentId", source = "replyToComment.groupPostCommentId")
    @Mapping(target = "replyToUserId", expression = "java(deriveReplyToUserId(comment))")
    @Mapping(target = "replyToFullName", expression = "java(deriveReplyToFullName(comment))")
    @Mapping(target = "answeredByMatchingMemberId", source = "answeredBy.matchingMemberId")
    @Mapping(target = "answeredByUserId", expression = "java(deriveAnsweredByUserId(comment))")
    @Mapping(target = "answeredByFullName", expression = "java(deriveAnsweredByFullName(comment))")
    @Mapping(target = "answeredByAvatarUrl", expression = "java(deriveAnsweredByAvatarUrl(comment))")
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

    default java.util.UUID derivePostedByUserId(GroupPost post) {
        if (post == null || post.getPostedBy() == null || post.getPostedBy().getUser() == null) return null;
        if (post.getPostedBy().getUser().getStatus() == com.sep.treksphere.user.UserStatus.LOCKED) {
            return null;
        }
        return post.getPostedBy().getUser().getUserId();
    }

    default String derivePostedByFullName(GroupPost post) {
        if (post == null || post.getPostedBy() == null || post.getPostedBy().getUser() == null) return null;
        if (post.getPostedBy().getUser().getStatus() == com.sep.treksphere.user.UserStatus.LOCKED) {
            return com.sep.treksphere.blog.BlogService.SYSTEM_USER_ANONYMOUS_NAME;
        }
        return post.getPostedBy().getUser().getFullName();
    }

    default String derivePostedByAvatarUrl(GroupPost post) {
        if (post == null || post.getPostedBy() == null || post.getPostedBy().getUser() == null) return null;
        if (post.getPostedBy().getUser().getStatus() == com.sep.treksphere.user.UserStatus.LOCKED) {
            return null;
        }
        return post.getPostedBy().getUser().getAvatarUrl();
    }

    default java.util.UUID deriveAnsweredByUserId(GroupPostComment comment) {
        if (comment == null || comment.getAnsweredBy() == null || comment.getAnsweredBy().getUser() == null) return null;
        if (comment.getAnsweredBy().getUser().getStatus() == com.sep.treksphere.user.UserStatus.LOCKED) {
            return null;
        }
        return comment.getAnsweredBy().getUser().getUserId();
    }

    default String deriveAnsweredByFullName(GroupPostComment comment) {
        if (comment == null || comment.getAnsweredBy() == null || comment.getAnsweredBy().getUser() == null) return null;
        if (comment.getAnsweredBy().getUser().getStatus() == com.sep.treksphere.user.UserStatus.LOCKED) {
            return com.sep.treksphere.blog.BlogService.SYSTEM_USER_ANONYMOUS_NAME;
        }
        return comment.getAnsweredBy().getUser().getFullName();
    }

    default String deriveAnsweredByAvatarUrl(GroupPostComment comment) {
        if (comment == null || comment.getAnsweredBy() == null || comment.getAnsweredBy().getUser() == null) return null;
        if (comment.getAnsweredBy().getUser().getStatus() == com.sep.treksphere.user.UserStatus.LOCKED) {
            return null;
        }
        return comment.getAnsweredBy().getUser().getAvatarUrl();
    }

    default java.util.UUID deriveReplyToUserId(GroupPostComment comment) {
        if (comment == null || comment.getReplyToMember() == null || comment.getReplyToMember().getUser() == null) return null;
        if (comment.getReplyToMember().getUser().getStatus() == com.sep.treksphere.user.UserStatus.LOCKED) {
            return null;
        }
        return comment.getReplyToMember().getUser().getUserId();
    }

    default String deriveReplyToFullName(GroupPostComment comment) {
        if (comment == null || comment.getReplyToMember() == null || comment.getReplyToMember().getUser() == null) return null;
        if (comment.getReplyToMember().getUser().getStatus() == com.sep.treksphere.user.UserStatus.LOCKED) {
            return com.sep.treksphere.blog.BlogService.SYSTEM_USER_ANONYMOUS_NAME;
        }
        return comment.getReplyToMember().getUser().getFullName();
    }
}
