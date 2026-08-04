package com.legacyloop.feed.service;

import com.legacyloop.common.dto.PageResponse;
import com.legacyloop.feed.dto.request.SendMessageRequest;
import com.legacyloop.feed.dto.response.ChatMessageResponse;
import com.legacyloop.feed.dto.response.ConversationResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatService {

    ChatMessageResponse send(Long senderId, SendMessageRequest request);

    PageResponse<ChatMessageResponse> history(Long otherUserId, Pageable pageable);

    /** The message list: one row per person you have talked to, newest first. */
    List<ConversationResponse> conversations();

    long unreadCount();
}
