package com.apulia.loanservice.client;

import com.apulia.loanservice.dto.MemberDTO;
import com.apulia.loanservice.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class MemberClient {

    private static final Logger logger = LoggerFactory.getLogger(MemberClient.class);

    private final RestTemplate restTemplate;

    private String memberServiceUrl;

    public MemberClient(RestTemplate restTemplate, @Value("${microservices.member-service.url}") String memberServiceUrl) {
        this.restTemplate = restTemplate;
        this.memberServiceUrl = memberServiceUrl;
    }

    void setMemberServiceUrl(String memberServiceUrl) {
        this.memberServiceUrl = memberServiceUrl;
    }

    public MemberDTO getMemberById(Integer memberId) {
        try {
            String url = memberServiceUrl + "/" + memberId;
            return restTemplate.getForObject(url, MemberDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            logger.error("Member not found for ID {}: {}", memberId, e.getMessage());
            throw new ValidationException("Member with ID " + memberId + " does not exist");
        }
    }
}
