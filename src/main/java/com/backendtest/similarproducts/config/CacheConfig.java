package com.backendtest.similarproducts.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced cache configuration using Caffeine for better performance
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.expiration:600}")
    private int cacheExpiration;

    @Value("${cache.maximum-size:25000}")
    private int cacheMaximumSize;

    /**
     * Create a cache manager with Caffeine for better performance
     * @return CacheManager
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
            "similarProducts",
            "productDetails",
            "similarIds",
            "productDetailOptimized"
        ));
        cacheManager.setAsyncCacheMode(true);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(cacheExpiration, TimeUnit.SECONDS)
                .maximumSize(cacheMaximumSize)
                .initialCapacity(1000)
                .recordStats());
        
        return cacheManager;
    }
    
    /**
     * Create a separate cache manager for short-lived caches
     * @return CacheManager
     */
    @Bean
    public CacheManager shortLivedCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("shortLivedCache");
        cacheManager.setAsyncCacheMode(true);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(1000)
                .recordStats());
        
        return cacheManager;
    }
} 