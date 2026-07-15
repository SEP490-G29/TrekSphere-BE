package com.sep.treksphere.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "coordinator_schedule")
@Getter
@Setter
@NoArgsConstructor
public class CoordinatorSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID coordinatorScheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_session_id")
    private TourSession tourSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinator_id")
    private User coordinator;

    @Column(name = "is_lead", nullable = false)
    private Boolean isLead = false;
}
