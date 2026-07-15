package com.sep.treksphere.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tour_checkpoint")
@Getter
@Setter
@NoArgsConstructor
public class TourCheckpoint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID checkpointId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @Column(nullable = false, length = 255)
    private String checkpointName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(precision = 7, scale = 1)
    private BigDecimal altitude;

    @Column(nullable = false)
    private Integer checkpointOrder;

    @Column(length = 500)
    private String checkpointImageUrl;
}
