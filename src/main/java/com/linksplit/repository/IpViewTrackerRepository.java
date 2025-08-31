package com.linksplit.repository;

import com.linksplit.entity.IpViewTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface IpViewTrackerRepository extends JpaRepository<IpViewTracker, Long> {
    
    @Query("SELECT COUNT(t) FROM IpViewTracker t WHERE t.ipAddress = :ipAddress AND t.viewedAt > :since")
    long countViewsSince(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    @Modifying
    @Query("DELETE FROM IpViewTracker t WHERE t.viewedAt < :before")
    void deleteOldRecords(@Param("before") LocalDateTime before);
}