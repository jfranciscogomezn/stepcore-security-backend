package com.stepcore.security.exception;

public class AdminSelfDeleteException extends RuntimeException {
    public AdminSelfDeleteException() {
        super("An admin cannot delete their own account");
    }
}
