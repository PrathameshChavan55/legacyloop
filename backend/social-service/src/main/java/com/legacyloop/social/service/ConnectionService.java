package com.legacyloop.social.service;

import com.legacyloop.common.ApiException;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.PageResponse;
import com.legacyloop.social.dto.SocialDtos.Author;
import com.legacyloop.social.dto.SocialDtos.ConnectionRequestBody;
import com.legacyloop.social.dto.SocialDtos.ConnectionResponse;
import com.legacyloop.social.dto.SocialDtos.NetworkSummary;
import com.legacyloop.social.entity.Connection;
import com.legacyloop.social.entity.Connection.Status;
import com.legacyloop.social.repository.ConnectionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Connection requests and the resulting network.
 *
 * <p>One document covers both: accepting a request updates its status rather than copying it into
 * a second collection, so there is no window in which a request and a connection disagree.
 */
@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connections;
    private final NotificationService notifications;
    private final PeopleClient people;
    private final ChatService chatService;

    public ConnectionResponse request(ConnectionRequestBody request, AuthUser requester) {
        if (request.userId().equals(requester.id())) {
            throw ApiException.badRequest("You cannot connect with yourself");
        }

        connections.findBetween(requester.id(), request.userId()).ifPresent(existing -> {
            if (existing.getStatus() == Status.ACCEPTED) {
                throw ApiException.conflict("You are already connected");
            }
            if (existing.getStatus() == Status.PENDING) {
                throw ApiException.conflict("There is already a pending request between you");
            }
        });

        Connection connection = connections.save(Connection.builder()
                .requesterId(requester.id())
                .addresseeId(request.userId())
                .message(request.message())
                .createdAt(Instant.now())
                .build());

        notifications.create(request.userId(), "CONNECTION_REQUEST", "New connection request",
                "%s would like to connect.".formatted(requester.fullName()), "/network");

        return ConnectionResponse.from(connection, requester.id(), people.author(request.userId()));
    }

    public ConnectionResponse accept(String connectionId, AuthUser user) {
        Connection connection = loadPendingFor(connectionId, user.id());
        connection.setStatus(Status.ACCEPTED);
        connection.setRespondedAt(Instant.now());
        connections.save(connection);

        // Ensure chat conversation thread is ready between both users
        try {
            chatService.with(connection.getRequesterId(), user.id());
        } catch (Exception ex) {
            // non-fatal if conversation already exists or creation failed
        }

        notifications.create(connection.getRequesterId(), "CONNECTION_ACCEPTED", "Connection accepted",
                "%s accepted your request.".formatted(user.fullName()), "/network");

        return ConnectionResponse.from(connection, user.id(), people.author(connection.getRequesterId()));
    }

    public ConnectionResponse reject(String connectionId, Long userId) {
        Connection connection = loadPendingFor(connectionId, userId);
        connection.setStatus(Status.REJECTED);
        connection.setRespondedAt(Instant.now());
        connections.save(connection);
        return ConnectionResponse.from(connection, userId, people.author(connection.getRequesterId()));
    }

    public ConnectionResponse withdraw(String connectionId, Long userId) {
        Connection connection = connections.findById(connectionId)
                .orElseThrow(() -> ApiException.notFound("Connection request", connectionId));
        if (!connection.getRequesterId().equals(userId)) {
            throw ApiException.forbidden("You can only withdraw a request you sent");
        }
        if (connection.getStatus() != Status.PENDING) {
            throw ApiException.badRequest("That request has already been answered");
        }
        connection.setStatus(Status.WITHDRAWN);
        connection.setRespondedAt(Instant.now());
        connections.save(connection);
        return ConnectionResponse.from(connection, userId, people.author(connection.getAddresseeId()));
    }

    /** Removing a connection deletes the document — there is no history worth keeping here. */
    public void remove(String connectionId, Long userId) {
        Connection connection = connections.findById(connectionId)
                .orElseThrow(() -> ApiException.notFound("Connection", connectionId));
        if (!connection.getRequesterId().equals(userId) && !connection.getAddresseeId().equals(userId)) {
            throw ApiException.forbidden("That is not your connection");
        }
        connections.delete(connection);
    }

    public PageResponse<ConnectionResponse> received(Long userId, Pageable pageable) {
        return render(connections.findByAddresseeIdAndStatus(userId, Status.PENDING, pageable), userId);
    }

    public PageResponse<ConnectionResponse> sent(Long userId, Pageable pageable) {
        return render(connections.findByRequesterIdAndStatus(userId, Status.PENDING, pageable), userId);
    }

    public PageResponse<ConnectionResponse> accepted(Long userId, Pageable pageable) {
        return render(connections.findAllInvolving(userId, Status.ACCEPTED, pageable), userId);
    }

    public NetworkSummary summary(Long userId) {
        return new NetworkSummary(
                connections.findAllInvolving(userId, Status.ACCEPTED).size(),
                connections.countByAddresseeIdAndStatus(userId, Status.PENDING),
                connections.findByRequesterIdAndStatus(userId, Status.PENDING, Pageable.ofSize(100))
                        .getTotalElements());
    }

    /**
     * People your connections are connected to, whom you are not.
     *
     * <p>Two hops, computed in memory: at this size a graph query would be more machinery than the
     * answer is worth.
     */
    public List<Long> suggestions(Long userId, int limit) {
        List<Long> direct = connectedUserIds(userId);
        return direct.stream()
                .flatMap(connectionId -> connectedUserIds(connectionId).stream())
                .filter(candidate -> !candidate.equals(userId) && !direct.contains(candidate))
                .distinct()
                .limit(limit)
                .toList();
    }

    /** The ids this person is connected to. Used by the feed as well as the network screen. */
    public List<Long> connectedUserIds(Long userId) {
        return connections.findAllInvolving(userId, Status.ACCEPTED).stream()
                .map(connection -> connection.otherParty(userId))
                .toList();
    }

    private PageResponse<ConnectionResponse> render(Page<Connection> page, Long viewerId) {
        Map<Long, Author> authors = people.authors(
                page.getContent().stream().map(connection -> connection.otherParty(viewerId)).toList());
        return PageResponse.of(page, connection -> ConnectionResponse.from(connection, viewerId,
                people.authorOrUnknown(authors, connection.otherParty(viewerId))));
    }

    private Connection loadPendingFor(String connectionId, Long addresseeId) {
        Connection connection = connections.findById(connectionId)
                .orElseThrow(() -> ApiException.notFound("Connection request", connectionId));
        if (!connection.getAddresseeId().equals(addresseeId)) {
            throw ApiException.forbidden("That request was not sent to you");
        }
        if (connection.getStatus() != Status.PENDING) {
            throw ApiException.badRequest("You have already answered that request");
        }
        return connection;
    }
}
