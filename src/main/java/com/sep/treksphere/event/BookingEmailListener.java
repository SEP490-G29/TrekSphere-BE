package com.sep.treksphere.event;

import com.sep.treksphere.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEmailListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendBookingConfirmation(BookingConfirmedEvent event) {
        try {
            emailService.sendBookingConfirmationEmail(event.emailData());
        } catch (RuntimeException exception) {
            log.error("Could not send booking confirmation email for booking {}",
                    event.emailData().bookingCode(), exception);
        }
    }
}
