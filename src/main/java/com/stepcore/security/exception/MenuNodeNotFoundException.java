package com.stepcore.security.exception;

public class MenuNodeNotFoundException extends RuntimeException {

    public MenuNodeNotFoundException(final Long id) {
        super("Menu node not found: " + id);
    }
}
