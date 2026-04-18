// src/main/java/com/stockbuddy/controller/CaptchaController.java

package com.stockbuddy.controller;

import com.stockbuddy.dto.VerifyCaptchaRequest;
import com.stockbuddy.service.CaptchaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;

    // ───────────────────────────────────────────────
    // GET /api/captcha
    // Mirrors: router.get('/captcha', ...) in captcha.js
    // ───────────────────────────────────────────────
    @GetMapping("/captcha")
    public ResponseEntity<?> getCaptcha(HttpSession session) {
        String captchaText = captchaService.generate(session);

        // Build spaced text representation — mirrors Node.js:
        // textRepresentation: `Verification Code: "${captchaText.split('').join(' ')}"`
        String spaced = String.join(" ", captchaText.split(""));
        String textRepresentation = "Verification Code: \"" + spaced + "\"";

        return ResponseEntity.ok(Map.of(
                "captchaText",        captchaText,
                "textRepresentation", textRepresentation,
                "timestamp",          System.currentTimeMillis()
        ));
    }

    // ───────────────────────────────────────────────
    // POST /api/verify-captcha
    // Mirrors: router.post('/verify-captcha', ...) in captcha.js
    // ───────────────────────────────────────────────
    @PostMapping("/verify-captcha")
    public ResponseEntity<?> verifyCaptcha(@RequestBody VerifyCaptchaRequest req,
                                           HttpSession session) {

        Object stored = session.getAttribute("captcha");

        if (stored == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "CAPTCHA session expired"));
        }

        String expected = stored.toString();
        String userInput = req.getUserInput();

        // Remove captcha from session regardless of outcome
        session.removeAttribute("captcha");

        boolean isValid = expected.equals(userInput); // case-sensitive

        if (isValid) {
            return ResponseEntity.ok(Map.of("success", true));
        } else {
            return ResponseEntity.ok(Map.of(
                    "success",       false,
                    "message",       "Invalid CAPTCHA. Please ensure you enter the exact characters shown (case-sensitive).",
                    "expectedValue", expected,   // for debugging; remove in production
                    "userValue",     userInput    // for debugging; remove in production
            ));
        }
    }
}
