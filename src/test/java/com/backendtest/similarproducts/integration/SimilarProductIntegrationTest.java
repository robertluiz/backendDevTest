package com.backendtest.similarproducts.integration;

import com.backendtest.similarproducts.model.ProductDetail;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SimilarProductIntegrationTest {

    private static MockWebServer mockWebServer;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("api.product.similarids.url", 
                () -> String.format("http://localhost:%s/product/{productId}/similarids", mockWebServer.getPort()));
        registry.add("api.product.detail.url", 
                () -> String.format("http://localhost:%s/product/{productId}", mockWebServer.getPort()));
    }

    @BeforeEach
    void setUpEach() {
        // Reset any previous requests
        mockWebServer.getRequestCount();
    }

    @Test
    void shouldReturnSimilarProducts() {
        // Given: Setup mock responses
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\":\"1\",\"name\":\"Product 1\",\"price\":10.0,\"availability\":true}")
                .addHeader("Content-Type", "application/json"));
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[\"2\", \"3\", \"4\"]")
                .addHeader("Content-Type", "application/json"));
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\":\"2\",\"name\":\"Product 2\",\"price\":20.0,\"availability\":true}")
                .addHeader("Content-Type", "application/json"));
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\":\"3\",\"name\":\"Product 3\",\"price\":30.0,\"availability\":false}")
                .addHeader("Content-Type", "application/json"));
                
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\":\"4\",\"name\":\"Product 4\",\"price\":40.0,\"availability\":true}")
                .addHeader("Content-Type", "application/json"));

        // When: Call our API
        String url = UriComponentsBuilder.fromHttpUrl("http://localhost:" + port + "/product/1/similar").toUriString();
        ResponseEntity<List<ProductDetail>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductDetail>>() {});

        // Then: Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
        
        ProductDetail product1 = response.getBody().get(0);
        assertEquals("2", product1.getId());
        assertEquals("Product 2", product1.getName());
        assertEquals(20.0, product1.getPrice());
        assertTrue(product1.getAvailability());
        
        ProductDetail product2 = response.getBody().get(1);
        assertEquals("3", product2.getId());
        assertEquals("Product 3", product2.getName());
        assertEquals(30.0, product2.getPrice());
        assertFalse(product2.getAvailability());
        
        ProductDetail product3 = response.getBody().get(2);
        assertEquals("4", product3.getId());
        assertEquals("Product 4", product3.getName());
        assertEquals(40.0, product3.getPrice());
        assertTrue(product3.getAvailability());
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() {
        // Given: Setup mock responses
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"scanAvailable\":true}")
                .addHeader("Content-Type", "application/json"));

        // When: Call our API
        String url = UriComponentsBuilder.fromHttpUrl("http://localhost:" + port + "/product/999/similar").toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class);

        // Then: Verify response
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("{\"scanAvailable\":true}", response.getBody());
    }
} 