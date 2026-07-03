package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Blog;
import com.sep.treksphere.entity.BlogComment;
import com.sep.treksphere.enums.blog.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BlogCommentRepository extends JpaRepository<BlogComment, UUID> {

    @Query("""
            SELECT c FROM BlogComment c
            JOIN FETCH c.user u
            WHERE c.blog.blogID = :blogId
              AND c.status = :status
            ORDER BY c.createdAt ASC
            """)
    List<BlogComment> findAllByBlogIdAndStatus(
            @Param("blogId") UUID blogId,
            @Param("status") CommentStatus status);

    @Query("""
            SELECT c FROM BlogComment c
            JOIN FETCH c.user u
            WHERE c.blog.blogID = :blogId
              AND c.parentComment IS NULL
              AND c.status = :status
            ORDER BY c.createdAt ASC
            """)
    Page<BlogComment> findTopLevelByBlogId(
            @Param("blogId") UUID blogId,
            @Param("status") CommentStatus status,
            Pageable pageable);

    @Query("""
            SELECT c FROM BlogComment c
            JOIN FETCH c.user u
            WHERE c.parentComment.commentID = :parentId
              AND c.status = :status
            ORDER BY c.createdAt ASC
            """)
    List<BlogComment> findRepliesByParentId(
            @Param("parentId") UUID parentId,
            @Param("status") CommentStatus status);

    int countByBlogAndStatus(Blog blog, CommentStatus status);
}
