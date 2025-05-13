package com.backendtest.similarproducts.client;

import com.backendtest.similarproducts.model.ProductDetail;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Client for consuming the product related APIs
 */
@Slf4j
@Component
public class ProductClient {
    private final Duration requestTimeout;
    private final Duration cacheDuration;
    
    @Value("${log.message.similar-ids-debug}")
    private String logDebugSimilarIds;
    
    @Value("${log.message.product-not-found}")
    private String logProductNotFound;
    
    @Value("${log.message.error-similar-ids}")
    private String logErrorSimilarIds;
    
    @Value("${log.message.warn-circuit-breaker}")
    private String logWarnCircuitBreaker;
    
    @Value("${log.message.product-detail-debug}")
    private String logDebugProductDetail;
    
    @Value("${log.message.error-product-detail}")
    private String logErrorProductDetail;

    private final WebClient webClient;
    private final String similarIdsUrl;
    private final String productDetailUrl;
    

    public ProductClient(
            WebClient webClient,
            @Value("${api.product.similarids.url}") String similarIdsUrl,
            @Value("${api.product.detail.url}") String productDetailUrl,
            @Value("${webclient.response-timeout:1500}") int responseTimeout,
            @Value("${cache.duration.minutes:10}") int cacheDurationMinutes,
            @Value("${circuit-breaker.name.product-api:productApi}") String circuitBreakerName,
            @Value("${cache.name.similar-ids:similarIds}") String cacheSimilarIds,
            @Value("${cache.name.product-details:productDetails}") String cacheProductDetails) {
        this.webClient = webClient;
        this.similarIdsUrl = similarIdsUrl;
        this.productDetailUrl = productDetailUrl;
        this.requestTimeout = Duration.ofMillis(responseTimeout);
        this.cacheDuration = Duration.ofMinutes(cacheDurationMinutes);
    }

    /**
     * Get similar product IDs for a given product ID
     * @param productId Product ID to find similar products for
     * @return List of similar product IDs
     */
    @Cacheable(value = "similarIds")
    @CircuitBreaker(name = "${circuit-breaker.name.product-api}", fallbackMethod = "getSimilarProductIdsFallback")
    @Retry(name = "${circuit-breaker.name.product-api}", fallbackMethod = "getSimilarProductIdsFallback")
    public Mono<List<String>> getSimilarProductIds(String productId) {
        log.debug(logDebugSimilarIds, productId);
        return webClient.get()
                .uri(similarIdsUrl, productId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .timeout(requestTimeout)
                .publishOn(Schedulers.boundedElastic())
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.warn(logProductNotFound, productId);
                    return Mono.just(Collections.emptyList());
                })
                .onErrorResume(e -> {
                    log.error(logErrorSimilarIds, e.getMessage());
                    return Mono.just(Collections.emptyList());
                })
                .cache(cacheDuration);
    }

    /**
     * Fallback method for getSimilarProductIds
     */
    private Mono<List<String>> getSimilarProductIdsFallback(String productId, Throwable throwable) {
        log.warn(logWarnCircuitBreaker, "getSimilarProductIds", throwable.getMessage());
        return Mono.just(Collections.emptyList());
    }

    /**
     * Get product detail for a given product ID
     * @param productId Product ID to get details for
     * @return Product detail
     */
    @Cacheable(value = "productDetails")
    @CircuitBreaker(name = "${circuit-breaker.name.product-api}", fallbackMethod = "getProductDetailFallback")
    @Retry(name = "${circuit-breaker.name.product-api}", fallbackMethod = "getProductDetailFallback")
    public Mono<ProductDetail> getProductDetail(String productId) {
        log.debug(logDebugProductDetail, productId);
        return webClient.get()
                .uri(productDetailUrl, productId)
                .retrieve()
                .bodyToMono(ProductDetail.class)
                .timeout(requestTimeout)
                .publishOn(Schedulers.boundedElastic())
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.warn(logProductNotFound, productId);
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error(logErrorProductDetail, e.getMessage());
                    return Mono.empty();
                })
                .cache(cacheDuration);
    }

    /**
     * Fallback method for getProductDetail
     */
    private Mono<ProductDetail> getProductDetailFallback(String productId, Throwable throwable) {
        log.warn(logWarnCircuitBreaker, "getProductDetail", throwable.getMessage());
        return Mono.empty();
    }
} 