package com.sep.treksphere.entity;

import com.sep.treksphere.enums.system.PolicyDataType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "system_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID policyID;

    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyDataType dataType;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedByUser;
}
