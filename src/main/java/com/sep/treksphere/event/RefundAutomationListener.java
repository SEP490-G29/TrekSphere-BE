package com.sep.treksphere.event;

import com.sep.treksphere.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefundAutomationListener {

    private final PaymentService paymentService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processRequestedRefund(RefundRequestedEvent event) {
        try {
            paymentService.processRefundAutomatically(event.refundId());
        } catch (RuntimeException exception) {
            log.warn("Automatic refund {} will be retried by scheduler: {}",
                    event.refundId(), exception.getMessage());
        }
    }
}
