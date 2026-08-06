package com.legacyloop.social.controller;

import com.legacyloop.common.ApiResponse;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.PageResponse;
import com.legacyloop.social.dto.SocialDtos.CommentRequest;
import com.legacyloop.social.dto.SocialDtos.CommentResponse;
import com.legacyloop.social.dto.SocialDtos.HashtagCount;
import com.legacyloop.social.dto.SocialDtos.PostRequest;
import com.legacyloop.social.dto.SocialDtos.PostResponse;
import com.legacyloop.social.dto.SocialDtos.ReactionRequest;
import com.legacyloop.social.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Feed", description = "Posts, comments, reactions, saves and hashtags")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class FeedController {

    private final PostService postService;

    @PostMapping
    @Operation(summary = "Write a post; hashtags in the text are indexed automatically")
    public ApiResponse<PostResponse> create(@AuthenticationPrincipal AuthUser user,
                                            @Valid @RequestBody PostRequest request) {
        return ApiResponse.ok(postService.create(request, user), "Posted");
    }

    @GetMapping("/feed")
    @Operation(summary = "Your feed: your posts and your connections (or all posts for staff/admin)'")
    public ApiResponse<PageResponse<PostResponse>> feed(@AuthenticationPrincipal AuthUser user,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(postService.feed(user, PageRequest.of(page, Math.min(size, 30))));
    }

    @GetMapping("/search")
    @Operation(summary = "Search post text, or browse everything when the query is empty")
    public ApiResponse<PageResponse<PostResponse>> search(@AuthenticationPrincipal AuthUser user,
                                                          @RequestParam(required = false) String query,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(postService.search(query, user.id(), PageRequest.of(page, Math.min(size, 30))));
    }

    @GetMapping("/saved")
    @Operation(summary = "Posts you saved")
    public ApiResponse<PageResponse<PostResponse>> saved(@AuthenticationPrincipal AuthUser user,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(postService.saved(user.id(), PageRequest.of(page, Math.min(size, 30))));
    }

    @GetMapping("/hashtags/trending")
    @Operation(summary = "The tags people are using right now")
    public ApiResponse<List<HashtagCount>> trending(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(postService.trending(Math.min(limit, 30)));
    }

    @GetMapping("/hashtags/suggest")
    @Operation(summary = "Autocomplete for the tag box")
    public ApiResponse<List<HashtagCount>> suggestHashtags(@RequestParam String query) {
        return ApiResponse.ok(postService.suggestHashtags(query));
    }

    @GetMapping("/hashtag/{tag}")
    @Operation(summary = "Posts carrying one tag")
    public ApiResponse<PageResponse<PostResponse>> byHashtag(@AuthenticationPrincipal AuthUser user,
                                                             @PathVariable String tag,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(postService.byHashtag(tag, user.id(), PageRequest.of(page, Math.min(size, 30))));
    }

    @GetMapping("/author/{authorId}")
    @Operation(summary = "Everything one person has posted")
    public ApiResponse<PageResponse<PostResponse>> byAuthor(@AuthenticationPrincipal AuthUser user,
                                                            @PathVariable Long authorId,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(postService.byAuthor(authorId, user.id(),
                PageRequest.of(page, Math.min(size, 30))));
    }

    @GetMapping("/comments/{commentId}/replies")
    @Operation(summary = "Replies to one comment")
    public ApiResponse<PageResponse<CommentResponse>> replies(@AuthenticationPrincipal AuthUser user,
                                                              @PathVariable String commentId,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(postService.replies(commentId, user.id(),
                PageRequest.of(page, Math.min(size, 30))));
    }

    @PostMapping("/comments/{commentId}/replies")
    @Operation(summary = "Reply to a comment")
    public ApiResponse<CommentResponse> reply(@AuthenticationPrincipal AuthUser user,
                                              @PathVariable String commentId,
                                              @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(postService.reply(commentId, request, user), "Reply added");
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete your comment")
    public ApiResponse<Void> deleteComment(@AuthenticationPrincipal AuthUser user,
                                           @PathVariable String commentId) {
        postService.deleteComment(commentId, user);
        return ApiResponse.message("Comment deleted");
    }

    @GetMapping("/{postId}")
    @Operation(summary = "One post")
    public ApiResponse<PostResponse> findById(@AuthenticationPrincipal AuthUser user,
                                              @PathVariable String postId) {
        return ApiResponse.ok(postService.findById(postId, user.id()));
    }

    @PutMapping("/{postId}")
    @Operation(summary = "Edit your post")
    public ApiResponse<PostResponse> update(@AuthenticationPrincipal AuthUser user,
                                            @PathVariable String postId,
                                            @Valid @RequestBody PostRequest request) {
        return ApiResponse.ok(postService.update(postId, request, user.id()), "Post updated");
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete your post")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable String postId) {
        postService.delete(postId, user);
        return ApiResponse.message("Post deleted");
    }

    @GetMapping("/{postId}/comments")
    @Operation(summary = "Comments on a post")
    public ApiResponse<PageResponse<CommentResponse>> comments(@AuthenticationPrincipal AuthUser user,
                                                               @PathVariable String postId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(postService.comments(postId, user.id(),
                PageRequest.of(page, Math.min(size, 30))));
    }

    @PostMapping("/{postId}/comments")
    @Operation(summary = "Comment on a post")
    public ApiResponse<CommentResponse> comment(@AuthenticationPrincipal AuthUser user,
                                                @PathVariable String postId,
                                                @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(postService.comment(postId, request, user), "Comment added");
    }

    @PostMapping("/{postId}/reactions")
    @Operation(summary = "React to a post, or take your reaction back by repeating it")
    public ApiResponse<PostResponse> react(@AuthenticationPrincipal AuthUser user,
                                           @PathVariable String postId,
                                           @Valid @RequestBody ReactionRequest request) {
        return ApiResponse.ok(postService.react(postId, request.type(), user.id()));
    }

    @PostMapping("/{postId}/save")
    @Operation(summary = "Save or unsave a post")
    public ApiResponse<PostResponse> toggleSave(@AuthenticationPrincipal AuthUser user,
                                                @PathVariable String postId) {
        return ApiResponse.ok(postService.toggleSave(postId, user.id()));
    }
}
