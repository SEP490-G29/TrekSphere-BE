package com.sep.treksphere.report;

import com.sep.treksphere.blog.Blog;
import com.sep.treksphere.blog.comment.BlogComment;
import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.user.User;
import com.sep.treksphere.report.ReportStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "report_content")
@Getter
@Setter
@NoArgsConstructor


public class ReportContent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reportContentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id")
    private Blog blog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_comment_id")
    private BlogComment blogComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @Column(nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;
}
