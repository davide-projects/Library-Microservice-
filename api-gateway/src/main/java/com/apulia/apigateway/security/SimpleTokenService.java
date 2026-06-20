package com.apulia.apigateway.security;

import org.springframework.stereotype.Service;

@Service
public class SimpleTokenService {

    private final TokenProperties tokenProperties;

    public SimpleTokenService(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
    }

    public boolean isValid(String token) {
        return tokenProperties.getValid().contains(token);
    }
}
