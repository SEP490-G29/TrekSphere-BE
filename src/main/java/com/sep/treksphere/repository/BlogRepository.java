package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Blog;
import com.sep.treksphere.enums.blog.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlogRepository extends JpaRepository<Blog, UUID> {

    @Query("""
               SELECT b FROM Blog b
               JOIN FETCH b.user u
               WHERE b.isDeleted = false
                 AND b.status = :status
                 AND (CAST(:keyword AS string) IS NULL
                      OR LOWER(b.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
               """)
    Page<Blog> searchBlogs(
               @Param("status") BlogStatus status,
               @Param("keyword") String keyword,
               Pageable pageable);

    @Query("""
               SELECT b FROM Blog b
               JOIN FETCH b.user u
               WHERE b.blogId = :blogId AND b.isDeleted = false
               """)
    Optional<Blog> findDetailById(@Param("blogId") UUID blogId);
}
