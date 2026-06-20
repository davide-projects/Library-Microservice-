package com.apulia.apigateway.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class BearerTokenFilter implements WebFilter {

    private final com.apulia.apigateway.security.SimpleTokenService tokenService;

    public BearerTokenFilter(com.apulia.apigateway.security.SimpleTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (auth == null || !auth.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = auth.substring(7);

        if (!tokenService.isValid(token)) {
            return unauthorized(exchange);
        }

        return chain.filter(exchange);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
