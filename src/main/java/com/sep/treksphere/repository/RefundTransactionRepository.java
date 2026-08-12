package com.sep.treksphere.repository;

import com.sep.treksphere.entity.RefundTransaction;
import com.sep.treksphere.enums.booking.RefundStatus;
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

public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, UUID> {
    List<RefundTransaction> findByBooking_BookingIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefundTransaction r join fetch r.booking b join fetch r.paymentTransaction p " +
            "join fetch p.vendorPaymentAccount where r.refundTransactionId = :refundId and r.isDeleted = false")
    Optional<RefundTransaction> findByIdForUpdate(@Param("refundId") UUID refundId);

    @Query("select coalesce(sum(r.amount), 0) from RefundTransaction r " +
            "where r.booking.bookingId = :bookingId and r.status in :statuses and r.isDeleted = false")
    BigDecimal sumByBookingAndStatuses(@Param("bookingId") UUID bookingId,
                                       @Param("statuses") Collection<RefundStatus> statuses);

    @Query("select coalesce(sum(r.amount), 0) from RefundTransaction r " +
            "where r.paymentTransaction.paymentTransactionId = :paymentId and r.status in :statuses and r.isDeleted = false")
    BigDecimal sumByPaymentAndStatuses(@Param("paymentId") UUID paymentId,
                                       @Param("statuses") Collection<RefundStatus> statuses);

    List<RefundTransaction> findTop100ByStatusAndGatewayRefundIdIsNotNullAndIsDeletedFalseOrderByProcessingAtAsc(
            RefundStatus status);
}
