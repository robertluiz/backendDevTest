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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimilarProductServiceTest {

    @Mock
    private ProductClient productClient;

    private SimilarProductService similarProductService;

    @BeforeEach
    void setUp() {
        // Crear el servicio manualmente con un valor para responseTimeout
        similarProductService = new SimilarProductService(productClient, 3000);
    }

    @Test
    void shouldGetSimilarProducts() {
        // Given
        String productId = "1";
        ProductDetail originalProduct = new ProductDetail("1", "Product 1", 10.0, true);
        List<String> similarIds = Arrays.asList("2", "3");
        ProductDetail product1 = new ProductDetail("2", "Product 2", 20.0, true);
        ProductDetail product2 = new ProductDetail("3", "Product 3", 30.0, false);
        
        when(productClient.getProductDetail(productId))
            .thenReturn(Mono.just(originalProduct));
        when(productClient.getSimilarProductIds(productId))
            .thenReturn(Mono.just(similarIds));
        when(productClient.getProductDetail("2"))
            .thenReturn(Mono.just(product1));
        when(productClient.getProductDetail("3"))
            .thenReturn(Mono.just(product2));
        
        // When & Then
        StepVerifier.create(similarProductService.getSimilarProducts(productId))
                .expectNextMatches(products -> {
                    // Verifica que la lista contiene exactamente 2 elementos
                    if (products.size() != 2) return false;
                    
                    // Verifica que contiene los productos esperados sin importar el orden
                    return products.stream().anyMatch(p -> p.getId().equals("2")) &&
                           products.stream().anyMatch(p -> p.getId().equals("3"));
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyListWhenNoSimilarProductsExist() {
        // Given
        String productId = "1";
        ProductDetail originalProduct = new ProductDetail("1", "Product 1", 10.0, true);
        
        when(productClient.getProductDetail(productId))
            .thenReturn(Mono.just(originalProduct));
        when(productClient.getSimilarProductIds(productId))
            .thenReturn(Mono.just(Collections.emptyList()));
        
        // When & Then
        StepVerifier.create(similarProductService.getSimilarProducts(productId))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }

    @Test
    void shouldHandleProductNotFound() {
        // Given
        String productId = "999";
        
        when(productClient.getProductDetail(productId))
            .thenReturn(Mono.error(WebClientResponseException.create(404, "Not Found", null, null, null)));
        
        // When & Then
        StepVerifier.create(similarProductService.getSimilarProducts(productId))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }

    @Test
    void shouldHandleTimeout() {
        // Given
        String productId = "1";
        ProductDetail originalProduct = new ProductDetail("1", "Product 1", 10.0, true);
        
        when(productClient.getProductDetail(productId))
            .thenReturn(Mono.just(originalProduct));
        when(productClient.getSimilarProductIds(productId))
            .thenReturn(Mono.error(new TimeoutException("Timeout occurred")));
        
        // When & Then
        StepVerifier.create(similarProductService.getSimilarProducts(productId))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }

    @Test
    void shouldHandleSimilarProductDetailNotFound() {
        // Given
        String productId = "1";
        ProductDetail originalProduct = new ProductDetail("1", "Product 1", 10.0, true);
        List<String> similarIds = Arrays.asList("2", "3");
        ProductDetail product2 = new ProductDetail("3", "Product 3", 30.0, false);
        
        when(productClient.getProductDetail(productId))
            .thenReturn(Mono.just(originalProduct));
        when(productClient.getSimilarProductIds(productId))
            .thenReturn(Mono.just(similarIds));
        when(productClient.getProductDetail("2"))
            .thenReturn(Mono.error(WebClientResponseException.create(404, "Not Found", null, null, null)));
        when(productClient.getProductDetail("3"))
            .thenReturn(Mono.just(product2));
        
        // When & Then
        StepVerifier.create(similarProductService.getSimilarProducts(productId))
                .expectNext(Arrays.asList(product2))
                .verifyComplete();
    }
} 