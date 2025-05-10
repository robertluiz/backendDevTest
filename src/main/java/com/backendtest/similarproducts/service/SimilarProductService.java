package com.backendtest.similarproducts.service;

import com.backendtest.similarproducts.client.ProductClient;
import com.backendtest.similarproducts.model.ProductDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Service for handling similar products
 */
@Service
public class SimilarProductService {
    private static final Logger logger = LoggerFactory.getLogger(SimilarProductService.class);
    private static final int PARALLEL_RAILS = Runtime.getRuntime().availableProcessors() * 2;
    private static final Duration REQUEST_TIMEOUT = Duration.ofMillis(2500);

    private final ProductClient productClient;

    public SimilarProductService(ProductClient productClient) {
        this.productClient = productClient;
    }

    /**
     * Get similar products for a given product ID
     * @param productId Product ID to find similar products for
     * @return List of similar product details
     */
    @Cacheable(value = "similarProducts", key = "#productId")
    public Mono<List<ProductDetail>> getSimilarProducts(String productId) {
        logger.info("Getting similar products for product: {}", productId);
               
        return productClient.getProductDetail(productId)
                .timeout(REQUEST_TIMEOUT)
                .onErrorResume(TimeoutException.class, e -> {
                    logger.error("Timeout getting product details: {}", productId, e);
                    return Mono.error(e);
                })
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    logger.error("Product not found: {}", productId);
                    return Mono.error(e);
                })
                .flatMap(product -> 
                    productClient.getSimilarProductIds(productId)
                            .timeout(REQUEST_TIMEOUT)
                            .flatMapMany(ids -> {
                                return Flux.fromIterable(ids)
                                        .parallel(PARALLEL_RAILS)
                                        .runOn(Schedulers.boundedElastic())
                                        .flatMap(this::getProductDetailWithTimeout)
                                        .sequential();
                            })
                            .collectList()
                            .timeout(REQUEST_TIMEOUT.multipliedBy(2))
                            .doOnSuccess(products -> 
                                logger.debug("Retrieved {} similar products for {}", products.size(), productId)
                            )
                );
    }

    /**
     * Get product detail safely with timeout control
     * @param productId Product ID to get details for
     * @return Product detail or empty if not found or error
     */
    @Cacheable(value = "productDetails", key = "#productId")
    private Mono<ProductDetail> getProductDetailWithTimeout(String productId) {
        return productClient.getProductDetail(productId)
                .timeout(REQUEST_TIMEOUT)
                .onErrorResume(TimeoutException.class, e -> {
                    logger.warn("Timeout getting similar product: {}", productId);
                    return Mono.empty();
                })
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    logger.warn("Similar product not found: {}", productId);
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    logger.warn("Error fetching similar product: {}", e.getMessage());
                    return Mono.empty();
                })
                .doOnSuccess(product -> {
                    if (product != null) {
                        logger.debug("Successfully retrieved product detail for ID: {}", productId);
                    }
                });
    }
} 