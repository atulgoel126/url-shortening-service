package com.linksplit.service;

import com.linksplit.entity.Comment;
import com.linksplit.entity.Link;
import com.linksplit.entity.User;
import com.linksplit.repository.CommentRepository;
import com.linksplit.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final LinkRepository linkRepository;

    public Comment createComment(Long linkId, String content, User user) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new RuntimeException("Link not found"));
        
        // Verify the user owns this link
        if (!link.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You can only add comments to your own links");
        }

        Comment comment = Comment.builder()
                .link(link)
                .user(user)
                .content(content)
                .build();

        return commentRepository.save(comment);
    }

    public List<Comment> getCommentsByLink(Long linkId, User user) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new RuntimeException("Link not found"));
        
        // Verify the user owns this link
        if (!link.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You can only view comments on your own links");
        }

        return commentRepository.findByLinkIdOrderByCreatedAtDesc(linkId);
    }

    public Comment updateComment(Long commentId, String content, User user) {
        Comment comment = commentRepository.findByIdAndUser(commentId, user)
                .orElseThrow(() -> new RuntimeException("Comment not found or unauthorized"));
        
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    public void deleteComment(Long commentId, User user) {
        Comment comment = commentRepository.findByIdAndUser(commentId, user)
                .orElseThrow(() -> new RuntimeException("Comment not found or unauthorized"));
        
        commentRepository.delete(comment);
    }
}