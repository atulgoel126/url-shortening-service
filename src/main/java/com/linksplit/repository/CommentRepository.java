package com.linksplit.repository;

import com.linksplit.entity.Comment;
import com.linksplit.entity.Link;
import com.linksplit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByLinkOrderByCreatedAtDesc(Link link);
    List<Comment> findByLinkIdOrderByCreatedAtDesc(Long linkId);
    Optional<Comment> findByIdAndUser(Long id, User user);
    void deleteByIdAndUser(Long id, User user);
    boolean existsByIdAndUser(Long id, User user);
}