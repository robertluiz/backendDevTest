package com.backendtest.similarproducts.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
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

    @Value("${webclient.max-connections:500}")
    private int maxConnections;
    
    @Value("${webclient.acquisition-timeout:3000}")
    private int acquisitionTimeout;
    
    @Value("${webclient.max-idle-time:20000}")
    private int maxIdleTime;
    
    @Value("${webclient.evict-interval:5000}")
    private int evictInterval;
    
    @Value("${webclient.connect-timeout:2000}")
    private int connectTimeout;
    
    @Value("${webclient.response-timeout:3000}")
    private int responseTimeout;
    
    @Value("${webclient.read-timeout:3000}")
    private int readTimeout;
    
    @Value("${webclient.write-timeout:3000}")
    private int writeTimeout;
    
    @Value("${webclient.max-memory-size:16777216}")
    private int maxMemorySize;
    
    @Value("${webclient.netty.selector-threads:1}")
    private int nettyEventLoopSelectorThreads;
    
    @Value("${webclient.netty.worker-threads:0}")
    private int nettyEventLoopWorkerThreads;

    @Bean
    public WebClient webClient() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("optimized-conn-pool")
                .maxConnections(maxConnections)
                .pendingAcquireTimeout(Duration.ofMillis(acquisitionTimeout))
                .maxIdleTime(Duration.ofMillis(maxIdleTime))
                .lifo() 
                .evictInBackground(Duration.ofMillis(evictInterval))
                .build();
        
        LoopResources loopResources = LoopResources.create(
                "webclient-event-loop", 
                nettyEventLoopSelectorThreads, 
                nettyEventLoopWorkerThreads, 
                true
        );
        
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, 4096) 
                .responseTimeout(Duration.ofMillis(responseTimeout))
                .doOnConnected(conn -> 
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS)))
                .compress(true) 
                .runOn(loopResources); 
        
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(maxMemorySize);
                })
                .build();
        
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .build();
    }
} 