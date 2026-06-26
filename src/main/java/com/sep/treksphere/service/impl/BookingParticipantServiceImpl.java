package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.BookingParticipantRepository;
import com.sep.treksphere.service.BookingParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingParticipantServiceImpl implements BookingParticipantService {

    private final BookingParticipantRepository bookingParticipantRepository;
}
