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
    
    @Value("${cache.initial-capacity:1000}")
    private int cacheInitialCapacity;
    
    @Value("${cache.short-expiration:60}")
    private int cacheShortExpiration;
    
    @Value("${cache.short-maximum-size:1000}")
    private int cacheShortMaximumSize;
    
    @Value("${cache.name.similar-products:similarProducts}")
    private String cacheSimilarProducts;
    
    @Value("${cache.name.product-details:productDetails}")
    private String cacheProductDetails;
    
    @Value("${cache.name.similar-ids:similarIds}")
    private String cacheSimilarIds;
    
    @Value("${cache.name.product-detail-optimized:productDetailOptimized}")
    private String cacheProductDetailOptimized;
    
    @Value("${cache.name.short-lived:shortLivedCache}")
    private String cacheShortLived;

    /**
     * Create a cache manager with Caffeine for better performance
     * @return CacheManager
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
            cacheSimilarProducts,
            cacheProductDetails,
            cacheSimilarIds,
            cacheProductDetailOptimized
        ));
        cacheManager.setAsyncCacheMode(true);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(cacheExpiration, TimeUnit.SECONDS)
                .maximumSize(cacheMaximumSize)
                .initialCapacity(cacheInitialCapacity)
                .recordStats());
        
        return cacheManager;
    }
    
    /**
     * Create a separate cache manager for short-lived caches
     * @return CacheManager
     */
    @Bean
    public CacheManager shortLivedCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(cacheShortLived);
        cacheManager.setAsyncCacheMode(true);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(cacheShortExpiration, TimeUnit.SECONDS)
                .maximumSize(cacheShortMaximumSize)
                .recordStats());
        
        return cacheManager;
    }
} 