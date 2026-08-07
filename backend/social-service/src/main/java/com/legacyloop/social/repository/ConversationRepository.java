package com.legacyloop.social.repository;

import com.legacyloop.social.entity.Conversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    Page<Conversation> findByParticipantIdsContainingOrderByLastMessageAtDesc(Long userId, Pageable pageable);

    List<Conversation> findByParticipantIdsContaining(Long userId);

    /**
     * The thread between two people.
     *
     * <p>Written as an explicit {@code $all} query rather than a derived one: a derived method
     * naming the same field twice builds two criteria on one key, which Mongo rejects.
     */
    @Query("{ 'participantIds': { $all: [?0, ?1] } }")
    Optional<Conversation> findBetween(Long firstUserId, Long secondUserId);
}
