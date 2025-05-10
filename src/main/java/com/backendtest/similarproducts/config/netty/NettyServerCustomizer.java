package com.backendtest.similarproducts.config.netty;

import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Customiza la configuración del servidor Netty para un rendimiento óptimo bajo carga alta
 */
@Configuration
public class NettyServerCustomizer implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {

    @Value("${server.netty.connection.backlog:4096}")
    private int connectionBacklog;

    @Value("${server.netty.connection.timeout:30000}")
    private int connectionTimeout;

    @Value("${server.netty.worker.count:0}")
    private int workerCount;

    @Value("${server.netty.boss.count:1}")
    private int bossCount;

    @Override
    public void customize(NettyReactiveWebServerFactory factory) {
        factory.addServerCustomizers(httpServer -> {
            httpServer = httpServer.option(ChannelOption.SO_BACKLOG, connectionBacklog)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_REUSEADDR, true);
            
            return httpServer;
        });
    }

    private EventLoopGroup createBossEventLoopGroup(boolean epoll) {
        ThreadFactory threadFactory = new CustomThreadFactory("netty-boss");
        return epoll 
            ? new EpollEventLoopGroup(bossCount, threadFactory)
            : new NioEventLoopGroup(bossCount, threadFactory);
    }

    private EventLoopGroup createWorkerEventLoopGroup(boolean epoll) {
        ThreadFactory threadFactory = new CustomThreadFactory("netty-worker");
        int count = workerCount > 0 ? workerCount : Runtime.getRuntime().availableProcessors() * 2;
        return epoll 
            ? new EpollEventLoopGroup(count, threadFactory)
            : new NioEventLoopGroup(count, threadFactory);
    }

    /**
     * Factory para crear hilos con nombres personalizados para mejor diagnóstico
     */
    private static class CustomThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicLong counter = new AtomicLong(0);

        public CustomThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
} 