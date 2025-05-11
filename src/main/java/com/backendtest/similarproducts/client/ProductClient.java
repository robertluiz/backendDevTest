package com.backendtest.similarproducts.client;

import com.backendtest.similarproducts.model.ProductDetail;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Component
public class ProductClient {
    private static final Logger logger = LoggerFactory.getLogger(ProductClient.class);
    private static final String PRODUCT_API = "productApi";
    private final Duration requestTimeout;
    private static final Duration CACHE_TIMEOUT = Duration.ofMillis(500);

    private final WebClient webClient;
    private final String similarIdsUrl;
    private final String productDetailUrl;

    public ProductClient(
            WebClient webClient,
            @Value("${api.product.similarids.url}") String similarIdsUrl,
            @Value("${api.product.detail.url}") String productDetailUrl,
            @Value("${webclient.response-timeout:1500}") int responseTimeout) {
        this.webClient = webClient;
        this.similarIdsUrl = similarIdsUrl;
        this.productDetailUrl = productDetailUrl;
        this.requestTimeout = Duration.ofMillis(responseTimeout);
    }

    /**
     * Get similar product IDs for a given product ID
     * @param productId Product ID to find similar products for
     * @return List of similar product IDs
     */
    @Cacheable(value = "similarIds", key = "#productId")
    @CircuitBreaker(name = PRODUCT_API, fallbackMethod = "getSimilarProductIdsFallback")
    @Retry(name = PRODUCT_API, fallbackMethod = "getSimilarProductIdsFallback")
    public Mono<List<String>> getSimilarProductIds(String productId) {
        logger.debug("Getting similar product IDs for product: {}", productId);
        return webClient.get()
                .uri(similarIdsUrl, productId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .timeout(requestTimeout)
                .publishOn(Schedulers.boundedElastic())
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    logger.warn("Product not found: {}", productId);
                    return Mono.just(Collections.emptyList());
                })
                .onErrorResume(e -> {
                    logger.error("Error fetching similar product IDs: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                })
                .cache(Duration.ofMinutes(10));
    }

    /**
     * Fallback method for getSimilarProductIds
     */
    private Mono<List<String>> getSimilarProductIdsFallback(String productId, Throwable throwable) {
        logger.warn("Circuit breaker or retry triggered for getSimilarProductIds: {}", throwable.getMessage());
        return Mono.just(Collections.emptyList());
    }

    /**
     * Get product detail for a given product ID
     * @param productId Product ID to get details for
     * @return Product detail
     */
    @Cacheable(value = "productDetails", key = "#productId")
    @CircuitBreaker(name = PRODUCT_API, fallbackMethod = "getProductDetailFallback")
    @Retry(name = PRODUCT_API, fallbackMethod = "getProductDetailFallback")
    public Mono<ProductDetail> getProductDetail(String productId) {
        logger.debug("Getting product detail for product: {}", productId);
        return webClient.get()
                .uri(productDetailUrl, productId)
                .retrieve()
                .bodyToMono(ProductDetail.class)
                .timeout(requestTimeout)
                .publishOn(Schedulers.boundedElastic())
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    logger.warn("Product not found: {}", productId);
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    logger.error("Error fetching product detail: {}", e.getMessage());
                    return Mono.empty();
                })
                .cache(Duration.ofMinutes(10));
    }

    /**
     * Fallback method for getProductDetail
     */
    private Mono<ProductDetail> getProductDetailFallback(String productId, Throwable throwable) {
        logger.warn("Circuit breaker or retry triggered for getProductDetail: {}", throwable.getMessage());
        return Mono.empty();
    }
} 