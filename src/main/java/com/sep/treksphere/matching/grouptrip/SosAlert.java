package com.sep.treksphere.matching.grouptrip;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.user.User;
import com.sep.treksphere.matching.grouptrip.IncidentType;
import com.sep.treksphere.matching.grouptrip.SosAlertStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sos_alert", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_trip_id", "sender_id", "idempotency_key"})
})
@Getter
@Setter
@NoArgsConstructor
public class SosAlert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID sosAlertId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_trip_id", nullable = false)
    private GroupTrip groupTrip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IncidentType incidentTypeCode;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SosAlertStatus status = SosAlertStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(nullable = false, length = 255)
    private String idempotencyKey;
}
