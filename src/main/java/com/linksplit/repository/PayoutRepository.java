package com.linksplit.repository;

import com.linksplit.entity.Payout;
import com.linksplit.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {
    
    @Query(value = "SELECT p FROM Payout p LEFT JOIN FETCH p.paymentMethod WHERE p.user = :user ORDER BY p.requestedAt DESC",
           countQuery = "SELECT COUNT(p) FROM Payout p WHERE p.user = :user")
    Page<Payout> findByUserOrderByRequestedAtDesc(@Param("user") User user, Pageable pageable);
    
    List<Payout> findByUserAndStatusOrderByRequestedAtDesc(User user, Payout.PayoutStatus status);
    
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payout p WHERE p.user = :user AND p.status = :status")
    BigDecimal getTotalPayoutsByUserAndStatus(@Param("user") User user, @Param("status") Payout.PayoutStatus status);
    
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payout p WHERE p.user = :user")
    BigDecimal getTotalPayoutsByUser(@Param("user") User user);
    
    @Query("SELECT p FROM Payout p WHERE p.status = :status AND p.requestedAt < :before")
    List<Payout> findPendingPayoutsOlderThan(@Param("status") Payout.PayoutStatus status, 
                                             @Param("before") LocalDateTime before);
    
    boolean existsByReferenceNumber(String referenceNumber);
}