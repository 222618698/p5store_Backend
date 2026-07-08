package com.p5store.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidDiscountException extends RuntimeException {
    public InvalidDiscountException(String code) {
        super("Discount code '%s' is invalid, expired, or does not meet minimum order requirements.".formatted(code));
    }
}
