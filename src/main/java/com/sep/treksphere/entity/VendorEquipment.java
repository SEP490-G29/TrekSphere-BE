package com.sep.treksphere.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "vendor_equipment")
@Getter
@Setter
@NoArgsConstructor
public class VendorEquipment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID equipmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Column(nullable = false, length = 255)
    private String equipmentName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column( nullable = false)
    private Integer totalQuantity = 0;
}
