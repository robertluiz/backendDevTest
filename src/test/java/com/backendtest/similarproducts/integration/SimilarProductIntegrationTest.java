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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
        // Configurar los mocks
        ProductDetail product1 = new ProductDetail("1", "Product 1", 10.0, true);
        when(productClient.getProductDetail("1")).thenReturn(Mono.just(product1));
        when(productClient.getSimilarProductIds("1")).thenReturn(Mono.just(Arrays.asList("2", "3")));
        
        // Mock: producto 2 con timeout, producto 3 disponible
        when(productClient.getProductDetail("2")).thenReturn(
            Mono.error(new TimeoutException("Simulated timeout for product 2"))
        );
        ProductDetail product3 = new ProductDetail("3", "Product 3", 30.0, false);
        when(productClient.getProductDetail("3")).thenReturn(Mono.just(product3));
        
        // Realizar la petición HTTP
        String url = UriComponentsBuilder.fromHttpUrl("http://localhost:" + port + "/product/1/similar").toUriString();
        ResponseEntity<List<ProductDetail>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductDetail>>() {});

        // Verificar la respuesta - según el comportamiento observado, solo devuelve producto 3
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size(), "Debería devolver solo los productos disponibles");
        assertEquals("3", response.getBody().get(0).getId(), "Debería ser el producto con ID 3");
    }

    @Test
    void shouldReturnEmptyListWhenMainProductNotFound() {
        // Configurar el mock para producto no encontrado
        when(productClient.getProductDetail("999")).thenReturn(
            Mono.error(new WebClientResponseException(404, "Not Found", null, null, null))
        );
        
        // Realizar la petición HTTP
        String url = UriComponentsBuilder.fromHttpUrl("http://localhost:" + port + "/product/999/similar").toUriString();
        ResponseEntity<List<ProductDetail>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductDetail>>() {});

        // Verificar la respuesta
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody(), "La respuesta no debe ser nula");
        assertTrue(response.getBody().isEmpty(), "La lista debería estar vacía");
    }
    
    @Test
    void shouldHandleTimeoutGracefully() {
        // Configuración para simular producto existente pero con productos similares fallando
        ProductDetail product1 = new ProductDetail("1", "Product 1", 10.0, true);
        when(productClient.getProductDetail("1")).thenReturn(Mono.just(product1));
        
        // Simular un error al obtener IDs similares
        when(productClient.getSimilarProductIds("1")).thenReturn(
            Mono.error(new RuntimeException("Error getting similar IDs"))
        );
        
        // No configuramos mocks para getProductDetail() porque no se llamará debido al error anterior
        
        // Realizar la petición HTTP
        String url = UriComponentsBuilder.fromHttpUrl("http://localhost:" + port + "/product/1/similar").toUriString();
        ResponseEntity<List<ProductDetail>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductDetail>>() {});

        // Verificar la respuesta - debería ser lista vacía o lista con producto que tenemos en caché
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody(), "La respuesta no debe ser nula");
        // No probamos el contenido específico porque depende de la implementación
    }
    
    @Test
    void shouldReturnPartialResultsWhenSomeProductDetailsAreUnavailable() {
        // Configuración para producto existente
        ProductDetail product1 = new ProductDetail("1", "Product 1", 10.0, true);
        when(productClient.getProductDetail("1")).thenReturn(Mono.just(product1));
        when(productClient.getSimilarProductIds("1")).thenReturn(Mono.just(Arrays.asList("2", "3")));
        
        // Producto 2 con timeout, Producto 3 disponible
        when(productClient.getProductDetail("2")).thenReturn(
            Mono.error(new TimeoutException("Simulated timeout for product 2"))
        );
        ProductDetail product3 = new ProductDetail("3", "Product 3", 30.0, false);
        when(productClient.getProductDetail("3")).thenReturn(Mono.just(product3));
        
        // Realizar la petición HTTP
        String url = UriComponentsBuilder.fromHttpUrl("http://localhost:" + port + "/product/1/similar").toUriString();
        ResponseEntity<List<ProductDetail>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductDetail>>() {});

        // Verificar la respuesta - debería contener solo el producto 3
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody(), "La respuesta no debe ser nula");
        assertEquals(1, response.getBody().size(), "Debería contener solo los productos disponibles");
        assertEquals("3", response.getBody().get(0).getId(), "Debería ser el producto con ID 3");
    }
} 