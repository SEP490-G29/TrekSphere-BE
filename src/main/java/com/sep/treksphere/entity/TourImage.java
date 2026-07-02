package com.sep.treksphere.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tour_image")
@Getter
@Setter
@NoArgsConstructor


public class TourImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID imageID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(length = 255)
    private String caption;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
