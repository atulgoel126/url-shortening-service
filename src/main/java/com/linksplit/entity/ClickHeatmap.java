package com.linksplit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_heatmaps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickHeatmap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;
    
    @Column(name = "page_url", nullable = false)
    private String pageUrl;
    
    @Column(name = "x_coordinate", nullable = false)
    private Integer xCoordinate;
    
    @Column(name = "y_coordinate", nullable = false)
    private Integer yCoordinate;
    
    @Column(name = "viewport_width")
    private Integer viewportWidth;
    
    @Column(name = "viewport_height")
    private Integer viewportHeight;
    
    @Column(name = "element_type")
    private String elementType;
    
    @Column(name = "element_text", length = 500)
    private String elementText;
    
    @Column(name = "element_id")
    private String elementId;
    
    @Column(name = "element_class")
    private String elementClass;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;
}