package com.gmm.devengos.exception;

public class UserHasAssociatedRecordsException extends RuntimeException {
    public UserHasAssociatedRecordsException(final Long userId) {
        super("User id=" + userId + " cannot be deleted because it has associated records");
    }
}
