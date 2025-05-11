package com.backendtest.similarproducts.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for WebClient with optimal performance settings
 */
@Configuration
public class WebClientConfig {

    @Value("${webclient.max-connections:1000}")
    private int maxConnections;
    
    @Value("${webclient.acquisition-timeout:1500}")
    private int acquisitionTimeout;
    
    @Value("${webclient.max-idle-time:15000}")
    private int maxIdleTime;
    
    @Value("${webclient.evict-interval:2000}")
    private int evictInterval;
    
    @Value("${webclient.connect-timeout:1000}")
    private int connectTimeout;
    
    @Value("${webclient.response-timeout:2000}")
    private int responseTimeout;
    
    @Value("${webclient.read-timeout:2000}")
    private int readTimeout;
    
    @Value("${webclient.write-timeout:2000}")
    private int writeTimeout;
    
    @Value("${webclient.max-memory-size:16777216}")
    private int maxMemorySize;
    
    @Value("${webclient.netty.selector-threads:2}")
    private int nettyEventLoopSelectorThreads;
    
    @Value("${webclient.netty.worker-threads:8}")
    private int nettyEventLoopWorkerThreads;

    @Bean
    public WebClient webClient() {
        ConnectionProvider provider = ConnectionProvider.builder("optimized-conn-pool")
                .maxConnections(maxConnections)
                .pendingAcquireTimeout(Duration.ofMillis(acquisitionTimeout))
                .maxIdleTime(Duration.ofMillis(maxIdleTime))
                .lifo() 
                .evictInBackground(Duration.ofMillis(evictInterval))
                .metrics(true)
                .build();

        LoopResources loop = LoopResources.create(
                "webclient-event-loop", 
                nettyEventLoopSelectorThreads, 
                nettyEventLoopWorkerThreads, 
                true
        );

        HttpClient httpClient = HttpClient.create(provider)
                .runOn(loop)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .responseTimeout(Duration.ofMillis(responseTimeout))
                .doOnConnected(conn -> 
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS)))
                .compress(true)
                .keepAlive(true)
                .wiretap(false); 
        
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(maxMemorySize);
                    configurer.defaultCodecs().enableLoggingRequestDetails(false);
                })
                .build();
        
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .filter(logRequest())
                .build();
    }
    
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (clientRequest.url().getPath().contains("product")) {
                return Mono.just(clientRequest);
            }
            return Mono.just(clientRequest);
        });
    }
} 