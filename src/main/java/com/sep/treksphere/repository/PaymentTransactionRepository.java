package com.sep.treksphere.repository;

import com.sep.treksphere.entity.PaymentTransaction;
import com.sep.treksphere.enums.booking.PaymentStage;
import com.sep.treksphere.enums.booking.PaymentTransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    boolean existsByBooking_BookingIdAndIsDeletedFalse(UUID bookingId);

    List<PaymentTransaction> findByBooking_BookingIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID bookingId);

    Optional<PaymentTransaction> findFirstByBooking_BookingIdAndPaymentStageAndStatusInAndIsDeletedFalseOrderByAttemptNumberDesc(
            UUID bookingId, PaymentStage stage, Collection<PaymentTransactionStatus> statuses);

    Optional<PaymentTransaction> findFirstByBooking_BookingIdAndPaymentStageAndIsDeletedFalseOrderByAttemptNumberDesc(
            UUID bookingId, PaymentStage stage);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentTransaction p join fetch p.booking b join fetch p.vendorPaymentAccount " +
            "where p.gatewayOrderCode = :orderCode and p.isDeleted = false")
    Optional<PaymentTransaction> findByGatewayOrderCodeForUpdate(@Param("orderCode") Long orderCode);

    Optional<PaymentTransaction> findByGatewayOrderCodeAndIsDeletedFalse(Long orderCode);

    @Query(value = "select nextval('gateway_order_code_seq')", nativeQuery = true)
    Long nextGatewayOrderCode();

    @Query("select coalesce(sum(p.paidAmount), 0) from PaymentTransaction p " +
            "where p.booking.bookingId = :bookingId " +
            "and p.status = com.sep.treksphere.enums.booking.PaymentTransactionStatus.PAID " +
            "and p.isDeleted = false")
    BigDecimal sumPaidByBooking(@Param("bookingId") UUID bookingId);
}
