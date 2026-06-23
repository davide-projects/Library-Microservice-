CREATE TABLE members (
    id          INT             NOT NULL AUTO_INCREMENT,
    first_name  VARCHAR(100)    NOT NULL,
    last_name   VARCHAR(100)    NOT NULL,
    city        VARCHAR(100)    NOT NULL,
    phone       VARCHAR(20)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_members_phone UNIQUE (phone)
);
