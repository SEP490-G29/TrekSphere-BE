package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.BookingRepository;
import com.sep.treksphere.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
}
