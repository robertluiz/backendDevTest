package com.backendtest.similarproducts.controller;

import com.backendtest.similarproducts.model.ProductDetail;
import com.backendtest.similarproducts.service.SimilarProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimilarProductControllerTest {

    @Mock
    private SimilarProductService similarProductService;

    @InjectMocks
    private SimilarProductController similarProductController;

    @Test
    void shouldGetSimilarProducts() {
        // Given
        String productId = "1";
        ProductDetail product1 = new ProductDetail("2", "Product 2", 20.0, true);
        ProductDetail product2 = new ProductDetail("3", "Product 3", 30.0, false);
        List<ProductDetail> similarProducts = Arrays.asList(product1, product2);
        
        when(similarProductService.getSimilarProducts(productId)).thenReturn(Mono.just(similarProducts));
        
        // When & Then
        StepVerifier.create(similarProductController.getSimilarProducts(productId))
                .expectNext(similarProducts)
                .verifyComplete();
    }

    @Test
    void shouldHandleNotFoundAndReturnEmptyList() {
        // Given
        String productId = "999";
        
        when(similarProductService.getSimilarProducts(productId)).thenReturn(
                Mono.error(WebClientResponseException.create(HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null)));
        
        // When & Then
        StepVerifier.create(similarProductController.getSimilarProducts(productId))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }
    
    @Test
    void shouldHandleTimeoutAndReturnEmptyList() {
        // Given
        String productId = "1";
        
        when(similarProductService.getSimilarProducts(productId)).thenReturn(
                Mono.error(new TimeoutException("Timeout occurred")));
        
        // When & Then
        StepVerifier.create(similarProductController.getSimilarProducts(productId))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }
    
    @Test
    void shouldHandleGenericErrorAndReturnEmptyList() {
        // Given
        String productId = "1";
        
        when(similarProductService.getSimilarProducts(productId)).thenReturn(
                Mono.error(new RuntimeException("Some error")));
        
        // When & Then
        StepVerifier.create(similarProductController.getSimilarProducts(productId))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }
} 