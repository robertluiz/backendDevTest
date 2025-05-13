package com.backendtest.similarproducts.integration;

import com.backendtest.similarproducts.client.ProductClient;
import com.backendtest.similarproducts.model.ProductDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "cache.name.similar-products=similarProducts",
    "cache.name.product-details=productDetails",
    "cache.name.similar-ids=similarIds",
    "cache.name.product-detail-optimized=productDetailOptimized",
    "cache.name.short-lived=shortLivedCache",
    "circuit-breaker.name.product-api=productApi"
})
class SimilarProductIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    
    @MockBean
    private ProductClient productClient;
    
    @BeforeEach
    void setUp() {
        reset(productClient);
    }

    @Test
    void shouldReturnOnlyAvailableProducts() {
        // Configure mocks
        ProductDetail product1 = new ProductDetail("1", "Product 1", 10.0, true);
        when(productClient.getProductDetail("1")).thenReturn(Mono.just(product1));
        when(productClient.getSimilarProductIds("1")).thenReturn(Mono.just(Arrays.asList("2", "3")));
        
        // Product 2 with timeout
        when(productClient.getProductDetail("2")).thenReturn(Mono.error(new TimeoutException("Timeout")));
        
        // Product 3 available
        ProductDetail product3 = new ProductDetail("3", "Product 3", 30.0, false);
        when(productClient.getProductDetail("3")).thenReturn(Mono.just(product3));
        
        // Make HTTP request
        String url = "http://localhost:" + port + "/product/1/similar";
        ResponseEntity<List<ProductDetail>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductDetail>>() {});

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("3", response.getBody().get(0).getId());
    }

    @Test
    void shouldReturnEmptyListWhenMainProductNotFound() {
        // Configure mock for product not found
        when(productClient.getProductDetail("999")).thenReturn(
            Mono.error(new WebClientResponseException(404, "Not Found", null, null, null))
        );
        
        // Make HTTP request
        String url = "http://localhost:" + port + "/product/999/similar";
        ResponseEntity<List<ProductDetail>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductDetail>>() {});

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }
    
    @Test
    void shouldHandleTimeoutGracefully() {
        // Configure existing product with error in similar IDs
        ProductDetail product1 = new ProductDetail("1", "Product 1", 10.0, true);
        when(productClient.getProductDetail("1")).thenReturn(Mono.just(product1));
        
        // Simulate error when getting similar IDs
        when(productClient.getSimilarProductIds("1")).thenReturn(
            Mono.error(new RuntimeException("Error getting similar IDs"))
        );
        
        // Make HTTP request
        String url = "http://localhost:" + port + "/product/1/similar";
        ResponseEntity<List<ProductDetail>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductDetail>>() {});

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
    
    @Test
    void shouldReturnPartialResultsWhenSomeProductDetailsAreUnavailable() {
        // Configure for existing product
        ProductDetail product1 = new ProductDetail("1", "Product 1", 10.0, true);
        when(productClient.getProductDetail("1")).thenReturn(Mono.just(product1));
        when(productClient.getSimilarProductIds("1")).thenReturn(Mono.just(Arrays.asList("2", "3")));
        
        // Product 2 with timeout, Product 3 available
        when(productClient.getProductDetail("2")).thenReturn(
            Mono.error(new TimeoutException("Timeout for product 2"))
        );
        ProductDetail product3 = new ProductDetail("3", "Product 3", 30.0, false);
        when(productClient.getProductDetail("3")).thenReturn(Mono.just(product3));
        
        // Make HTTP request
        String url = "http://localhost:" + port + "/product/1/similar";
        ResponseEntity<List<ProductDetail>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductDetail>>() {});

        // Verify response - should contain only product 3
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("3", response.getBody().get(0).getId());
    }
} 