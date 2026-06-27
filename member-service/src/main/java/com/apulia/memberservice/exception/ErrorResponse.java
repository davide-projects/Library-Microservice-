package com.apulia.memberservice.exception;

public class ErrorResponse {

    private String timestamp;
    private int status;
    private String error;
    private String message;
    private Long retryAfterSeconds;

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

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
