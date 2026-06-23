CREATE TABLE loans (
    id          INT         NOT NULL AUTO_INCREMENT,
    book_id     INT         NOT NULL,
    member_id   INT         NOT NULL,
    loan_date   DATE        NOT NULL,
    return_date DATE        NULL,
    PRIMARY KEY (id)
);
