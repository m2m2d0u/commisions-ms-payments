package com.payment.commission.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Service for internationalized messages
 * Supports French (default) and English
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Get message by key with current locale
     */
    public String getMessage(String key) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(key, null, locale);
        } catch (Exception e) {
            log.warn("Message not found for key: {}", key);
            return key;
        }
    }

    /**
     * Get message by key with parameters
     */
    public String getMessage(String key, Object... args) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(key, args, locale);
        } catch (Exception e) {
            log.warn("Message not found for key: {}", key);
            return key;
        }
    }

    /**
     * Get message or return default if not found
     */
    public String getMessageOrDefault(String key, String defaultMessage) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(key, null, defaultMessage, locale);
        } catch (Exception e) {
            return defaultMessage;
        }
    }
}
