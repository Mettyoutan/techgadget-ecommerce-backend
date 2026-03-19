package com.techgadget.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class InternalServerException extends BaseException {
    static final String MESSAGE = "An unexpected error occurred. Please try again later.";

    public InternalServerException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, MESSAGE);
    }
}
