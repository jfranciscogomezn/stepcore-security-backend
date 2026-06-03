package com.stepcore.security.i18n;

import com.stepcore.security.exception.DuplicateEmailException;
import com.stepcore.security.exception.InvalidMenuNodeAssignmentException;
import com.stepcore.security.exception.InvalidPasswordException;
import com.stepcore.security.exception.MenuNodeInUseException;
import com.stepcore.security.exception.MenuNodeNotFoundException;
import com.stepcore.security.exception.MenuNodeValidationException;
import com.stepcore.security.exception.RoleInUseException;
import com.stepcore.security.exception.RoleNotFoundException;
import com.stepcore.security.exception.TenantNotFoundException;
import com.stepcore.security.exception.TenantSlugAlreadyExistsException;
import com.stepcore.security.exception.TenantSuspendedException;
import com.stepcore.security.exception.UserHasAssociatedRecordsException;
import com.stepcore.security.exception.UserLimitReachedException;
import com.stepcore.security.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ApiMessageService {

    private static final Pattern USER_ID_PATTERN = Pattern.compile("User not found with id: (\\d+)");
    private static final Pattern USER_EMAIL_PATTERN = Pattern.compile("User not found with email: (.+)");
    private static final Pattern ROLE_ID_PATTERN = Pattern.compile("Role not found with id: (\\d+)");
    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("Tenant not found: (\\d+)");
    private static final Pattern MENU_NODE_ID_PATTERN = Pattern.compile("Menu node not found: (\\d+)");
    private static final Pattern USER_LIMIT_PATTERN = Pattern.compile("max (\\d+)");

    private final MessageSource messageSource;

    public String get(final String code, final Object... args) {
        final Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, code, locale);
    }

    public String resolve(final Throwable throwable, final String fallback) {
        if (throwable instanceof UserLimitReachedException) {
            final int maxUsers = extractMaxUsers(throwable.getMessage());
            return get("error.userLimitReached", maxUsers);
        }
        if (throwable instanceof TenantSuspendedException ex) {
            return get("error.tenantSuspended", ex.getMessage().replace("Tenant is not active: ", ""));
        }
        if (throwable instanceof UserNotFoundException) {
            return resolveUserNotFound(throwable.getMessage());
        }
        if (throwable instanceof RoleNotFoundException) {
            return get("error.roleNotFound", extractGroup(ROLE_ID_PATTERN, throwable.getMessage()));
        }
        if (throwable instanceof TenantNotFoundException) {
            return get("error.tenantNotFound", extractGroup(TENANT_ID_PATTERN, throwable.getMessage()));
        }
        if (throwable instanceof MenuNodeNotFoundException) {
            return get("error.menuNodeNotFound", extractGroup(MENU_NODE_ID_PATTERN, throwable.getMessage()));
        }
        if (throwable instanceof DuplicateEmailException ex) {
            if (ex.getMessage().startsWith("Email is already registered")) {
                return get("error.emailAlreadyRegistered", ex.getMessage().replace("Email is already registered: ", ""));
            }
            return get("error.roleNameAlreadyExists", ex.getMessage().replace("Role name already exists: ", ""));
        }
        if (throwable instanceof RoleInUseException) {
            return get("error.roleInUse", throwable.getMessage().replace("Role id=", "").replace(" cannot be deleted because it is assigned to one or more users", ""));
        }
        if (throwable instanceof UserHasAssociatedRecordsException) {
            return get("error.userHasAssociatedRecords", throwable.getMessage().replace("User id=", "").replace(" cannot be deleted because it has associated records", ""));
        }
        if (throwable instanceof TenantSlugAlreadyExistsException ex) {
            return get("error.tenantSlugExists", ex.getMessage().replace("Tenant slug already exists: ", ""));
        }
        if (throwable instanceof InvalidPasswordException) {
            return get("error.currentPasswordIncorrect");
        }
        if (throwable instanceof InvalidMenuNodeAssignmentException ex) {
            return get("error.invalidMenuAssignment", ex.getMessage());
        }
        if (throwable instanceof MenuNodeValidationException ex) {
            return get("error.menuNodeValidation", ex.getMessage());
        }
        if (throwable instanceof MenuNodeInUseException ex) {
            return get("error.menuNodeInUse", ex.getMessage());
        }
        if (throwable instanceof AuthenticationException) {
            if (fallback != null && fallback.toLowerCase().contains("invalid tenant or credentials")) {
                return get("error.invalidCredentials");
            }
            return get("error.unauthorized");
        }
        return fallback != null ? fallback : get("error.unexpected");
    }

    public String resolveKey(final String code, final Object... args) {
        return get(code, args);
    }

    private String resolveUserNotFound(final String message) {
        final Matcher byId = USER_ID_PATTERN.matcher(message);
        if (byId.matches()) {
            return get("error.userNotFoundById", byId.group(1));
        }
        final Matcher byEmail = USER_EMAIL_PATTERN.matcher(message);
        if (byEmail.matches()) {
            return get("error.userNotFoundByEmail", byEmail.group(1));
        }
        return get("error.userNotFound");
    }

    private int extractMaxUsers(final String message) {
        final Matcher matcher = USER_LIMIT_PATTERN.matcher(message);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    private String extractGroup(final Pattern pattern, final String message) {
        final Matcher matcher = pattern.matcher(message);
        return matcher.matches() ? matcher.group(1) : message;
    }
}
