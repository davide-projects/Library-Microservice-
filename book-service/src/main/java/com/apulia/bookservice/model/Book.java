package com.apulia.bookservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Forza L'ordine nella response
@JsonPropertyOrder({"id", "title", "author", "publisher"})
@JsonIgnoreProperties(ignoreUnknown = false)

@Entity(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Field 'title' is required")
    @Size(min = 1, max = 255,
            message = "Title must be between 1 and 255 characters")
    @Column(nullable = false, name = "title")
    private String title;


    @NotBlank(message = "Field 'author' is required")
    @Size(min = 1, max = 255,
            message = "Author must be between 1 and 255 characters")
    @Column(nullable = false, name = "author")
    private String author;


    @NotBlank(message = "Field 'publisher' is required")
    @Size(min = 1, max = 255,
            message = "Publisher must be between 1 and 255 characters")
    @Column(nullable = false, name = "publisher")
    private String publisher;

    public Book() {}

    public Book(int id, String title, String author, String publisher){
        this.id = id;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
    }

    public Book(String title, String author, String publisher){
        this.title = title;
        this.author = author;
        this.publisher = publisher;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer newId) {
        this.id = newId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publisher='" + publisher + '\'' +
                '}';
    }
}
