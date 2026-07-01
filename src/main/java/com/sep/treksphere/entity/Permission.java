package com.sep.treksphere.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "permission", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"resource", "action"})
})
@Getter
@Setter
@NoArgsConstructor


public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID permissionID;

    @Column(nullable = false, length = 100)
    private String resource;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 255)
    private String description;
}
