package com.legacyloop.social.service;

import com.legacyloop.common.ApiException;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.PageResponse;
import com.legacyloop.social.dto.SocialDtos.Author;
import com.legacyloop.social.dto.SocialDtos.CommentRequest;
import com.legacyloop.social.dto.SocialDtos.CommentResponse;
import com.legacyloop.social.dto.SocialDtos.HashtagCount;
import com.legacyloop.social.dto.SocialDtos.PostRequest;
import com.legacyloop.social.dto.SocialDtos.PostResponse;
import com.legacyloop.social.entity.Comment;
import com.legacyloop.social.entity.Post;
import com.legacyloop.social.repository.CommentRepository;
import com.legacyloop.social.repository.PostRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Posts, comments, reactions, saves and hashtags.
 *
 * <p>Hashtags are derived from the post body when it is written and stored on the document, so
 * "posts tagged #java" is an indexed lookup and there is no hashtag collection to keep in step.
 * Trending is counted over recent posts on demand.
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private static final Pattern HASHTAG = Pattern.compile("#(\\w{2,40})");
    private static final Set<String> REACTION_TYPES = Set.of("LIKE", "CELEBRATE", "SUPPORT", "INSIGHTFUL");

    private final PostRepository posts;
    private final CommentRepository comments;
    private final ConnectionService connections;
    private final NotificationService notifications;
    private final PeopleClient people;

    /* ------------------------------------------------------------------------------ posts */

    public PostResponse create(PostRequest request, AuthUser author) {
        Post post = posts.save(Post.builder()
                .authorId(author.id())
                .content(request.content())
                .imageUrls(request.imageUrls())
                .linkUrl(request.linkUrl())
                .hashtags(extractHashtags(request.content()))
                .createdAt(Instant.now())
                .build());

        return PostResponse.from(post, new Author(author.id(), author.fullName(), null, null), author.id());
    }

    /** Your posts and your connections' — or all platform posts for admins and staff. */
    public PageResponse<PostResponse> feed(AuthUser viewer, Pageable pageable) {
        if (viewer.isAdmin() || viewer.isStaff()) {
            return explore(viewer.id(), pageable);
        }
        Set<Long> authorIds = new LinkedHashSet<>(connections.connectedUserIds(viewer.id()));
        authorIds.add(viewer.id());
        return render(posts.findByAuthorIdInAndDeletedFalseOrderByCreatedAtDesc(authorIds, pageable), viewer.id());
    }

    /** Everything, for discovery. */
    public PageResponse<PostResponse> explore(Long viewerId, Pageable pageable) {
        return render(posts.findByDeletedFalseOrderByCreatedAtDesc(pageable), viewerId);
    }

    public PageResponse<PostResponse> search(String query, Long viewerId, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return explore(viewerId, pageable);
        }
        return render(posts.search(Pattern.quote(query.trim()), pageable), viewerId);
    }

    public PageResponse<PostResponse> byAuthor(Long authorId, Long viewerId, Pageable pageable) {
        return render(posts.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(authorId, pageable), viewerId);
    }

    public PageResponse<PostResponse> byHashtag(String tag, Long viewerId, Pageable pageable) {
        String normalised = tag.replace("#", "").toLowerCase(Locale.ROOT);
        return render(posts.findByHashtagsContainingAndDeletedFalseOrderByCreatedAtDesc(normalised, pageable),
                viewerId);
    }

    public PageResponse<PostResponse> saved(Long viewerId, Pageable pageable) {
        return render(posts.findBySavedByContainingAndDeletedFalseOrderByCreatedAtDesc(viewerId, pageable),
                viewerId);
    }

    public PostResponse findById(String postId, Long viewerId) {
        Post post = load(postId);
        return PostResponse.from(post, people.author(post.getAuthorId()), viewerId);
    }

    public PostResponse update(String postId, PostRequest request, Long editorId) {
        Post post = load(postId);
        requireAuthor(post, editorId);

        post.setContent(request.content());
        post.setImageUrls(request.imageUrls());
        post.setLinkUrl(request.linkUrl());
        post.setHashtags(extractHashtags(request.content()));
        post.setEdited(true);
        post.setUpdatedAt(Instant.now());

        return PostResponse.from(posts.save(post), people.author(post.getAuthorId()), editorId);
    }

    /** Soft delete: the comments underneath still reference the post. */
    public void delete(String postId, AuthUser user) {
        Post post = load(postId);
        if (!user.isAdmin()) {
            requireAuthor(post, user.id());
        }
        post.setDeleted(true);
        posts.save(post);
    }

    /* -------------------------------------------------------------------------- reactions */

    /** Reacting again with the same type removes it, which is what a toggle should do. */
    public PostResponse react(String postId, String type, Long userId) {
        String reaction = type == null ? "LIKE" : type.trim().toUpperCase(Locale.ROOT);
        if (!REACTION_TYPES.contains(reaction)) {
            throw ApiException.badRequest("Unknown reaction: " + type);
        }

        Post post = load(postId);
        String key = String.valueOf(userId);
        if (reaction.equals(post.getReactions().get(key))) {
            post.getReactions().remove(key);
        } else {
            post.getReactions().put(key, reaction);
            if (!post.getAuthorId().equals(userId)) {
                notifications.create(post.getAuthorId(), "POST_REACTION", "New reaction",
                        "Someone reacted to your post.", "/feed/" + postId);
            }
        }
        return PostResponse.from(posts.save(post), people.author(post.getAuthorId()), userId);
    }

    public PostResponse toggleSave(String postId, Long userId) {
        Post post = load(postId);
        if (!post.getSavedBy().add(userId)) {
            post.getSavedBy().remove(userId);
        }
        return PostResponse.from(posts.save(post), people.author(post.getAuthorId()), userId);
    }

    /* --------------------------------------------------------------------------- comments */

    public CommentResponse comment(String postId, CommentRequest request, AuthUser author) {
        Post post = load(postId);

        Comment comment = comments.save(Comment.builder()
                .postId(postId)
                .authorId(author.id())
                .content(request.content())
                .createdAt(Instant.now())
                .build());

        post.setCommentCount(post.getCommentCount() + 1);
        posts.save(post);

        if (!post.getAuthorId().equals(author.id())) {
            notifications.create(post.getAuthorId(), "POST_COMMENT", "New comment",
                    "%s commented on your post.".formatted(author.fullName()), "/feed/" + postId);
        }
        return CommentResponse.from(comment, new Author(author.id(), author.fullName(), null, null),
                author.id());
    }

    public CommentResponse reply(String commentId, CommentRequest request, AuthUser author) {
        Comment parent = comments.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Comment", commentId));

        Comment reply = comments.save(Comment.builder()
                .postId(parent.getPostId())
                .parentId(commentId)
                .authorId(author.id())
                .content(request.content())
                .createdAt(Instant.now())
                .build());

        parent.setReplyCount(parent.getReplyCount() + 1);
        comments.save(parent);

        if (!parent.getAuthorId().equals(author.id())) {
            notifications.create(parent.getAuthorId(), "COMMENT_REPLY", "New reply",
                    "%s replied to your comment.".formatted(author.fullName()),
                    "/feed/" + parent.getPostId());
        }
        return CommentResponse.from(reply, new Author(author.id(), author.fullName(), null, null),
                author.id());
    }

    public PageResponse<CommentResponse> comments(String postId, Long viewerId, Pageable pageable) {
        Page<Comment> page = comments
                .findByPostIdAndParentIdIsNullAndDeletedFalseOrderByCreatedAtDesc(postId, pageable);
        return renderComments(page, viewerId);
    }

    public PageResponse<CommentResponse> replies(String commentId, Long viewerId, Pageable pageable) {
        return renderComments(comments.findByParentIdAndDeletedFalseOrderByCreatedAtAsc(commentId, pageable),
                viewerId);
    }

    public void deleteComment(String commentId, AuthUser user) {
        Comment comment = comments.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Comment", commentId));
        if (!user.isAdmin() && !comment.getAuthorId().equals(user.id())) {
            throw ApiException.forbidden("You can only delete your own comments");
        }
        comment.setDeleted(true);
        comments.save(comment);

        posts.findById(comment.getPostId()).ifPresent(post -> {
            post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
            posts.save(post);
        });
    }

    /* --------------------------------------------------------------------------- hashtags */

    /** Counted over the most recent posts rather than kept in a collection with a counter. */
    public List<HashtagCount> trending(int limit) {
        Map<String, Long> counts = new HashMap<>();
        posts.findTop50ByDeletedFalseOrderByCreatedAtDesc().forEach(post -> {
            if (post.getHashtags() != null) {
                post.getHashtags().forEach(tag -> counts.merge(tag, 1L, Long::sum));
            }
        });
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new HashtagCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<HashtagCount> suggestHashtags(String query) {
        String needle = query == null ? "" : query.replace("#", "").toLowerCase(Locale.ROOT);
        return trending(50).stream()
                .filter(tag -> tag.tag().contains(needle))
                .limit(10)
                .toList();
    }

    /* -------------------------------------------------------------------------- internals */

    /** Resolves every author on the page in one call, then maps. */
    private PageResponse<PostResponse> render(Page<Post> page, Long viewerId) {
        Map<Long, Author> authors = people.authors(page.getContent().stream().map(Post::getAuthorId).toList());
        return PageResponse.of(page, post ->
                PostResponse.from(post, people.authorOrUnknown(authors, post.getAuthorId()), viewerId));
    }

    private PageResponse<CommentResponse> renderComments(Page<Comment> page, Long viewerId) {
        Map<Long, Author> authors = people.authors(
                page.getContent().stream().map(Comment::getAuthorId).toList());
        return PageResponse.of(page, comment ->
                CommentResponse.from(comment, people.authorOrUnknown(authors, comment.getAuthorId()), viewerId));
    }

    private static Set<String> extractHashtags(String content) {
        Set<String> tags = new LinkedHashSet<>();
        Matcher matcher = HASHTAG.matcher(content == null ? "" : content);
        while (matcher.find()) {
            tags.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return tags;
    }

    private Post load(String postId) {
        return posts.findById(postId)
                .filter(post -> !post.isDeleted())
                .orElseThrow(() -> ApiException.notFound("Post", postId));
    }

    private static void requireAuthor(Post post, Long userId) {
        if (!post.getAuthorId().equals(userId)) {
            throw ApiException.forbidden("You can only change your own posts");
        }
    }
}
