package com.linksplit.repository;

import com.linksplit.entity.PaymentMethod;
import com.linksplit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    
    List<PaymentMethod> findByUserOrderByIsPrimaryDescCreatedAtDesc(User user);
    
    Optional<PaymentMethod> findByUserAndIsPrimaryTrue(User user);
    
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.user = :user AND pm.upiId = :upiId")
    Optional<PaymentMethod> findByUserAndUpiId(@Param("user") User user, @Param("upiId") String upiId);
    
    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.isPrimary = false WHERE pm.user = :user")
    void clearPrimaryForUser(@Param("user") User user);
    
    Long countByUser(User user);
}