package it.polimi.nasa.nasabackend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration for Analysis Service
 *
 * Implements caching to prevent redundant database queries when multiple
 * API endpoints request the same data. This significantly improves performance
 * for the Analysis Screen which makes 8 simultaneous API calls.
 *
 * Cache Strategy:
 * - Correlation results cached for 5 minutes
 * - Automatically evicts old entries
 * - Uses Caffeine for high-performance in-memory caching
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine-based cache manager
     *
     * Cache Names:
     * - "flare-cme-verified" - NASA verified Flare → CME correlations
     * - "cme-ips-verified" - NASA verified CME → IPS correlations
     * - "ips-storm-verified" - NASA verified IPS → Storm correlations
     * - "complete-chain-verified" - Complete verified chains
     * - "flare-cme-manual" - Manual temporal Flare → CME
     * - "cme-ips-manual" - Manual temporal CME → IPS
     * - "ips-storm-manual" - Manual temporal IPS → Storm
     * - "complete-chain-manual" - Manual temporal chains
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "flare-cme-verified",
                "cme-ips-verified",
                "ips-storm-verified",
                "complete-chain-verified",
                "flare-cme-manual",
                "cme-ips-manual",
                "ips-storm-manual",
                "complete-chain-manual"
        );

        cacheManager.setCaffeine(caffeineCacheBuilder());

        return cacheManager;
    }

    /**
     * Configure Caffeine cache properties
     *
     * Settings:
     * - expireAfterWrite: 5 minutes (data doesn't change frequently)
     * - maximumSize: 100 entries per cache
     * - recordStats: Enable cache statistics for monitoring
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)  // Cache expires after 5 minutes
                .maximumSize(100)                        // Max 100 entries per cache
                .recordStats();                          // Enable statistics
    }
}