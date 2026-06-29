package com.sep.treksphere.entity;

import com.sep.treksphere.enums.blog.ReportStatus;
import com.sep.treksphere.enums.blog.TargetType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "report_content")
@Getter
@Setter
@NoArgsConstructor


public class ReportContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reportID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private User reporterUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TargetType targetType;

    @Column(nullable = false)
    private UUID targetID;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String moderationDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by_user_id")
    private User moderatedByUser;

    @Column(nullable = false, updatable = false)
    private LocalDateTime reportedAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        this.reportedAt = LocalDateTime.now();
    }
}
