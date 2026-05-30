package com.stepcore.security.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(final String email) {
        super("Email is already registered: " + email);
    }
}
