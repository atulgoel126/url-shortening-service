package com.linksplit.repository;

import com.linksplit.entity.Referrer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferrerRepository extends JpaRepository<Referrer, Long> {
    Optional<Referrer> findByReferrerId(String referrerId);

    @Query("SELECT DISTINCT r FROM Referrer r LEFT JOIN FETCH r.users")
    List<Referrer> findAllWithUsers();

    @Query("SELECT r FROM Referrer r LEFT JOIN FETCH r.users WHERE r.id = :id")
    Optional<Referrer> findByIdWithUsers(Long id);
}