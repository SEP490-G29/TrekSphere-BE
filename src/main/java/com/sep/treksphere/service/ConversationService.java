package com.sep.treksphere.service;

import com.sep.treksphere.dto.response.ConversationResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;

public interface ConversationService {

    PaginationResponse<ConversationResponse> getConversations(
            int page,
            int size,
            CustomUserDetails userDetails
    );
}
