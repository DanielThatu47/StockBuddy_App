// src/main/java/com/stockbuddy/service/CaptchaService.java
package com.stockbuddy.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class CaptchaService {

    private static final String CHAR_PRESET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private static final int CAPTCHA_LENGTH = 6;
    private static final String SESSION_KEY = "captcha";

    private final SecureRandom random = new SecureRandom();

    /**
     * Generate a random captcha text, store it in the HTTP session, and return it.
     * Mirrors Node.js svg-captcha.create({ size: 6, charPreset: '...' })
     */
    public String generate(HttpSession session) {
        StringBuilder sb = new StringBuilder(CAPTCHA_LENGTH);
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            sb.append(CHAR_PRESET.charAt(random.nextInt(CHAR_PRESET.length())));
        }
        String captchaText = sb.toString();
        session.setAttribute(SESSION_KEY, captchaText);
        return captchaText;
    }

    /**
     * Verify user input against the session captcha (case-sensitive).
     * Clears the captcha from session after checking regardless of result.
     *
     * @return true if valid, false if not or session expired
     */
    public boolean verify(HttpSession session, String userInput) {
        Object stored = session.getAttribute(SESSION_KEY);
        if (stored == null) return false;

        String expected = stored.toString();
        session.removeAttribute(SESSION_KEY);
        return expected.equals(userInput); // case-sensitive
    }
}
