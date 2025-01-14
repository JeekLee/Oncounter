package com.bluehair.hanghaefinalproject.like.service;

import com.bluehair.hanghaefinalproject.comment.entity.Comment;
import com.bluehair.hanghaefinalproject.like.dto.CommentLikeDto;
import com.bluehair.hanghaefinalproject.like.entity.CommentLike;
import com.bluehair.hanghaefinalproject.like.entity.CommentLikeCompositeKey;
import com.bluehair.hanghaefinalproject.like.repository.CommentLikeRepository;
import com.bluehair.hanghaefinalproject.comment.repository.CommentRepository;
import com.bluehair.hanghaefinalproject.common.exception.Domain;
import com.bluehair.hanghaefinalproject.common.exception.Layer;
import com.bluehair.hanghaefinalproject.common.exception.NotFoundException;
import com.bluehair.hanghaefinalproject.like.dto.PostLikeDto;
import com.bluehair.hanghaefinalproject.like.repository.PostLikeRepository;
import com.bluehair.hanghaefinalproject.member.entity.Member;
import com.bluehair.hanghaefinalproject.member.repository.MemberRepository;
import com.bluehair.hanghaefinalproject.post.entity.Post;
import com.bluehair.hanghaefinalproject.post.repository.PostRepository;
import com.bluehair.hanghaefinalproject.like.entity.PostLike;
import com.bluehair.hanghaefinalproject.like.entity.PostLikeCompositeKey;
import com.bluehair.hanghaefinalproject.sse.dto.RequestNotificationDto;
import com.bluehair.hanghaefinalproject.sse.entity.NotificationType;
import com.bluehair.hanghaefinalproject.sse.entity.RedirectionType;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.bluehair.hanghaefinalproject.common.exception.Domain.COMMENT;
import static com.bluehair.hanghaefinalproject.common.exception.Domain.LIKE;
import static com.bluehair.hanghaefinalproject.common.exception.Layer.SERVICE;
import static com.bluehair.hanghaefinalproject.common.response.error.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PostLikeDto postLike(Long postId, Member member){
        Post postliked = postRepository.findById(postId)
                .orElseThrow(()-> new NotFoundException(LIKE, SERVICE, POST_NOT_FOUND, "Post ID : " + postId)
                );
        PostLikeCompositeKey postLikeCompositeKey
                = new PostLikeCompositeKey(member.getId(), postliked.getId());
        boolean likecheck;
        Optional<PostLike> postLike= postLikeRepository.findByPostLikedIdAndMemberId(postliked.getId(), member.getId());

        if(postLike.isPresent()){
            postLikeRepository.deleteById(postLikeCompositeKey);
            postliked.unLike();
            postRepository.save(postliked);
            likecheck = false;

            return new PostLikeDto(likecheck, postliked.getLikeCount());
        }

        postLikeRepository.save(new PostLike(postLikeCompositeKey, member, postliked));
        likecheck=true;
        postliked.like();
        postRepository.save(postliked);


        Member postMember = memberRepository.findByNickname(postliked.getNickname())
                .orElseThrow(() -> new NotFoundException(COMMENT, SERVICE, MEMBER_NOT_FOUND, "Nickname : " + postliked.getNickname()));
        if(!postMember.getNickname().equals(member.getNickname())) {
            String content = postliked.getTitle() + "을(를) " + member.getNickname() + "님이 좋아합니다.";
            notify(postMember, member, NotificationType.POST_LIKED, content, RedirectionType.detail, postId, null);
        }

        return new PostLikeDto(likecheck, postliked.getLikeCount());

    }

    @Transactional
    public CommentLikeDto likeComment(Long commentId, String nickname) {

        boolean isLiked = false;

        Member member = memberRepository.findByNickname(nickname).orElseThrow(
                () -> new NotFoundException(Domain.COMMENT, Layer.SERVICE,MEMBER_NOT_FOUND, "Nickname : " + nickname)
        );
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new NotFoundException(Domain.COMMENT,Layer.SERVICE,COMMENT_NOT_FOUND, "Comment ID : " + commentId)
        );

        CommentLikeCompositeKey commentLikeCompositeKey = new CommentLikeCompositeKey(commentId, member.getId());
        Optional<CommentLike> like = commentLikeRepository.findByCommentIdAndMemberId(commentId,member.getId());
        if (like.isPresent()){
            CommentLike commentLike = like.get();
            comment.unlike();
            commentLikeRepository.delete(commentLike);

            return new CommentLikeDto(isLiked, comment.getLikeCount());
        }
        CommentLike commentLike = new CommentLike(commentLikeCompositeKey, member, comment);
        comment.like();
        commentLikeRepository.save(commentLike);
        isLiked = true;

        Member commentMember = memberRepository.findByNickname(comment.getNickname())
                .orElseThrow(() -> new NotFoundException(COMMENT, SERVICE, MEMBER_NOT_FOUND, "Nickname : " + comment.getNickname()));
        if(!commentMember.getNickname().equals(member.getNickname())) {
            Long postId = comment.getPost().getId();
            String content = commentMember.getNickname() + "님의 댓글을 " + nickname + "님이 좋아합니다.";
            notify(commentMember, member, NotificationType.COMMENT_LIKED, content, RedirectionType.detail, postId, null);
        }

        return new CommentLikeDto(isLiked, comment.getLikeCount());

        }

    private void notify(Member postMember, Member sender, NotificationType notificationType,
                            String content, RedirectionType type, Long typeId, Long postId){
        eventPublisher.publishEvent(new RequestNotificationDto(postMember,sender, notificationType,content,type, typeId, postId));

    }

    }

