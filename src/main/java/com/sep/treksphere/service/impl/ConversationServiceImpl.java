package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.ConversationRepository;
import com.sep.treksphere.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
}
