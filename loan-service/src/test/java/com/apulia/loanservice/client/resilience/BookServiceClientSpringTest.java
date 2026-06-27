package com.apulia.loanservice.client.resilience;

import com.apulia.loanservice.dto.BookClientDTO;
import com.apulia.loanservice.exception.ServiceUnavailableException;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class BookServiceClientSpringTest {

    private static WireMockServer wireMock;

    @Autowired
    private BookServiceClient bookServiceClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();

        registry.add("microservices.book-service.url", () -> "http://localhost:" + wireMock.port() + "/book");
        registry.add("microservices.member-service.url", () -> "http://localhost:1/members");
    }

    @AfterAll
    static void cleanup() {
        if (wireMock != null) wireMock.stop();
    }

    @BeforeEach
    void reset() {
        wireMock.resetAll();
        circuitBreakerRegistry.getAllCircuitBreakers()
                .forEach(cb -> cb.reset());
    }

    @Test
    void getBookById_shouldReturnBook_onSuccess() {
        wireMock.stubFor(get(urlEqualTo("/book/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"title\":\"Test Book\",\"author\":\"Author\",\"publisher\":\"Publisher\"}")));

        BookClientDTO book = bookServiceClient.getBookById(1);

        assertThat(book).isNotNull();
        assertThat(book.getId()).isEqualTo(1);
    }

    @Test
    void getBookById_shouldThrowServiceUnavailable_whenServiceFails() {
        wireMock.stubFor(get(urlEqualTo("/book/1"))
                .willReturn(aResponse().withStatus(500)));

        assertThrows(ServiceUnavailableException.class, () -> bookServiceClient.getBookById(1));
    }

}
