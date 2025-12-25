package com.trabix.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtro global que registra todas las peticiones que pasan por el gateway.
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        long startTime = System.currentTimeMillis();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String clientIp = request.getRemoteAddress() != null ? 
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";

        log.info(">>> {} {} - IP: {}", method, path, clientIp);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = exchange.getResponse().getStatusCode() != null ? 
                    exchange.getResponse().getStatusCode().value() : 0;
            
            log.info("<<< {} {} - Status: {} - {}ms", method, path, statusCode, duration);
        }));
    }

    @Override
    public int getOrder() {
        return -1; // Ejecutar primero
    }
}
