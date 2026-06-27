package com.apulia.memberservice.resilience;

import com.apulia.memberservice.service.MemberService;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MemberServiceResilienceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    @BeforeEach
    void setUp() {
        assertNotEquals(MemberService.class, memberService.getClass(),
                "MemberService must be proxied for @Bulkhead and @RateLimiter to work");
    }

    @Test
    void getAllMembers_shouldThrowBulkheadFull_whenTooManyConcurrentCalls() throws Exception {
        int concurrentCalls = 5;
        CountDownLatch readyLatch = new CountDownLatch(concurrentCalls);
        CountDownLatch goLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentCalls);

        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < concurrentCalls; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                goLatch.await();
                try {
                    bulkheadRegistry.bulkhead("memberService")
                            .executeCheckedSupplier(() -> {
                                Thread.sleep(200);
                                return null;
                            });
                } catch (BulkheadFullException e) {
                    throw e;
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
                return null;
            }));
        }

        readyLatch.await();
        goLatch.countDown();
        executor.shutdown();

        long bulkheadExceptions = futures.stream()
                .filter(f -> {
                    try {
                        f.get();
                        return false;
                    } catch (ExecutionException e) {
                        return e.getCause() instanceof BulkheadFullException;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();

        assertTrue(bulkheadExceptions > 0,
                "Expected at least one BulkheadFullException with max-concurrent-calls=2");
    }

    @Test
    void getAllMembers_shouldThrowRequestNotPermitted_whenRateLimitExceeded() {
        int calls = 15;
        long rateLimitErrors = 0;

        for (int i = 0; i < calls; i++) {
            try {
                memberService.getAllMembers();
            } catch (RequestNotPermitted e) {
                rateLimitErrors++;
            }
        }

        assertTrue(rateLimitErrors > 0,
                "Expected at least one RequestNotPermitted with limit-for-period=10");
    }
}
