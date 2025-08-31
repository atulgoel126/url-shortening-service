// Heatmap Tracking Script for LinkSplit Analytics
(function() {
    'use strict';
    
    const HeatmapTracker = {
        shortCode: null,
        sessionId: null,
        pageUrl: window.location.href,
        
        init: function(shortCode, sessionId) {
            this.shortCode = shortCode;
            this.sessionId = sessionId || this.generateSessionId();
            this.attachClickListener();
            this.trackScrollDepth();
            this.trackTimeOnPage();
        },
        
        generateSessionId: function() {
            return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
        },
        
        attachClickListener: function() {
            document.addEventListener('click', (e) => {
                const clickData = {
                    shortCode: this.shortCode,
                    sessionId: this.sessionId,
                    pageUrl: this.pageUrl,
                    x: e.pageX,
                    y: e.pageY,
                    viewportWidth: window.innerWidth,
                    viewportHeight: window.innerHeight,
                    elementType: e.target.tagName,
                    elementText: this.getElementText(e.target),
                    elementId: e.target.id || null,
                    elementClass: e.target.className || null,
                    timestamp: new Date().toISOString()
                };
                
                this.sendClickData(clickData);
            });
        },
        
        getElementText: function(element) {
            let text = element.innerText || element.textContent || '';
            // Limit text length to prevent excessive data
            return text.substring(0, 100);
        },
        
        sendClickData: function(data) {
            // Send data to backend
            fetch('/api/analytics/heatmap', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(data)
            }).catch(error => {
                console.error('Failed to send heatmap data:', error);
            });
        },
        
        trackScrollDepth: function() {
            let maxScrollPercentage = 0;
            let ticking = false;
            
            const calculateScrollPercentage = () => {
                const windowHeight = window.innerHeight;
                const documentHeight = document.documentElement.scrollHeight;
                const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
                const scrollPercentage = Math.round((scrollTop + windowHeight) / documentHeight * 100);
                
                if (scrollPercentage > maxScrollPercentage) {
                    maxScrollPercentage = scrollPercentage;
                    
                    // Track significant scroll milestones
                    if ([25, 50, 75, 90, 100].includes(maxScrollPercentage)) {
                        this.sendScrollData(maxScrollPercentage);
                    }
                }
                
                ticking = false;
            };
            
            window.addEventListener('scroll', () => {
                if (!ticking) {
                    window.requestAnimationFrame(calculateScrollPercentage);
                    ticking = true;
                }
            });
        },
        
        sendScrollData: function(percentage) {
            // Store scroll depth data for analytics
            const scrollData = {
                shortCode: this.shortCode,
                sessionId: this.sessionId,
                scrollDepth: percentage,
                timestamp: new Date().toISOString()
            };
            
            // You can send this to a separate endpoint or store locally
            console.log('Scroll depth:', percentage + '%');
        },
        
        trackTimeOnPage: function() {
            const startTime = Date.now();
            let isActive = true;
            let totalActiveTime = 0;
            let lastActiveTime = startTime;
            
            // Track active/inactive state
            document.addEventListener('visibilitychange', () => {
                if (document.hidden) {
                    totalActiveTime += Date.now() - lastActiveTime;
                    isActive = false;
                } else {
                    lastActiveTime = Date.now();
                    isActive = true;
                }
            });
            
            // Send time data before page unload
            window.addEventListener('beforeunload', () => {
                if (isActive) {
                    totalActiveTime += Date.now() - lastActiveTime;
                }
                
                const timeData = {
                    shortCode: this.shortCode,
                    sessionId: this.sessionId,
                    totalTime: Math.round((Date.now() - startTime) / 1000),
                    activeTime: Math.round(totalActiveTime / 1000)
                };
                
                // Use sendBeacon for reliability
                if (navigator.sendBeacon) {
                    navigator.sendBeacon('/api/analytics/time', JSON.stringify(timeData));
                }
            });
        }
    };
    
    // Make HeatmapTracker available globally
    window.HeatmapTracker = HeatmapTracker;
})();