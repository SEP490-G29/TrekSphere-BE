package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.BookingCancelRequest;
import com.sep.treksphere.dto.response.CancellationQuoteResponse;
import com.sep.treksphere.dto.request.VendorBookingCancelRequest;
import com.sep.treksphere.entity.Booking;

import java.util.UUID;

public interface CancellationService {
    CancellationQuoteResponse quoteForTrekker(String email, UUID bookingId);
    Booking cancelByTrekker(String email, UUID bookingId, BookingCancelRequest request);
    Booking cancelByVendor(String email, UUID bookingId, VendorBookingCancelRequest request);
}
