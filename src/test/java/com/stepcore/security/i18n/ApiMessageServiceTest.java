package com.stepcore.security.i18n;

import com.stepcore.security.exception.UserLimitReachedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ApiMessageServiceTest {

    private ApiMessageService apiMessageService;

    @BeforeEach
    void setUp() {
        final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        apiMessageService = new ApiMessageService(messageSource);
    }

    @Test
    void shouldReturnSpanishAccessDeniedByDefault() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es-CO"));
        assertThat(apiMessageService.resolveKey("error.accessDenied"))
                .isEqualTo("No tiene permiso para realizar esta acción");
    }

    @Test
    void shouldReturnEnglishWhenEnUsRequested() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("en-US"));
        assertThat(apiMessageService.resolveKey("error.accessDenied"))
                .isEqualTo("Access denied");
    }

    @Test
    void shouldLocalizeUserLimitReachedWithCodePrefix() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es-CO"));
        final String message = apiMessageService.resolve(new UserLimitReachedException(50), null);
        assertThat(message).contains("USER_LIMIT_REACHED");
        assertThat(message).contains("50");
    }
}
