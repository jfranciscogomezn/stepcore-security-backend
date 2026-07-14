package com.stepcore.security.exception;

public class AdminSelfDisableException extends RuntimeException {
    public AdminSelfDisableException() {
        super("An admin cannot disable their own account");
    }
}
