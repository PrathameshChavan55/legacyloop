package com.legacyloop.social.dto;

import com.legacyloop.social.entity.Comment;
import com.legacyloop.social.entity.Connection;
import com.legacyloop.social.entity.Notification;
import com.legacyloop.social.entity.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Every request and response shape for this service, in one file.
 *
 * <p>The feed, the network and chat are one screen area to a user, and their DTOs are small
 * records; the original spread the same shapes over eleven files in three packages.
 *
 * <p>Author names are passed into the factories rather than looked up inside them, so a page of
 * twenty posts resolves names in one call to user-service instead of twenty.
 */
public final class SocialDtos {

    private SocialDtos() {
    }

    /* ------------------------------------------------------------------------------ feed */

    public record PostRequest(@NotBlank @Size(max = 5000) String content,
                              @Size(max = 4) List<@Size(max = 500) String> imageUrls,
                              @Size(max = 500) String linkUrl) {
    }

    public record CommentRequest(@NotBlank @Size(max = 2000) String content) {
    }

    public record ReactionRequest(@NotBlank String type) {
    }

    public record Author(Long id, String name, String headline, String photoUrl) {
    }

    public record PostResponse(String id, Author author, String content, List<String> imageUrls,
                               String linkUrl, List<String> hashtags, int reactionCount,
                               Map<String, Long> reactionBreakdown, String myReaction, boolean saved,
                               int commentCount, boolean edited, boolean mine, Instant createdAt) {

        public static PostResponse from(Post post, Author author, Long viewerId) {
            return new PostResponse(post.getId(), author, post.getContent(),
                    post.getImageUrls() == null ? List.of() : post.getImageUrls(), post.getLinkUrl(),
                    post.getHashtags() == null ? List.of() : List.copyOf(post.getHashtags()),
                    post.reactionCount(), post.reactionBreakdown(), post.reactionOf(viewerId),
                    post.isSavedBy(viewerId), post.getCommentCount(), post.isEdited(),
                    post.getAuthorId().equals(viewerId), post.getCreatedAt());
        }
    }

    public record CommentResponse(String id, String postId, String parentId, Author author, String content,
                                  int replyCount, boolean mine, Instant createdAt) {

        public static CommentResponse from(Comment comment, Author author, Long viewerId) {
            return new CommentResponse(comment.getId(), comment.getPostId(), comment.getParentId(), author,
                    comment.getContent(), comment.getReplyCount(),
                    comment.getAuthorId().equals(viewerId), comment.getCreatedAt());
        }
    }

    public record HashtagCount(String tag, long count) {
    }

    /* --------------------------------------------------------------------------- network */

    public record ConnectionRequestBody(@NotNull Long userId, @Size(max = 500) String message) {
    }

    public record ConnectionResponse(String id, Long userId, String name, String headline, String photoUrl,
                                     String status, String message, boolean incoming, Instant createdAt,
                                     Instant respondedAt) {

        public static ConnectionResponse from(Connection connection, Long viewerId, Author other) {
            return new ConnectionResponse(connection.getId(), other.id(), other.name(), other.headline(),
                    other.photoUrl(), connection.getStatus().name(), connection.getMessage(),
                    connection.getAddresseeId().equals(viewerId), connection.getCreatedAt(),
                    connection.getRespondedAt());
        }
    }

    public record NetworkSummary(long connections, long pendingReceived, long pendingSent) {
    }

    /* ------------------------------------------------------------------------------ chat */

    public record SendMessageRequest(String conversationId, Long recipientId,
                                     @NotBlank @Size(max = 4000) String content,
                                     @Size(max = 500) String attachmentUrl) {
    }

    public record ConversationResponse(String id, Long otherUserId, String otherUserName,
                                       String otherUserPhotoUrl, String lastMessagePreview,
                                       Long lastMessageSenderId, Instant lastMessageAt, int unread) {
    }

    public record MessageResponse(String id, String conversationId, Long senderId, String senderName,
                                  String content, String attachmentUrl, boolean mine, Instant readAt,
                                  Instant createdAt) {
    }

    /* --------------------------------------------------------------------- notifications */

    public record NotificationResponse(String id, String type, String title, String body, String link,
                                       boolean read, Instant createdAt) {

        public static NotificationResponse from(Notification notification) {
            return new NotificationResponse(notification.getId(), notification.getType(),
                    notification.getTitle(), notification.getBody(), notification.getLink(),
                    notification.isRead(), notification.getCreatedAt());
        }
    }
}
