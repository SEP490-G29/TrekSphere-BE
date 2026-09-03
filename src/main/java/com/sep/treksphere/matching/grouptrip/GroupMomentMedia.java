package com.sep.treksphere.matching.grouptrip;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_moment_media", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_moment_id", "sort_order"})
})
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GroupMomentMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupMomentMediaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_moment_id", nullable = false)
    private GroupMoment groupMoment;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Integer sortOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
