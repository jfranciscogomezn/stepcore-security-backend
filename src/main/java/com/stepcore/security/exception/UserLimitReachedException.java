package com.stepcore.security.exception;

/**
 * Raised when creating a user would exceed the tenant's {@code max_users} plan cap.
 * Carries a stable error code so the frontend can show an upgrade hint.
 */
public class UserLimitReachedException extends RuntimeException {

    public static final String CODE = "USER_LIMIT_REACHED";

    public UserLimitReachedException(final int maxUsers) {
        super(CODE + ": tenant user limit reached (max " + maxUsers + ")");
    }
}
