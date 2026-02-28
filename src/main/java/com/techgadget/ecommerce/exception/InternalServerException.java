package com.techgadget.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class InternalServerException extends BaseException {
    public InternalServerException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
