package com.linksplit.repository;

import com.linksplit.entity.Link;
import com.linksplit.entity.LinkView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LinkViewRepository extends JpaRepository<LinkView, Long> {
    
    @Query("SELECT COUNT(lv) > 0 FROM LinkView lv " +
           "WHERE lv.link = :link " +
           "AND lv.ipAddress = :ipAddress " +
           "AND lv.viewedAt > :sinceTime")
    boolean existsRecentView(@Param("link") Link link, 
                            @Param("ipAddress") String ipAddress, 
                            @Param("sinceTime") LocalDateTime sinceTime);
    
    @Query("SELECT COUNT(lv) FROM LinkView lv WHERE lv.link = :link")
    Long countViewsByLink(@Param("link") Link link);
    
    List<LinkView> findByLink(Link link);
    
    @Query("SELECT lv FROM LinkView lv WHERE lv.link.id = :linkId ORDER BY lv.viewedAt DESC")
    List<LinkView> findRecentViewsByLinkId(@Param("linkId") Long linkId);
    
    List<LinkView> findByLinkInAndViewedAtBetween(List<Link> links, LocalDateTime start, LocalDateTime end);
    
    List<LinkView> findByLinkAndViewedAtBetween(Link link, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT lv.country, COUNT(lv) FROM LinkView lv WHERE lv.link IN :links " +
           "GROUP BY lv.country ORDER BY COUNT(lv) DESC")
    List<Object[]> getCountryStatistics(@Param("links") List<Link> links);
    
    @Query("SELECT lv.browser, COUNT(lv) FROM LinkView lv WHERE lv.link IN :links " +
           "GROUP BY lv.browser ORDER BY COUNT(lv) DESC")
    List<Object[]> getBrowserStatistics(@Param("links") List<Link> links);
    
    @Query("SELECT COUNT(lv) FROM LinkView lv WHERE lv.link IN :links " +
           "AND lv.viewedAt >= :startOfDay AND lv.viewedAt < :endOfDay")
    Long getTodayClicksByLinks(@Param("links") List<Link> links, 
                              @Param("startOfDay") LocalDateTime startOfDay, 
                              @Param("endOfDay") LocalDateTime endOfDay);
}