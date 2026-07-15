package com.sep.treksphere.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "porter_schedule")
@Getter
@Setter
@NoArgsConstructor
public class PorterSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID porterScheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_session_id")
    private TourSession tourSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "porter_id")
    private PorterProfile porter;

    @Column(columnDefinition = "TEXT")
    private String note;
}
