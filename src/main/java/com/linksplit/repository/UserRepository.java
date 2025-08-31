package com.linksplit.repository;

import com.linksplit.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    List<User> findTop10ByOrderByCreatedAtDesc();
    
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT u, SUM(l.estimatedEarnings) as totalEarnings " +
           "FROM User u LEFT JOIN u.links l " +
           "GROUP BY u " +
           "ORDER BY totalEarnings DESC")
    List<Object[]> findTopCreatorsByEarnings(Pageable pageable);
}