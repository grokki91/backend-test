package com.example.backendtest;

import org.springframework.http.HttpStatus;

/** Any expected error the API answers with: 401, 403, 404, 409. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
