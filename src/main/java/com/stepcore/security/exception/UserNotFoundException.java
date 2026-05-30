package com.stepcore.security.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(final Long id) {
        super("User not found with id: " + id);
    }
    public UserNotFoundException(final String email) {
        super("User not found with email: " + email);
    }
}
