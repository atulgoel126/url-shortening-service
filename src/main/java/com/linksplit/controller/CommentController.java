package com.linksplit.controller;

import com.linksplit.dto.CommentDto;
import com.linksplit.dto.CreateCommentRequest;
import com.linksplit.entity.Comment;
import com.linksplit.entity.User;
import com.linksplit.service.CommentService;
import com.linksplit.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/links/{linkId}/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<CommentDto> createComment(
            @PathVariable Long linkId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        
        User user = userService.getUserByEmail(authentication.getName());
        Comment comment = commentService.createComment(linkId, request.getContent(), user);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(comment));
    }

    @GetMapping
    public ResponseEntity<List<CommentDto>> getComments(
            @PathVariable Long linkId,
            Authentication authentication) {
        
        User user = userService.getUserByEmail(authentication.getName());
        List<Comment> comments = commentService.getCommentsByLink(linkId, user);
        
        List<CommentDto> commentDtos = comments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(commentDtos);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable Long linkId,
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        
        User user = userService.getUserByEmail(authentication.getName());
        Comment comment = commentService.updateComment(commentId, request.getContent(), user);
        
        return ResponseEntity.ok(toDto(comment));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long linkId,
            @PathVariable Long commentId,
            Authentication authentication) {
        
        User user = userService.getUserByEmail(authentication.getName());
        commentService.deleteComment(commentId, user);
        
        return ResponseEntity.noContent().build();
    }

    private CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .linkId(comment.getLink().getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}