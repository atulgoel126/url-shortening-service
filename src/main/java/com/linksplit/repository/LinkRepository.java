package com.linksplit.repository;

import com.linksplit.entity.Link;
import com.linksplit.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findByShortCode(String shortCode);
    
    Page<Link> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    List<Link> findByUser(User user);
    
    @Query("SELECT SUM(l.viewCount) FROM Link l WHERE l.user = :user")
    Long getTotalViewsByUser(@Param("user") User user);
    
    @Query("SELECT SUM(l.estimatedEarnings) FROM Link l WHERE l.user = :user")
    BigDecimal getTotalEarningsByUser(@Param("user") User user);
    
    @Modifying
    @Query("UPDATE Link l SET l.viewCount = l.viewCount + 1 WHERE l.id = :linkId")
    void incrementViewCount(@Param("linkId") Long linkId);
    
    @Modifying
    @Query("UPDATE Link l SET l.duplicateViewCount = l.duplicateViewCount + 1 WHERE l.id = :linkId")
    void incrementDuplicateViewCount(@Param("linkId") Long linkId);
    
    @Modifying
    @Query("UPDATE Link l SET l.estimatedEarnings = :earnings WHERE l.id = :linkId")
    void updateEarnings(@Param("linkId") Long linkId, @Param("earnings") BigDecimal earnings);
    
    boolean existsByShortCode(String shortCode);
}