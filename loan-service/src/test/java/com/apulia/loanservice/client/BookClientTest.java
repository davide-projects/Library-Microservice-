package com.apulia.loanservice.client;

import com.apulia.loanservice.dto.BookClientDTO;
import com.apulia.loanservice.exception.ValidationException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

class BookClientTest {

    private WireMockServer wireMock;
    private BookClient bookClient;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();

        bookClient = new BookClient(new RestTemplate(), "http://localhost:" + wireMock.port() + "/book");
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void getBookById_shouldReturnBook_whenServiceResponds200() {
        wireMock.stubFor(get(urlEqualTo("/book/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"title\":\"Test Book\",\"author\":\"Author\",\"publisher\":\"Publisher\"}")));

        BookClientDTO book = bookClient.getBookById(1);

        assertNotNull(book);
        assertEquals(1, book.getId());
        assertEquals("Test Book", book.getTitle());
        assertEquals("Author", book.getAuthor());
    }

    @Test
    void getBookById_shouldThrowValidationException_whenServiceResponds404() {
        wireMock.stubFor(get(urlEqualTo("/book/999"))
                .willReturn(aResponse().withStatus(404)));

        assertThrows(ValidationException.class, () -> bookClient.getBookById(999));
    }

    @Test
    void getBookById_shouldPropagateException_whenServiceResponds500() {
        wireMock.stubFor(get(urlEqualTo("/book/1"))
                .willReturn(aResponse().withStatus(500)));

        assertThrows(Exception.class, () -> bookClient.getBookById(1));
    }

    @Test
    void getBookById_shouldPropagateException_whenServiceIsDown() {
        wireMock.stop();

        assertThrows(Exception.class, () -> bookClient.getBookById(1));
    }
}
