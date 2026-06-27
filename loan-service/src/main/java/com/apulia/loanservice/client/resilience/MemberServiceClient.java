package com.apulia.loanservice.client.resilience;

import com.apulia.loanservice.client.MemberClient;
import com.apulia.loanservice.dto.MemberDTO;
import com.apulia.loanservice.exception.ServiceUnavailableException;
import com.apulia.loanservice.exception.ValidationException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MemberServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(MemberServiceClient.class);

    private final MemberClient memberClient;

    public MemberServiceClient(MemberClient memberClient) {
        this.memberClient = memberClient;
    }

    @CircuitBreaker(name = "memberService", fallbackMethod = "memberFallback")
    @Retry(name = "memberService")
    public MemberDTO getMemberById(Integer memberId) {
        return memberClient.getMemberById(memberId);
    }

    public MemberDTO memberFallback(Integer memberId, Throwable t) {
        if (t instanceof ValidationException) {
            throw (ValidationException) t;
        }
        logger.error("Member service is unavailable for ID {}. Fallback triggered: {}", memberId, t.getMessage());
        throw new ServiceUnavailableException("Member service is currently unavailable. Please try again later.");
    }
}
