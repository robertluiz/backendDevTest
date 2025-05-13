package com.backendtest.similarproducts.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Global exception handler for the application
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @Value("${log.message.error-not-found}")
    private String logErrorNotFound;
    
    @Value("${log.message.error-timeout}")
    private String logErrorTimeout;
    
    @Value("${log.message.error-unexpected}")
    private String logErrorUnexpected;
    
    @Value("${response.key.scan-available}")
    private String responseKeyScanAvailable;
    
    @Value("${response.value.scan-available:true}")
    private Boolean responseValueScanAvailable;
    
    /**
     * Handle not found exceptions
     * @param ex Exception thrown
     * @return Error response
     */
    @ExceptionHandler(WebClientResponseException.NotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Map<String, Boolean>> handleNotFoundException(WebClientResponseException.NotFound ex) {
        log.warn(logErrorNotFound, ex.getMessage());
        return Mono.just(Map.of(responseKeyScanAvailable, responseValueScanAvailable));
    }

    /**
     * Handle timeout exceptions
     * @param ex Exception thrown
     * @return Error response
     */
    @ExceptionHandler(TimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public Mono<Map<String, Boolean>> handleTimeoutException(TimeoutException ex) {
        log.warn(logErrorTimeout, ex.getMessage());
        return Mono.just(Map.of(responseKeyScanAvailable, responseValueScanAvailable));
    }

    /**
     * Handle general exceptions
     * @param ex Exception thrown
     * @return Error response
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<Map<String, Boolean>> handleException(Exception ex) {
        log.error(logErrorUnexpected, ex.getMessage());
        return Mono.just(Map.of(responseKeyScanAvailable, responseValueScanAvailable));
    }
} 