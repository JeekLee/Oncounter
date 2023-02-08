package com.bluehair.hanghaefinalproject.post.repository;

import com.bluehair.hanghaefinalproject.post.entity.Post;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PostCustomRepository {
    Post save(Post post);
    Optional<Post> findById(Long postId);
    List<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Post> findByTitleContainsOrContentsContains(Pageable pageable, String search, String searchContents);
    List<Post> findByContentsContains(Pageable pageable, String search);
    List<Post> findByNickname(Pageable pageable,String nickname);
    void deleteById(Long postId);
    void updateNickname(String before, String after);
}