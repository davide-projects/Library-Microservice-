package com.apulia.loanservice.dto;

public class BookClientDTO {

    private Integer id;
    private String title;
    private String author;
    private String publisher;

    public BookClientDTO() {}

    public BookClientDTO(Integer id, String title, String author, String publisher) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
    }

    // GETTERS
    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getPublisher() {
        return publisher;
    }

    // SETTERS
    public void setId(Integer id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }
}
