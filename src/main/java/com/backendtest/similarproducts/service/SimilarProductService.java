package com.backendtest.similarproducts.service;

import com.backendtest.similarproducts.client.ProductClient;
import com.backendtest.similarproducts.model.ProductDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Service
public class SimilarProductService {
    private static final Logger logger = LoggerFactory.getLogger(SimilarProductService.class);
    private static final int PARALLEL_RAILS = Runtime.getRuntime().availableProcessors() * 4;
    private final Duration requestTimeout;
    private static final Duration CACHE_DURATION = Duration.ofMinutes(10);

    private final ProductClient productClient;

    public SimilarProductService(
            ProductClient productClient,
            @Value("${webclient.response-timeout:1500}") int responseTimeout) {
        this.productClient = productClient;
        this.requestTimeout = Duration.ofMillis(responseTimeout);
    }

    /**
     * Get similar products for a given product ID
     * @param productId Product ID to find similar products for
     * @return List of similar product details
     */
    @Cacheable(value = "similarProducts", key = "#productId")
    public Mono<List<ProductDetail>> getSimilarProducts(String productId) {
        logger.debug("Getting similar products for product: {}", productId);
        
        return checkProductExists(productId)
                .flatMap(exists -> {
                    if (!exists) {
                        logger.warn("Main product {} not found, returning empty list for similar products.", productId);
                        return Mono.just(Collections.emptyList());
                    }
                    return productClient.getSimilarProductIds(productId)
                        .timeout(requestTimeout)
                        .flatMapMany(ids -> {
                            if (ids.isEmpty()) {
                                logger.debug("No similar product IDs found for {}", productId);
                                return Flux.empty();
                            }
                            
                            return Flux.fromIterable(ids)
                                .parallel(PARALLEL_RAILS)
                                .runOn(Schedulers.boundedElastic())
                                .flatMap(this::getProductDetailOptimized)
                                .sequential();
                        })
                        .collectList()
                        .timeout(requestTimeout.multipliedBy(2))
                        .doOnSuccess(products -> 
                            logger.debug("Retrieved {} similar products for {}", products.size(), productId)
                        )
                        .onErrorReturn(error -> {
                            logger.warn("Error retrieving similar products for {}: {}. Returning empty list.", productId, error.getMessage());
                            return true;
                        }, Collections.emptyList())
                        .cache(CACHE_DURATION);
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
                        logger.warn("Error checking if product {} exists: {}", productId, error.getMessage());
                    }
                    return Mono.just(false);
                });
    }

    /**
     * Get product detail safely with improved performance
     * @param productId Product ID to get details for
     * @return Product detail or empty if not found or error
     */
    @Cacheable(value = "productDetailOptimized", key = "#productId")
    public Mono<ProductDetail> getProductDetailOptimized(String productId) {
        return productClient.getProductDetail(productId)
                .timeout(requestTimeout)
                .onErrorResume(error -> {
                    if (error instanceof TimeoutException) {
                        logger.warn("Timeout getting similar product detail: {}", productId);
                    } else if (error instanceof WebClientResponseException.NotFound) {
                        logger.warn("Similar product detail not found: {}", productId);
                    } else {
                        logger.warn("Error fetching similar product detail for {}: {}", productId, error.getMessage());
                    }
                    return Mono.empty();
                })
                .publishOn(Schedulers.boundedElastic())
                .cache(CACHE_DURATION);
    }
} 