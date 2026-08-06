package com.legacyloop.social.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A connection between two people, in one document for both its states.
 *
 * <p>A pending request and an accepted connection are the same pair of ids with a different
 * status, so the original's separate request and connection collections meant copying a row on
 * acceptance and keeping the two in step. Here acceptance is a field update.
 */
@Document("connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Connection {

    public enum Status { PENDING, ACCEPTED, REJECTED, WITHDRAWN }

    @Id
    private String id;

    @Indexed
    private Long requesterId;

    @Indexed
    private Long addresseeId;

    private String message;

    @Builder.Default
    private Status status = Status.PENDING;

    @CreatedDate
    private Instant createdAt;

    private Instant respondedAt;

    /** The other person, seen from one side of the pair. */
    public Long otherParty(Long userId) {
        return requesterId.equals(userId) ? addresseeId : requesterId;
    }
}
