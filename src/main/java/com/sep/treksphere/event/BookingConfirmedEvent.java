package com.sep.treksphere.event;

import com.sep.treksphere.dto.email.BookingConfirmationEmailData;

public record BookingConfirmedEvent(BookingConfirmationEmailData emailData) {
}
