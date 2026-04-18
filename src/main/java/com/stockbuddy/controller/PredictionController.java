// src/main/java/com/stockbuddy/controller/PredictionController.java
package com.stockbuddy.controller;

import com.stockbuddy.dto.DeleteMultiplePredictionsRequest;
import com.stockbuddy.dto.PredictionRequest;
import com.stockbuddy.model.Prediction;
import com.stockbuddy.model.PredictionPoint;
import com.stockbuddy.model.Sentiment;
import com.stockbuddy.model.SentimentTotals;
import com.stockbuddy.model.User;
import com.stockbuddy.model.UserPreferences;
import com.stockbuddy.repository.PredictionRepository;
import com.stockbuddy.repository.UserPreferencesRepository;
import com.stockbuddy.repository.UserRepository;
import com.stockbuddy.service.EmailService;
import com.stockbuddy.service.PredictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.*;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private static final Logger log = LoggerFactory.getLogger(PredictionController.class);

    @Autowired private PredictionRepository predictionRepository;
    @Autowired private PredictionService predictionService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserPreferencesRepository preferencesRepository;
    @Autowired private EmailService emailService;

    // ───────────────────────────────────────────────
    // GET /api/predictions
    // ───────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> getAllPredictions(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        try {
            List<Prediction> predictions =
                    predictionRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return ResponseEntity.ok(predictions);
        } catch (Exception e) {
            log.error("getAllPredictions failed userId={}", userId, e);
            return serverError("Could not load predictions", e);
        }
    }

    // ───────────────────────────────────────────────
    // GET /api/predictions/{id}
    // ───────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getPrediction(@PathVariable String id, Authentication auth) {
        String userId = (String) auth.getPrincipal();
        try {
            Optional<Prediction> opt = predictionRepository.findById(id);

            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "Prediction not found"));
            }

            Prediction prediction = opt.get();

            if (!prediction.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("message", "Not authorized"));
            }

            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            log.error("getPrediction failed id={} userId={}", id, userId, e);
            return serverError("Could not load prediction", e);
        }
    }

    // ───────────────────────────────────────────────
    // POST /api/predictions  — start a new prediction
    // ───────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> startPrediction(@RequestBody PredictionRequest req,
                                              Authentication auth) {
        String userId = (String) auth.getPrincipal();

        if (req.getSymbol() == null || req.getSymbol().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Symbol is required"));
        }

        String symbol       = req.getSymbol().toUpperCase();
        int predictionDays  = (req.getDaysAhead() != null && req.getDaysAhead() > 0)
                              ? req.getDaysAhead() : 3;

        try {
            // Check for existing pending/running prediction for same symbol
            Optional<Prediction> existing = predictionRepository
                    .findByUserIdAndSymbolAndStatusIn(userId, symbol,
                            List.of("pending", "running"));

            if (existing.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "You already have a pending prediction for this symbol. " +
                                   "Please wait for it to complete or stop it first."));
            }

            // Call external model API
            ResponseEntity<Map> apiResponse =
                    predictionService.startPrediction(userId, symbol, predictionDays);

            if (!apiResponse.getStatusCode().is2xxSuccessful()) {
                log.warn("startPrediction: model non-success userId={} symbol={} status={}",
                        userId, symbol, apiResponse.getStatusCode());
                return forwardModelApiError(apiResponse,
                        "Model service returned an error (check MODEL_API_URL and model logs)");
            }

            Map<?, ?> data = apiResponse.getBody();
            if (data == null || data.get("taskId") == null) {
                log.error("startPrediction: model response missing taskId userId={} symbol={} body={}",
                        userId, symbol, data);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody(
                        "No taskId returned from prediction model",
                        data == null ? "Response body was null" : "Body keys: " + data.keySet()));
            }

            String taskId = data.get("taskId").toString();

            // Persist prediction record
            Prediction prediction = new Prediction();
            prediction.setUserId(userId);
            prediction.setSymbol(symbol);
            prediction.setDaysAhead(predictionDays);
            prediction.setStatus("pending");
            prediction.setTaskId(taskId);
            prediction.setPredictions(new ArrayList<>());
            prediction.setCreatedAt(new Date());
            prediction.setPredictionId(UUID.randomUUID().toString());

            try {
                predictionRepository.save(prediction);
            } catch (Exception dupEx) {
                // Retry with fresh UUID on duplicate key error
                prediction.setPredictionId(UUID.randomUUID().toString());
                predictionRepository.save(prediction);
            }

            return ResponseEntity.ok(prediction);

        } catch (Exception e) {
            return handleModelOrServerFailure("startPrediction", userId, symbol, e);
        }
    }

    // ───────────────────────────────────────────────
    // GET /api/predictions/status/{taskId}
    // ───────────────────────────────────────────────
    @GetMapping("/status/{taskId}")
    public ResponseEntity<?> getPredictionStatus(@PathVariable String taskId,
                                                  Authentication auth) {
        String userId = (String) auth.getPrincipal();
        try {
            Optional<Prediction> opt = predictionRepository.findByTaskId(taskId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "Prediction not found"));
            }

            Prediction prediction = opt.get();

            if (!prediction.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("message", "Not authorized"));
            }

            // Call external model API for status
            ResponseEntity<Map> apiResponse = predictionService.getPredictionStatus(taskId);

            if (!apiResponse.getStatusCode().is2xxSuccessful()) {
                log.warn("getPredictionStatus: model non-success taskId={} status={}", taskId, apiResponse.getStatusCode());
                return forwardModelApiError(apiResponse, "Model status request failed");
            }

            if (apiResponse.getBody() == null) {
                log.error("getPredictionStatus: model returned null body taskId={}", taskId);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody(
                        "Model returned an empty status response",
                        "taskId=" + taskId));
            }

            Map<String, Object> data = new LinkedHashMap<>(apiResponse.getBody());

            String apiStatus = data.get("status") != null ? data.get("status").toString() : "";

            // Update local prediction status
            if ("completed".equals(apiStatus) && !"completed".equals(prediction.getStatus())) {
                prediction.setStatus("completed");

                @SuppressWarnings("unchecked")
                Map<?, ?> result = (Map<?, ?>) data.get("result");

                if (result != null && result.get("predictions") != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<?, ?>> rawPredictions = (List<Map<?, ?>>) result.get("predictions");

                    List<PredictionPoint> points = new ArrayList<>();
                    for (Map<?, ?> p : rawPredictions) {
                        PredictionPoint point = new PredictionPoint();
                        point.setDate(p.get("date") != null ? p.get("date").toString() : "");
                        point.setPrice(p.get("price") != null
                                ? Double.parseDouble(p.get("price").toString()) : 0);
                        points.add(point);
                    }
                    prediction.setPredictions(points);

                    // Parse sentiment if present
                    @SuppressWarnings("unchecked")
                    Map<?, ?> sentimentRaw = (Map<?, ?>) result.get("sentiment");
                    if (sentimentRaw != null) {
                        Sentiment sentiment = new Sentiment();
                        sentiment.setSummary(sentimentRaw.get("summary") != null
                                ? sentimentRaw.get("summary").toString() : "");

                        @SuppressWarnings("unchecked")
                        Map<?, ?> totalsRaw = (Map<?, ?>) sentimentRaw.get("totals");
                        if (totalsRaw != null) {
                            SentimentTotals totals = new SentimentTotals();
                            totals.setPositive(toInt(totalsRaw.get("positive")));
                            totals.setNegative(toInt(totalsRaw.get("negative")));
                            totals.setNeutral(toInt(totalsRaw.get("neutral")));
                            sentiment.setTotals(totals);
                        }
                        prediction.setSentiment(sentiment);
                    }

                    predictionRepository.save(prediction);
                    notifyPredictionCompletedByEmail(prediction);
                } else {
                    return ResponseEntity.status(500).body(Map.of(
                            "message", "Invalid prediction data format"));
                }

            } else if ("failed".equals(apiStatus) && !"failed".equals(prediction.getStatus())) {
                prediction.setStatus("failed");
                prediction.setError(data.get("error") != null
                        ? data.get("error").toString() : "Unknown error occurred");
                predictionRepository.save(prediction);

            } else if (!apiStatus.equals(prediction.getStatus())) {
                prediction.setStatus(apiStatus);
                predictionRepository.save(prediction);
            }

            data.put("id", prediction.getId());
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            log.error("getPredictionStatus failed taskId={} userId={}", taskId, userId, e);
            return handleModelOrServerFailure("getPredictionStatus", userId, taskId, e);
        }
    }

    // ───────────────────────────────────────────────
    // POST /api/predictions/stop/{taskId}
    // ───────────────────────────────────────────────
    @PostMapping("/stop/{taskId}")
    public ResponseEntity<?> stopPrediction(@PathVariable String taskId,
                                             Authentication auth) {
        String userId = (String) auth.getPrincipal();
        try {
            Optional<Prediction> opt = predictionRepository.findByTaskId(taskId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "Prediction not found"));
            }

            Prediction prediction = opt.get();

            if (!prediction.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("message", "Not authorized"));
            }

            ResponseEntity<Map> apiResponse = predictionService.stopPrediction(taskId);

            if (!apiResponse.getStatusCode().is2xxSuccessful()) {
                return forwardModelApiError(apiResponse, "Model stop request failed");
            }

            prediction.setStatus("stopped");
            predictionRepository.save(prediction);

            Map<String, Object> response = new LinkedHashMap<>(apiResponse.getBody());
            response.put("id", prediction.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("stopPrediction failed taskId={} userId={}", taskId, userId, e);
            return handleModelOrServerFailure("stopPrediction", userId, taskId, e);
        }
    }

    // ───────────────────────────────────────────────
    // DELETE /api/predictions/{id}
    // ───────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePrediction(@PathVariable String id,
                                               Authentication auth) {
        String userId = (String) auth.getPrincipal();
        try {
            Optional<Prediction> opt = predictionRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "Prediction not found"));
            }

            Prediction prediction = opt.get();

            if (!prediction.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("message", "Not authorized"));
            }

            // Stop active prediction before deleting
            if ("pending".equals(prediction.getStatus()) || "running".equals(prediction.getStatus())) {
                try {
                    predictionService.stopPrediction(prediction.getTaskId());
                } catch (Exception ignored) {}
            }

            predictionRepository.deleteById(id);

            return ResponseEntity.ok(Map.of("message", "Prediction deleted successfully"));

        } catch (Exception e) {
            log.error("deletePrediction failed id={} userId={}", id, userId, e);
            return serverError("Could not delete prediction", e);
        }
    }

    // ───────────────────────────────────────────────
    // POST /api/predictions/delete-multiple
    // ───────────────────────────────────────────────
    @PostMapping("/delete-multiple")
    public ResponseEntity<?> deleteMultiple(@RequestBody DeleteMultiplePredictionsRequest req,
                                             Authentication auth) {
        String userId = (String) auth.getPrincipal();

        if (req.getIds() == null || req.getIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No prediction IDs provided"));
        }

        try {
            List<Prediction> predictions = predictionRepository
                    .findByIdInAndUserId(req.getIds(), userId);

            if (predictions.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "No predictions found"));
            }

            // Stop any active predictions
            for (Prediction p : predictions) {
                if ("pending".equals(p.getStatus()) || "running".equals(p.getStatus())) {
                    try {
                        predictionService.stopPrediction(p.getTaskId());
                    } catch (Exception ignored) {}
                }
            }

            predictionRepository.deleteByUserIdAndIdIn(userId, req.getIds());

            return ResponseEntity.ok(Map.of(
                    "message", "Predictions deleted successfully",
                    "count", predictions.size()));

        } catch (Exception e) {
            log.error("deleteMultiple failed userId={}", userId, e);
            return serverError("Could not delete predictions", e);
        }
    }

    /** When the Python model returns a non-2xx response, normalize JSON for mobile clients. */
    private static ResponseEntity<Map<String, Object>> forwardModelApiError(ResponseEntity<Map> apiResponse, String hint) {
        int code = apiResponse.getStatusCode().value();
        Map<?, ?> err = apiResponse.getBody();
        String detail = formatModelErrorPayload(err, code);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", hint + " (HTTP " + code + ")");
        body.put("detail", detail);
        return ResponseEntity.status(apiResponse.getStatusCode()).body(body);
    }

    private static String formatModelErrorPayload(Map<?, ?> err, int httpCode) {
        if (err == null || err.isEmpty()) {
            return "HTTP " + httpCode + " with empty body from model API";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP ").append(httpCode);
        appendField(sb, "error", err.get("error"));
        appendField(sb, "message", err.get("message"));
        appendField(sb, "details", err.get("details"));
        if (sb.length() > 1200) {
            return sb.substring(0, 1200) + "...";
        }
        return sb.toString();
    }

    private static void appendField(StringBuilder sb, String key, Object val) {
        if (val == null) return;
        String s = val.toString();
        if (s.isBlank()) return;
        sb.append(" | ").append(key).append(": ").append(s);
    }

    private ResponseEntity<Map<String, Object>> handleModelOrServerFailure(
            String operation, String userId, String symbolOrTask, Exception e) {
        if (e instanceof HttpStatusCodeException hs) {
            log.error("{} failed userId={} {}: HTTP {} bodySnippet={}",
                    operation, userId, symbolOrTask, hs.getStatusCode(), abbreviate(hs.getResponseBodyAsString()), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody(
                    operation + " failed: model HTTP " + hs.getStatusCode().value(),
                    extractHttpExceptionDetail(hs)));
        }
        if (e instanceof ResourceAccessException) {
            log.error("{} failed userId={} {}: cannot reach model API", operation, userId, symbolOrTask, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody(
                    operation + " failed: cannot reach model API (timeout, DNS, or connection refused)",
                    rootMessage(e)));
        }
        if (e instanceof RestClientException) {
            log.error("{} failed userId={} {}: RestClient error", operation, userId, symbolOrTask, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody(
                    operation + " failed: model request error",
                    rootMessage(e)));
        }
        log.error("{} failed userId={} {}", operation, userId, symbolOrTask, e);
        return serverError(operation + " failed: unexpected server error", e);
    }

    private static ResponseEntity<Map<String, Object>> serverError(String message, Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(message, rootMessage(e)));
    }

    private static Map<String, Object> errorBody(String message, String detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("message", message);
        if (detail != null && !detail.isBlank()) {
            m.put("detail", detail);
        }
        return m;
    }

    private static String extractHttpExceptionDetail(HttpStatusCodeException e) {
        String body = e.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return e.getStatusCode() + " " + e.getStatusText();
        }
        if (body.length() > 800) {
            return e.getStatusCode() + " " + e.getStatusText() + ": " + body.substring(0, 800) + "...";
        }
        return e.getStatusCode() + " " + e.getStatusText() + ": " + body;
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String m = t.getMessage();
        return m != null && !m.isBlank() ? m : t.getClass().getSimpleName();
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > 200 ? t.substring(0, 200) + "..." : t;
    }

    // ─── Helper ───────────────────────────────────
    private int toInt(Object val) {
        if (val == null) return 0;
        try { return Integer.parseInt(val.toString()); }
        catch (NumberFormatException e) { return 0; }
    }

    private void notifyPredictionCompletedByEmail(Prediction prediction) {
        try {
            String uid = prediction.getUserId();
            UserPreferences prefs = preferencesRepository.findByUserId(uid).orElse(null);
            if (prefs == null || !prefs.isEmailNotifications()) {
                return;
            }
            Optional<User> userOpt = userRepository.findById(uid);
            if (userOpt.isEmpty()) {
                return;
            }
            String email = userOpt.get().getEmail();
            if (email == null || email.isBlank()) {
                return;
            }
            emailService.sendPredictionCompletedEmail(email.trim(), prediction);
        } catch (Exception ignored) {
        }
    }
}
