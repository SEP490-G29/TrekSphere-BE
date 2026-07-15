package com.sep.treksphere.entity;

import com.sep.treksphere.enums.tour.SessionCheckpointLogStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "session_checkpoint_log")
@Getter
@Setter
@NoArgsConstructor
public class SessionCheckpointLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID sessionCheckpointLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_session_id")
    private TourSession tourSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkpoint_id")
    private TourCheckpoint checkpoint;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SessionCheckpointLogStatus status = SessionCheckpointLogStatus.PENDING;

    private LocalDateTime reachedAt;

    @Column(precision = 10, scale = 7)
    private BigDecimal actualLatitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal actualLongitude;

    @Column(columnDefinition = "TEXT")
    private String note;
}
