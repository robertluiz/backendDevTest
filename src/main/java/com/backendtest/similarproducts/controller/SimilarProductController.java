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

import java.util.List;

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
        logger.info("Request received for similar products of: {}", productId);
        return similarProductService.getSimilarProducts(productId);
    }

    /**
     * Handle not found exceptions
     * @param ex Exception thrown
     * @return Error response
     */
    @ExceptionHandler(WebClientResponseException.NotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<String> handleNotFoundException(WebClientResponseException.NotFound ex) {
        logger.error("Product not found error: {}", ex.getMessage());
        return Mono.just("Product not found");
    }

    /**
     * Handle general exceptions
     * @param ex Exception thrown
     * @return Error response
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<String> handleException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage());
        return Mono.just("An unexpected error occurred");
    }
} 