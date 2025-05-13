package com.backendtest.similarproducts.service;

import com.backendtest.similarproducts.client.ProductClient;
import com.backendtest.similarproducts.model.ProductDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Service for handling similar products
 */
@Slf4j
@Service
public class SimilarProductService {
    private final int parallelRails;
    private final Duration requestTimeout;
    private final Duration cacheDuration;
    private final int timeoutMultiplier;

    
    @Value("${log.message.similar-products-debug}")
    private String logDebugSimilarProducts;
    
    @Value("${log.message.warn-not-found}")
    private String logWarnNotFound;
    
    @Value("${log.message.debug-no-similar}")
    private String logDebugNoSimilar;
    
    @Value("${log.message.debug-retrieved}")
    private String logDebugRetrieved;
    
    @Value("${log.message.warn-error-retrieve}")
    private String logWarnErrorRetrieve;
    
    @Value("${log.message.warn-timeout}")
    private String logWarnTimeout;
    
    @Value("${log.message.warn-similar-not-found}")
    private String logWarnSimilarNotFound;
    
    @Value("${log.message.error-similar-detail}")
    private String logErrorSimilarDetail;
    
    @Value("${log.message.warn-error-exists}")
    private String logWarnErrorExists;

    private final ProductClient productClient;

    public SimilarProductService(
            ProductClient productClient,
            @Value("${webclient.response-timeout:1500}") int responseTimeout,
            @Value("${service.parallel-rails:4}") int parallelRails,
            @Value("${cache.duration.minutes:10}") int cacheDurationMinutes,
            @Value("${cache.name.similar-products:similarProducts}") String cacheName,
            @Value("${cache.name.product-detail-optimized:productDetailOptimized}") String cacheNameOptimized,
            @Value("${webclient.timeout-multiplier:2}") int timeoutMultiplier) {
        this.productClient = productClient;
        this.requestTimeout = Duration.ofMillis(responseTimeout);
        this.parallelRails = parallelRails * Runtime.getRuntime().availableProcessors();
        this.cacheDuration = Duration.ofMinutes(cacheDurationMinutes);
        this.timeoutMultiplier = timeoutMultiplier;
    }

    /**
     * Get similar products for a given product ID
     * @param productId Product ID to find similar products for
     * @return List of similar product details
     */
    @Cacheable(value = "similarProducts")
    public Mono<List<ProductDetail>> getSimilarProducts(String productId) {
        log.debug(logDebugSimilarProducts, productId);
        
        return checkProductExists(productId)
                .flatMap(exists -> {
                    if (!exists) {
                        log.warn(logWarnNotFound, productId);
                        return Mono.just(Collections.emptyList());
                    }
                    return productClient.getSimilarProductIds(productId)
                        .timeout(requestTimeout)
                        .flatMapMany(ids -> {
                            if (ids.isEmpty()) {
                                log.debug(logDebugNoSimilar, productId);
                                return Flux.empty();
                            }
                            
                            return Flux.fromIterable(ids)
                                .parallel(parallelRails)
                                .runOn(Schedulers.boundedElastic())
                                .flatMap(this::getProductDetailOptimized)
                                .sequential();
                        })
                        .collectList()
                        .timeout(requestTimeout.multipliedBy(timeoutMultiplier))
                        .doOnSuccess(products -> 
                            log.debug(logDebugRetrieved, products.size(), productId)
                        )
                        .onErrorReturn(error -> {
                            log.warn(logWarnErrorRetrieve, productId, error.getMessage());
                            return true;
                        }, Collections.emptyList())
                        .cache(cacheDuration);
                });
    }

    /**
     * Check if product exists
     * @param productId Product ID to check
     * @return Boolean indicating if product exists
     */
    private Mono<Boolean> checkProductExists(String productId) {
        return productClient.getProductDetail(productId)
                .map(product -> true)
                .onErrorReturn(WebClientResponseException.NotFound.class, false)
                .onErrorResume(error -> {
                    if (!(error instanceof WebClientResponseException.NotFound)) {
                        log.warn(logWarnErrorExists, productId, error.getMessage());
                    }
                    return Mono.just(false);
                });
    }

    /**
     * Get product detail safely with improved performance
     * @param productId Product ID to get details for
     * @return Product detail or empty if not found or error
     */
    @Cacheable(value = "productDetailOptimized")
    public Mono<ProductDetail> getProductDetailOptimized(String productId) {
        return productClient.getProductDetail(productId)
                .timeout(requestTimeout)
                .onErrorResume(error -> {
                    if (error instanceof TimeoutException) {
                        log.warn(logWarnTimeout, productId);
                    } else if (error instanceof WebClientResponseException.NotFound) {
                        log.warn(logWarnSimilarNotFound, productId);
                    } else {
                        log.warn(logErrorSimilarDetail, productId, error.getMessage());
                    }
                    return Mono.empty();
                })
                .publishOn(Schedulers.boundedElastic())
                .cache(cacheDuration);
    }
} 