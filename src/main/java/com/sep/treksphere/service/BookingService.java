package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.BookingCancelRequest;
import com.sep.treksphere.dto.request.BookingRequest;
import com.sep.treksphere.dto.request.VendorBookingFilterRequest;
import com.sep.treksphere.dto.response.BookingDetailResponse;
import com.sep.treksphere.dto.response.BookingResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.enums.booking.BookingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface BookingService {
    PaginationResponse<BookingResponse> getMyBookingHistory(String email, BookingStatus status, Pageable pageable);

    BookingDetailResponse getBookingDetail(String email, UUID bookingId);

    BookingDetailResponse createBooking(String email, String idempotencyKey, BookingRequest request);

    BookingDetailResponse cancelBooking(String email, UUID bookingId, BookingCancelRequest request);

    // Vendor Booking Management
    PaginationResponse<BookingResponse> getVendorBookings(String email, VendorBookingFilterRequest request);

    BookingDetailResponse confirmVendorBooking(String email, UUID bookingId);
}
