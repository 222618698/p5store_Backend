package com.p5store.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String variantId, int available, int requested) {
        super("Variant %s has only %d units but %d were requested".formatted(variantId, available, requested));
    }
}
