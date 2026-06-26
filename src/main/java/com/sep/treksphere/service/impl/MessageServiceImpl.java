package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.MessageRepository;
import com.sep.treksphere.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
}
