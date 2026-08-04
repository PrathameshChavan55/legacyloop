package com.legacyloop.feed.repository;

import com.legacyloop.feed.document.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    Page<ChatMessage> findByConversationIdOrderBySentAtDesc(String conversationId, Pageable pageable);

    List<ChatMessage> findBySenderIdOrRecipientIdOrderBySentAtDesc(Long senderId, Long recipientId);

    long countByRecipientIdAndReadFalse(Long recipientId);

    /**
     * Every message this person is part of, newest first. The conversation list is built
     * from this in memory: one query instead of an aggregation pipeline, which is the right
     * trade at the volume a single institution produces. If a thread ever grows past a few
     * thousand messages per person, this becomes a $group aggregation.
     */
    @Query("{ $or: [ { 'senderId': ?0 }, { 'recipientId': ?0 } ] }")
    List<ChatMessage> findAllInvolving(Long userId, org.springframework.data.domain.Sort sort);
}
