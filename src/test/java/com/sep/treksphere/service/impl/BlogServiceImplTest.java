package com.sep.treksphere.service.impl;

import com.sep.treksphere.entity.Blog;
import com.sep.treksphere.entity.Role;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.blog.BlogStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.BlogCommentRepository;
import com.sep.treksphere.repository.BlogRepository;
import com.sep.treksphere.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogServiceImplTest {

    @Mock
    private BlogRepository blogRepository;

    @Mock
    private BlogCommentRepository blogCommentRepository;

    @InjectMocks
    private BlogServiceImpl blogService;

    private User author;
    private User otherUser;
    private User admin;
    private Blog blog;
    private CustomUserDetails authorDetails;
    private CustomUserDetails otherUserDetails;
    private CustomUserDetails adminDetails;

    private UUID blogId;

    @BeforeEach
    void setUp() {
        blogId = UUID.randomUUID();

        Role roleUser = new Role();
        roleUser.setRoleName("ROLE_USER");
        
        Role roleAdmin = new Role();
        roleAdmin.setRoleName("ROLE_ADMIN");

        // Setup Author
        author = new User();
        author.setUserID(UUID.randomUUID());
        author.setRoles(new HashSet<>(Collections.singletonList(roleUser)));
        authorDetails = new CustomUserDetails(author);

        // Setup Other User
        otherUser = new User();
        otherUser.setUserID(UUID.randomUUID());
        otherUser.setRoles(new HashSet<>(Collections.singletonList(roleUser)));
        otherUserDetails = new CustomUserDetails(otherUser);

        // Setup Admin
        admin = new User();
        admin.setUserID(UUID.randomUUID());
        admin.setRoles(new HashSet<>(Collections.singletonList(roleAdmin)));
        adminDetails = new CustomUserDetails(admin);

        // Setup Blog
        blog = new Blog();
        blog.setBlogID(blogId);
        blog.setUser(author);
        blog.setStatus(BlogStatus.PUBLISHED);
        blog.setIsDeleted(false);
    }

    @Test
    void hideBlog_Success_AsAuthor() {
        when(blogRepository.findDetailById(blogId)).thenReturn(Optional.of(blog));

        blogService.hideBlog(blogId, authorDetails);

        assertEquals(BlogStatus.HIDDEN, blog.getStatus());
        verify(blogRepository, times(1)).save(blog);
    }

    @Test
    void hideBlog_Success_AsAdmin() {
        when(blogRepository.findDetailById(blogId)).thenReturn(Optional.of(blog));

        blogService.hideBlog(blogId, adminDetails);

        assertEquals(BlogStatus.HIDDEN, blog.getStatus());
        verify(blogRepository, times(1)).save(blog);
    }

    @Test
    void hideBlog_Fail_AccessDenied() {
        when(blogRepository.findDetailById(blogId)).thenReturn(Optional.of(blog));

        AppException exception = assertThrows(AppException.class, () -> 
            blogService.hideBlog(blogId, otherUserDetails)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verify(blogRepository, never()).save(any());
    }

    @Test
    void hideBlog_Fail_BlogNotFound() {
        when(blogRepository.findDetailById(blogId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> 
            blogService.hideBlog(blogId, authorDetails)
        );

        assertEquals(ErrorCode.BLOG_NOT_FOUND, exception.getErrorCode());
        verify(blogRepository, never()).save(any());
    }

    @Test
    void deleteBlog_Success_AsAuthor() {
        when(blogRepository.findDetailById(blogId)).thenReturn(Optional.of(blog));

        blogService.deleteBlog(blogId, authorDetails);

        assertEquals(BlogStatus.DELETED, blog.getStatus());
        assertTrue(blog.getIsDeleted());
        assertNotNull(blog.getDeletedAt());
        assertEquals(author.getUserID().toString(), blog.getDeletedBy());
        verify(blogRepository, times(1)).save(blog);
    }

    @Test
    void deleteBlog_Success_AsAdmin() {
        when(blogRepository.findDetailById(blogId)).thenReturn(Optional.of(blog));

        blogService.deleteBlog(blogId, adminDetails);

        assertEquals(BlogStatus.DELETED, blog.getStatus());
        assertTrue(blog.getIsDeleted());
        assertNotNull(blog.getDeletedAt());
        assertEquals(admin.getUserID().toString(), blog.getDeletedBy());
        verify(blogRepository, times(1)).save(blog);
    }

    @Test
    void deleteBlog_Fail_BlogNotFound() {
        when(blogRepository.findDetailById(blogId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> 
            blogService.deleteBlog(blogId, authorDetails)
        );

        assertEquals(ErrorCode.BLOG_NOT_FOUND, exception.getErrorCode());
        verify(blogRepository, never()).save(any());
    }
    @Test
    void deleteBlog_Fail_AccessDenied() {
        when(blogRepository.findDetailById(blogId)).thenReturn(Optional.of(blog));

        AppException exception = assertThrows(AppException.class, () -> 
            blogService.deleteBlog(blogId, otherUserDetails)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verify(blogRepository, never()).save(any());
    }
}
