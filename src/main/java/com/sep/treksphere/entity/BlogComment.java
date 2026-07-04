package com.sep.treksphere.entity;

import com.sep.treksphere.enums.blog.CommentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "blog_comment")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class BlogComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID commentID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private BlogComment parentComment;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentStatus status = CommentStatus.ACTIVE;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BlogComment> replies = new HashSet<>();
}