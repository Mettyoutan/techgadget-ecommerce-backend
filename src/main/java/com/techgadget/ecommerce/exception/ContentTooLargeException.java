package com.techgadget.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class ContentTooLargeException extends BaseException {
    public ContentTooLargeException(String message) {
        super(HttpStatus.CONTENT_TOO_LARGE, message);
    }
}
