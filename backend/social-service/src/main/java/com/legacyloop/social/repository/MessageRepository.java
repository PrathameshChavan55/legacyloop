package com.legacyloop.social.repository;

import com.legacyloop.social.entity.Message;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<Message, String> {

    Page<Message> findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(String conversationId,
                                                                          Pageable pageable);

    List<Message> findByConversationIdAndSenderIdNotAndReadAtIsNull(String conversationId, Long readerId);
}
