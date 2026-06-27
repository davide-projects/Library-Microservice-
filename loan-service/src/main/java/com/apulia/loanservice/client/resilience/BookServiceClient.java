package com.apulia.loanservice.client.resilience;

import com.apulia.loanservice.client.BookClient;
import com.apulia.loanservice.dto.BookClientDTO;
import com.apulia.loanservice.exception.ServiceUnavailableException;
import com.apulia.loanservice.exception.ValidationException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BookServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(BookServiceClient.class);

    private final BookClient bookClient;

    public BookServiceClient(BookClient bookClient) {
        this.bookClient = bookClient;
    }

    @CircuitBreaker(name = "bookService", fallbackMethod = "bookFallback")
    @Retry(name = "bookService")
    public BookClientDTO getBookById(Integer bookId) {
        return bookClient.getBookById(bookId);
    }

    public BookClientDTO bookFallback(Integer bookId, Throwable t) {
        if (t instanceof ValidationException) {
            throw (ValidationException) t;
        }
        logger.error("Book service is unavailable for ID {}. Fallback triggered: {}", bookId, t.getMessage());
        throw new ServiceUnavailableException("Book service is currently unavailable. Please try again later.");
    }
}
