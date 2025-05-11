package com.backendtest.similarproducts.controller;

import com.backendtest.similarproducts.model.ProductDetail;
import com.backendtest.similarproducts.service.SimilarProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Controller for similar products API
 */
@RestController
@RequestMapping("/product")
public class SimilarProductController {
    private static final Logger logger = LoggerFactory.getLogger(SimilarProductController.class);

    private final SimilarProductService similarProductService;

    public SimilarProductController(SimilarProductService similarProductService) {
        this.similarProductService = similarProductService;
    }

    /**
     * Get similar products for a given product ID
     * @param productId Product ID to find similar products for
     * @return List of similar product details
     */
    @GetMapping(value = "/{productId}/similar", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<ProductDetail>> getSimilarProducts(@PathVariable String productId) {
        logger.debug("Request received for similar products of: {}", productId);
        
        return similarProductService.getSimilarProducts(productId)
                .onErrorReturn(Collections.emptyList());
    }

    /**
     * Handle not found exceptions
     * @param ex Exception thrown
     * @return Error response
     */
    @ExceptionHandler(WebClientResponseException.NotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Map<String, Boolean>> handleNotFoundException(WebClientResponseException.NotFound ex) {
        logger.warn("Product not found error: {}", ex.getMessage());
        return Mono.just(Map.of("scanAvailable", true));
    }

    /**
     * Handle timeout exceptions
     * @param ex Exception thrown
     * @return Error response
     */
    @ExceptionHandler(TimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public Mono<Map<String, Boolean>> handleTimeoutException(TimeoutException ex) {
        logger.warn("Timeout error: {}", ex.getMessage());
        return Mono.just(Map.of("scanAvailable", true));
    }

    /**
     * Handle general exceptions
     * @param ex Exception thrown
     * @return Error response
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<Map<String, Boolean>> handleException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage());
        return Mono.just(Map.of("scanAvailable", true));
    }
} 