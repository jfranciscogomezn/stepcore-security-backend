package com.stepcore.security.exception;

public class RoleInUseException extends RuntimeException {
    public RoleInUseException(final Long roleId) {
        super("Role id=" + roleId + " cannot be deleted because it is assigned to one or more users");
    }
}
