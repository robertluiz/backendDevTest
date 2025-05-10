package com.backendtest.similarproducts.service;

import com.backendtest.similarproducts.client.ProductClient;
import com.backendtest.similarproducts.model.ProductDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimilarProductServiceTest {

    @Mock
    private ProductClient productClient;

    private SimilarProductService similarProductService;

    @BeforeEach
    void setUp() {
        similarProductService = new SimilarProductService(productClient);
    }

    @Test
    void shouldGetSimilarProducts() {
        // Given
        String productId = "1";
        ProductDetail product1 = new ProductDetail("1", "Product 1", 10.0, true);
        ProductDetail product2 = new ProductDetail("2", "Product 2", 20.0, true);
        ProductDetail product3 = new ProductDetail("3", "Product 3", 30.0, false);
        
        List<String> similarIds = Arrays.asList("2", "3");
        
        when(productClient.getProductDetail(productId)).thenReturn(Mono.just(product1));
        when(productClient.getSimilarProductIds(productId)).thenReturn(Mono.just(similarIds));
        when(productClient.getProductDetail("2")).thenReturn(Mono.just(product2));
        when(productClient.getProductDetail("3")).thenReturn(Mono.just(product3));
        
        // When & Then
        similarProductService.getSimilarProducts(productId)
                .as(StepVerifier::create)
                .consumeNextWith(productList -> {
                    assertThat(productList).hasSize(2);
                    assertThat(productList).containsExactlyInAnyOrder(product2, product3);
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleProductNotFound() {
        // Given
        String productId = "999";
        
        when(productClient.getProductDetail(productId)).thenReturn(
                Mono.error(WebClientResponseException.create(404, "Not Found", null, null, null)));
        
        // When & Then
        StepVerifier.create(similarProductService.getSimilarProducts(productId))
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void shouldHandleSimilarProductNotFound() {
        // Given
        String productId = "1";
        ProductDetail product1 = new ProductDetail("1", "Product 1", 10.0, true);
        ProductDetail product2 = new ProductDetail("2", "Product 2", 20.0, true);
        
        List<String> similarIds = Arrays.asList("2", "999");
        
        when(productClient.getProductDetail(productId)).thenReturn(Mono.just(product1));
        when(productClient.getSimilarProductIds(productId)).thenReturn(Mono.just(similarIds));
        when(productClient.getProductDetail("2")).thenReturn(Mono.just(product2));
        when(productClient.getProductDetail("999")).thenReturn(
                Mono.error(WebClientResponseException.create(404, "Not Found", null, null, null)));
        
        // When & Then
        StepVerifier.create(similarProductService.getSimilarProducts(productId))
                .expectNext(Arrays.asList(product2))
                .verifyComplete();
    }
} 