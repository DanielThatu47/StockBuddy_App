// src/main/java/com/stockbuddy/controller/PreferencesController.java
package com.stockbuddy.controller;

import com.stockbuddy.model.UserPreferences;
import com.stockbuddy.repository.UserPreferencesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

/**
 * User Preferences
 *
 * GET  /api/preferences         — get all preferences
 * PUT  /api/preferences         — update preferences
 * POST /api/preferences/push-token  — register Expo push notification token
 */
@RestController
@RequestMapping("/api/preferences")
public class PreferencesController {

    @Autowired
    private UserPreferencesRepository preferencesRepository;

    // ── GET /api/preferences ──────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> getPreferences(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        try {
            UserPreferences prefs = preferencesRepository.findByUserId(userId)
                    .orElseGet(() -> createDefaultPreferences(userId));
            return ResponseEntity.ok(Map.of("success", true, "preferences", prefs));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── PUT /api/preferences ──────────────────────────────────────
    @PutMapping
    public ResponseEntity<?> updatePreferences(@RequestBody Map<String, Object> body,
                                               Authentication auth) {
        String userId = (String) auth.getPrincipal();
        try {
            UserPreferences prefs = preferencesRepository.findByUserId(userId)
                    .orElseGet(() -> createDefaultPreferences(userId));

            // Apply each field if present in request body
            if (body.containsKey("darkMode"))           prefs.setDarkMode(toBool(body.get("darkMode")));
            if (body.containsKey("pushNotifications"))  prefs.setPushNotifications(toBool(body.get("pushNotifications")));
            if (body.containsKey("emailNotifications")) prefs.setEmailNotifications(toBool(body.get("emailNotifications")));
            if (body.containsKey("marketAlerts"))       prefs.setMarketAlerts(toBool(body.get("marketAlerts")));
            if (body.containsKey("portfolioUpdates"))   prefs.setPortfolioUpdates(toBool(body.get("portfolioUpdates")));
            if (body.containsKey("newsUpdates"))        prefs.setNewsUpdates(toBool(body.get("newsUpdates")));
            if (body.containsKey("tradingSignals"))     prefs.setTradingSignals(toBool(body.get("tradingSignals")));
            if (body.containsKey("systemUpdates"))      prefs.setSystemUpdates(toBool(body.get("systemUpdates")));
            if (body.containsKey("marketingEmails"))    prefs.setMarketingEmails(toBool(body.get("marketingEmails")));
            if (body.containsKey("language"))           prefs.setLanguage(body.get("language").toString());
            if (body.containsKey("currency"))           prefs.setCurrency(body.get("currency").toString());
            if (body.containsKey("timezone"))           prefs.setTimezone(body.get("timezone").toString());
            if (body.containsKey("dataSharing"))        prefs.setDataSharing(toBool(body.get("dataSharing")));
            if (body.containsKey("activityTracking"))   prefs.setActivityTracking(toBool(body.get("activityTracking")));

            prefs.setUpdatedAt(new Date());
            preferencesRepository.save(prefs);

            return ResponseEntity.ok(Map.of("success", true, "preferences", prefs));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── POST /api/preferences/push-token ─────────────────────────
    @PostMapping("/push-token")
    public ResponseEntity<?> registerPushToken(@RequestBody Map<String, Object> body,
                                               Authentication auth) {
        String userId = (String) auth.getPrincipal();
        try {
            String token = body.get("token") != null ? body.get("token").toString() : null;
            if (token == null || token.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Token is required"));
            }

            UserPreferences prefs = preferencesRepository.findByUserId(userId)
                    .orElseGet(() -> createDefaultPreferences(userId));
            prefs.setExpoPushToken(token);
            prefs.setUpdatedAt(new Date());
            preferencesRepository.save(prefs);

            return ResponseEntity.ok(Map.of("success", true, "message", "Push token registered"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────
    private UserPreferences createDefaultPreferences(String userId) {
        UserPreferences prefs = new UserPreferences();
        prefs.setUserId(userId);
        return preferencesRepository.save(prefs);
    }

    private boolean toBool(Object val) {
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(val.toString());
    }
}
