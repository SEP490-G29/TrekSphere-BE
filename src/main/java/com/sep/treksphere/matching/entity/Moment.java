package com.sep.treksphere.matching.entity;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.enums.MomentStatus;
import com.sep.treksphere.matching.enums.MomentVisibility;
import com.sep.treksphere.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "moment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Moment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID momentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id", nullable = false)
    private User authorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_group_id")
    private MatchingGroup matchingGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_matching_member_id")
    private MatchingMember authorMatchingMember;

    @Column(columnDefinition = "TEXT")
    private String caption;

    private LocalDateTime capturedAt;

    @Column(length = 200)
    private String placeName;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MomentVisibility visibility = MomentVisibility.GROUP_ONLY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MomentStatus status = MomentStatus.VISIBLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hidden_by_user_id")
    private User hiddenByUser;

    @Column(columnDefinition = "TEXT")
    private String hiddenReason;

    @OneToMany(mappedBy = "moment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<MomentMedia> mediaList = new ArrayList<>();

    public void addMedia(MomentMedia media) {
        mediaList.add(media);
        media.setMoment(this);
    }

    public void removeMedia(MomentMedia media) {
        mediaList.remove(media);
        media.setMoment(null);
    }
}
