package com.legacyloop.feed.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One row per pair of people, in whichever direction the request was first sent.
 *
 * <p>{@code pairKey} is the two ids sorted and joined ("7:19") - the same trick
 * {@link ChatMessage} uses for conversations. It makes "are these two already connected, in
 * either direction?" a single indexed equality lookup instead of the four-way OR the .NET
 * build ran on every request.</p>
 */
@Document("connections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "recipient_status", def = "{'recipientId': 1, 'status': 1}")
@CompoundIndex(name = "requester_status", def = "{'requesterId': 1, 'status': 1}")
public class Connection {

    public static final String PENDING = "PENDING";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String REJECTED = "REJECTED";

    @Id
    private String id;

    /** Sorted pair key, unique: one connection record per pair, ever. */
    @Indexed(unique = true)
    private String pairKey;

    private Long requesterId;

    private Long recipientId;

    /** PENDING, ACCEPTED or REJECTED. */
    private String status;

    @CreatedDate
    private Instant createdAt;

    private Instant respondedAt;

    public static String pairKey(Long a, Long b) {
        return a < b ? a + ":" + b : b + ":" + a;
    }

    public boolean isAccepted() {
        return ACCEPTED.equals(status);
    }
}
