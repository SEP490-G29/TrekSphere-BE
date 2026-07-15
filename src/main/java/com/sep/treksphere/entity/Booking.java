package com.sep.treksphere.entity;

import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.booking.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "booking")
@Getter
@Setter
@NoArgsConstructor


public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID bookingId;

    @Column(nullable = false, unique = true, length = 50)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_schedule_id", nullable = false)
    private TourSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(nullable = false)
    private Integer numberOfParticipants;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BookingStatus bookingStatus = BookingStatus.PENDING;

    @Column(length = 500)
    private String proofImageUrl;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(precision = 12, scale = 2)
    private BigDecimal refundAmount;

    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BookingParticipant> participants = new HashSet<>();
}
