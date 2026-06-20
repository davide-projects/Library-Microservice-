package com.apulia.apigateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "security.tokens")
public class TokenProperties {
    private List<String> valid;

    public List<String> getValid() {
        return valid;
    }

    public void setValid(List<String> valid) {
        this.valid = valid;
    }
}
