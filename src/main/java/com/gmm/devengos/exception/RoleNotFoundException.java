package com.gmm.devengos.exception;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(final Long id) {
        super("Role not found with id: " + id);
    }
}
