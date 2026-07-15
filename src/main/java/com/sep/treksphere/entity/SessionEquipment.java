package com.sep.treksphere.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "session_equipment")
@Getter
@Setter
@NoArgsConstructor
public class SessionEquipment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID sessionEquipmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_session_id")
    private TourSession tourSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private VendorEquipment equipment;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false)
    private Boolean isChecked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by")
    private User checkedBy;

    @Column(columnDefinition = "TEXT")
    private String note;
}
