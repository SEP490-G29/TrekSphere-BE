package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.NotificationRepository;
import com.sep.treksphere.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
}
