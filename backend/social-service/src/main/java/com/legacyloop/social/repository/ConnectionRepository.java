package com.legacyloop.social.repository;

import com.legacyloop.social.entity.Connection;
import com.legacyloop.social.entity.Connection.Status;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ConnectionRepository extends MongoRepository<Connection, String> {

    Page<Connection> findByAddresseeIdAndStatus(Long addresseeId, Status status, Pageable pageable);

    Page<Connection> findByRequesterIdAndStatus(Long requesterId, Status status, Pageable pageable);

    /** Every connection involving this person, whichever side they are on. */
    @Query("{ 'status': ?1, $or: [ { 'requesterId': ?0 }, { 'addresseeId': ?0 } ] }")
    Page<Connection> findAllInvolving(Long userId, Status status, Pageable pageable);

    @Query("{ 'status': ?1, $or: [ { 'requesterId': ?0 }, { 'addresseeId': ?0 } ] }")
    List<Connection> findAllInvolving(Long userId, Status status);

    /** The pair in either direction — used before creating a request, so duplicates cannot happen. */
    @Query("{ $or: [ { 'requesterId': ?0, 'addresseeId': ?1 }, { 'requesterId': ?1, 'addresseeId': ?0 } ] }")
    Optional<Connection> findBetween(Long firstUserId, Long secondUserId);

    long countByAddresseeIdAndStatus(Long addresseeId, Status status);
}
