package com.backendtest.similarproducts.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Enhanced cache configuration using Caffeine for better performance
 */
@Configuration
public class CacheConfig {

    @Value("${cache.expiration:300}")
    private int cacheExpiration;

    @Value("${cache.maximum-size:10000}")
    private int cacheMaximumSize;

    /**
     * Create a cache manager with Caffeine for better performance
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("similarProducts", "productDetails");
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(cacheExpiration, TimeUnit.SECONDS)
                .maximumSize(cacheMaximumSize)
                .recordStats());
        
        return cacheManager;
    }
} 