package com.legacyloop.social.repository;

import com.legacyloop.social.entity.Post;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PostRepository extends MongoRepository<Post, String> {

    Page<Post> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    Page<Post> findByHashtagsContainingAndDeletedFalseOrderByCreatedAtDesc(String hashtag, Pageable pageable);

    Page<Post> findBySavedByContainingAndDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** The personalised feed: your posts and your connections'. */
    Page<Post> findByAuthorIdInAndDeletedFalseOrderByCreatedAtDesc(Collection<Long> authorIds,
                                                                   Pageable pageable);

    /** Mongo's own regex search over the post body — no separate search index to keep in step. */
    @Query("{ 'deleted': false, 'content': { $regex: ?0, $options: 'i' } }")
    Page<Post> search(String query, Pageable pageable);

    List<Post> findTop50ByDeletedFalseOrderByCreatedAtDesc();
}
