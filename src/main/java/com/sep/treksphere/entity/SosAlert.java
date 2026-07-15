package com.sep.treksphere.entity;

import com.sep.treksphere.enums.tour.SosAlertStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sos_alert")
@Getter
@Setter
@NoArgsConstructor
public class SosAlert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID sosAlertId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_session_id")
    private TourSession tourSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal longitude;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SosAlertStatus status = SosAlertStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;
}
