package com.apulia.bookservice.controller;

import com.apulia.bookservice.dto.BookDTO;
import com.apulia.bookservice.model.Book;
import com.apulia.bookservice.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/book")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // -------------------------
    // MAPPER
    // -------------------------
    private BookDTO toDTO(Book book) {
        return new BookDTO(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher()
        );
    }

    private Book toEntity(BookDTO dto) {
        return new Book(
                dto.getTitle(),
                dto.getAuthor(),
                dto.getPublisher()
        );
    }

    // -------------------------
    // GET ALL
    // -------------------------
    @GetMapping
    public ResponseEntity<List<BookDTO>> getAllBooks() {
        List<BookDTO> dtos = bookService.getAllBooks()
                .stream()
                .map(this::toDTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // -------------------------
    // GET BY ID
    // -------------------------
    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(@PathVariable("id") int id) {
        return ResponseEntity.ok(toDTO(bookService.getBookById(id)));
    }

    // -------------------------
    // CREATE
    // -------------------------
    @PostMapping
    public ResponseEntity<BookDTO> addBook(@Valid @RequestBody BookDTO dto) {
        Book saved = bookService.addBook(toEntity(dto));
        URI location = URI.create("/book/" + saved.getId());
        return ResponseEntity.created(location).body(toDTO(saved));
    }

    // -------------------------
    // UPDATE (PUT)
    // -------------------------
    @PutMapping("/{id}")
    public ResponseEntity<BookDTO> updateBook(
            @PathVariable("id") int id,
            @Valid @RequestBody BookDTO dto) {

        Book updated = bookService.updateBook(id, toEntity(dto));
        return ResponseEntity.ok(toDTO(updated));
    }

    // -------------------------
    // PARTIAL UPDATE (PATCH)
    // -------------------------
    @PatchMapping("/{id}")
    public ResponseEntity<BookDTO> patchBook(
            @PathVariable("id") int id,
            @RequestBody Map<String, Object> updates) {

        Book updated = bookService.patchBook(id, updates);
        return ResponseEntity.ok(toDTO(updated));
    }

    // -------------------------
    // DELETE
    // -------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable("id") int id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------
    // SEARCH
    // -------------------------
    @GetMapping("/search")
    public ResponseEntity<List<BookDTO>> searchBooks(
            // Queste righe qui servono per cercare i libri per titolo o autore
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "author", required = false) String author) {

        List<BookDTO> dtos = bookService.searchByAuthorAndTitle(author, title)
                .stream()
                .map(this::toDTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }
}
