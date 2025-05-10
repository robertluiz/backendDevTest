package com.backendtest.similarproducts.client;

import com.backendtest.similarproducts.model.ProductDetail;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Client for consuming the product related APIs
 */
@Component
public class ProductClient {
    private static final Logger logger = LoggerFactory.getLogger(ProductClient.class);
    private static final String PRODUCT_API = "productApi";

    private final WebClient webClient;
    private final String similarIdsUrl;
    private final String productDetailUrl;

    public ProductClient(
            WebClient webClient,
            @Value("${api.product.similarids.url}") String similarIdsUrl,
            @Value("${api.product.detail.url}") String productDetailUrl) {
        this.webClient = webClient;
        this.similarIdsUrl = similarIdsUrl;
        this.productDetailUrl = productDetailUrl;
    }

    /**
     * Get similar product IDs for a given product ID
     * @param productId Product ID to find similar products for
     * @return List of similar product IDs
     */
    @CircuitBreaker(name = PRODUCT_API, fallbackMethod = "getSimilarProductIdsFallback")
    public Mono<List<String>> getSimilarProductIds(String productId) {
        logger.info("Getting similar product IDs for product: {}", productId);
        return webClient.get()
                .uri(similarIdsUrl, productId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    logger.error("Product not found: {}", productId);
                    return Mono.error(e);
                })
                .onErrorResume(e -> {
                    logger.error("Error fetching similar product IDs: {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    /**
     * Fallback method for getSimilarProductIds
     */
    private Mono<List<String>> getSimilarProductIdsFallback(String productId, Throwable throwable) {
        logger.warn("Circuit breaker triggered for getSimilarProductIds: {}", throwable.getMessage());
        return Mono.just(List.of());
    }

    /**
     * Get product detail for a given product ID
     * @param productId Product ID to get details for
     * @return Product detail
     */
    @CircuitBreaker(name = PRODUCT_API, fallbackMethod = "getProductDetailFallback")
    public Mono<ProductDetail> getProductDetail(String productId) {
        logger.info("Getting product detail for product: {}", productId);
        return webClient.get()
                .uri(productDetailUrl, productId)
                .retrieve()
                .bodyToMono(ProductDetail.class)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    logger.error("Product not found: {}", productId);
                    return Mono.error(e);
                })
                .onErrorResume(e -> {
                    logger.error("Error fetching product detail: {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    /**
     * Fallback method for getProductDetail
     */
    private Mono<ProductDetail> getProductDetailFallback(String productId, Throwable throwable) {
        logger.warn("Circuit breaker triggered for getProductDetail: {}", throwable.getMessage());
        return Mono.empty();
    }
} 