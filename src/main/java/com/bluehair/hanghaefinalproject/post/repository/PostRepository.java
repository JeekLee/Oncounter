package com.bluehair.hanghaefinalproject.post.repository;

import com.bluehair.hanghaefinalproject.post.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface PostRepository {
    Post save(Post post);
    Optional<Post> findById(Long postId);
    List<Post> findAllByOrderByModifiedAtDesc(Pageable pageable);
}
