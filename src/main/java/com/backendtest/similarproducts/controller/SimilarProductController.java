package com.backendtest.similarproducts.controller;

import com.backendtest.similarproducts.model.ProductDetail;
import com.backendtest.similarproducts.service.SimilarProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * Controller for similar products API
 */
@Slf4j
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class SimilarProductController {

    private final SimilarProductService similarProductService;
    
    @Value("${log.message.controller-similar-request}")
    private String logSimilarRequest;

    /**
     * Get similar products for a given product ID
     * @param productId Product ID to find similar products for
     * @return List of similar product details
     */
    @GetMapping(value = "/{productId}/similar", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<ProductDetail>> getSimilarProducts(@PathVariable String productId) {
        log.debug(logSimilarRequest, productId);
        
        return similarProductService.getSimilarProducts(productId)
                .onErrorReturn(Collections.emptyList());
    }
} 