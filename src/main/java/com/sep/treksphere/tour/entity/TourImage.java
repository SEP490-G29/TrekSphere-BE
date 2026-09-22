package com.sep.treksphere.tour.image;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.tour.Tour;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tour_image")
@Getter
@Setter
@NoArgsConstructor


public class TourImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tourImageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(length = 255)
    private String caption;

}
