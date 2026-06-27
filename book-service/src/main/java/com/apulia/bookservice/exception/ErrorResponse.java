package com.apulia.bookservice.exception;

public class ErrorResponse {

    private String timestamp;
    private int status;
    private String error;
    private String message;
    private Long retryAfterSeconds;

    public ErrorResponse() {}

    public ErrorResponse(String timestamp, int status, String error, String message) {
        this(timestamp, status, error, message, null);
    }

    public ErrorResponse(String timestamp, int status, String error, String message, Long retryAfterSeconds) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public void setRetryAfterSeconds(Long retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
