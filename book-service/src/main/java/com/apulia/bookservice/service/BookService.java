package com.apulia.bookservice.service;

import com.apulia.bookservice.dto.BookPatchDTO;
import com.apulia.bookservice.exception.BookNotFoundException;
import com.apulia.bookservice.exception.SearchException;
import com.apulia.bookservice.model.Book;
import com.apulia.bookservice.repository.BookRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository){
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    @Bulkhead(name = "bookService")
    @RateLimiter(name = "bookService")
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Bulkhead(name = "bookService")
    @RateLimiter(name = "bookService")
    public Book getBookById(Integer id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    @Bulkhead(name = "bookService")
    @RateLimiter(name = "bookService")
    public Book addBook(Book book) {
        return bookRepository.save(book);
    }

    @Transactional
    @Bulkhead(name = "bookService")
    @RateLimiter(name = "bookService")
    public Book updateBook(int id, Book book) {
        Book existingBook = getBookById(id);

        existingBook.setTitle(book.getTitle());
        existingBook.setAuthor(book.getAuthor());
        existingBook.setPublisher(book.getPublisher());

        return bookRepository.save(existingBook);
    }

    @Transactional
    @Bulkhead(name = "bookService")
    @RateLimiter(name = "bookService")
    public Book patchBook(int id, BookPatchDTO patch) {

        Book existingBook = getBookById(id);

        if (patch.getTitle() != null) {
            existingBook.setTitle(patch.getTitle().trim());
        }
        if (patch.getAuthor() != null) {
            existingBook.setAuthor(patch.getAuthor().trim());
        }
        if (patch.getPublisher() != null) {
            existingBook.setPublisher(patch.getPublisher().trim());
        }

        return bookRepository.save(existingBook);
    }

    @Transactional
    @Bulkhead(name = "bookService")
    @RateLimiter(name = "bookService")
    public void deleteBook(int id) {
        Book existingBook = getBookById(id);
        bookRepository.delete(existingBook);
    }

    @Transactional(readOnly = true)
    @Bulkhead(name = "bookService")
    @RateLimiter(name = "bookService")
    public List<Book> searchByAuthorAndTitle(String author, String title) {

        boolean hasAuthor = (author != null && !author.isBlank());
        boolean hasTitle  = (title != null && !title.isBlank());

        if (!hasAuthor && !hasTitle) {
            throw new SearchException("You must specify at least 'author' or 'title'");
        }

        List<Book> results;

        if (hasAuthor && !hasTitle) {
            results = bookRepository.findByAuthorContainingIgnoreCase(author);
        }
        else if (!hasAuthor) {
            results = bookRepository.findByTitleContainingIgnoreCase(title);
        }
        else {
            results = bookRepository
                    .findByAuthorContainingIgnoreCaseAndTitleContainingIgnoreCase(author, title);
        }

        if (results.isEmpty()) {
            throw new SearchException("No books found with the provided criteria");
        }

        return results;
    }
}
