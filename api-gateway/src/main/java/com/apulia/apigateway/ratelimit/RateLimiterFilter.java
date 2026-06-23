package com.apulia.apigateway.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(-1)
public class RateLimiterFilter implements WebFilter {

    private final RateLimiterConfig config;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterFilter(RateLimiterConfig config) {
        this.config = config;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String clientIp = getClientIp(exchange);
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);

        if (bucket.tryConsume(1)) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(config.getRefillSeconds()));
        return exchange.getResponse().setComplete();
    }

    private Bucket createBucket(String clientIp) {
        Refill refill = Refill.intervally(config.getRefillTokens(), Duration.ofSeconds(config.getRefillSeconds()));
        Bandwidth limit = Bandwidth.classic(config.getCapacity(), refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String remoteAddress = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        return remoteAddress;
    }
}
