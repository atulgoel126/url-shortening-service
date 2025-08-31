package com.linksplit.repository;

import com.linksplit.entity.ClickHeatmap;
import com.linksplit.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClickHeatmapRepository extends JpaRepository<ClickHeatmap, Long> {
    
    List<ClickHeatmap> findByLinkAndClickedAtBetween(Link link, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT c FROM ClickHeatmap c WHERE c.link = :link AND c.pageUrl = :pageUrl " +
           "AND c.clickedAt BETWEEN :start AND :end")
    List<ClickHeatmap> findHeatmapData(@Param("link") Link link, 
                                       @Param("pageUrl") String pageUrl,
                                       @Param("start") LocalDateTime start, 
                                       @Param("end") LocalDateTime end);
    
    @Query("SELECT c.elementText, COUNT(c) as clickCount FROM ClickHeatmap c " +
           "WHERE c.link = :link AND c.clickedAt BETWEEN :start AND :end " +
           "GROUP BY c.elementText ORDER BY clickCount DESC")
    List<Object[]> findMostClickedElements(@Param("link") Link link,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);
}