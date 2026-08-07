package com.legacyloop.social.repository;

import com.legacyloop.social.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentRepository extends MongoRepository<Comment, String> {

    /** Top-level comments only; replies are fetched per comment when the reader expands them. */
    Page<Comment> findByPostIdAndParentIdIsNullAndDeletedFalseOrderByCreatedAtDesc(String postId,
                                                                                   Pageable pageable);

    Page<Comment> findByParentIdAndDeletedFalseOrderByCreatedAtAsc(String parentId, Pageable pageable);
}
