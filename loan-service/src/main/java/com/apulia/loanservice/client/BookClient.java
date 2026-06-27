package com.apulia.loanservice.client;

import com.apulia.loanservice.dto.BookClientDTO;
import com.apulia.loanservice.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class BookClient {

    private static final Logger logger = LoggerFactory.getLogger(BookClient.class);

    private final RestTemplate restTemplate;

    private String bookServiceUrl;

    public BookClient(RestTemplate restTemplate, @Value("${microservices.book-service.url}") String bookServiceUrl) {
        this.restTemplate = restTemplate;
        this.bookServiceUrl = bookServiceUrl;
    }

    void setBookServiceUrl(String bookServiceUrl) {
        this.bookServiceUrl = bookServiceUrl;
    }

    public BookClientDTO getBookById(Integer bookId) {
        try {
            String url = bookServiceUrl + "/" + bookId;
            return restTemplate.getForObject(url, BookClientDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            logger.error("Book not found for ID {}: {}", bookId, e.getMessage());
            throw new ValidationException("Book with ID " + bookId + " does not exist");
        }
    }
}
