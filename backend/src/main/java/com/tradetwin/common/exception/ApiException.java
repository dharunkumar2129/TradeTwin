package com.tradetwin.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final List<String> details;

    public ApiException(String message) {
        this(message, HttpStatus.BAD_REQUEST, null);
    }

    public ApiException(String message, HttpStatus status) {
        this(message, status, null);
    }

    public ApiException(String message, HttpStatus status, List<String> details) {
        super(message);
        this.status = status;
        this.details = details != null ? List.copyOf(details) : Collections.emptyList();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<String> getDetails() {
        return details;
    }
}
