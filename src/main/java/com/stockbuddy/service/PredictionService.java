// src/main/java/com/stockbuddy/service/PredictionService.java
package com.stockbuddy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);

    @Value("${model.api.url}")
    private String modelApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Call external model API to start a prediction.
     * POST {modelApiUrl}/api/predict
     */
    public ResponseEntity<Map> startPrediction(String userId, String symbol, int daysAhead) {
        String url = modelApiUrl + "/api/predict";
        log.info("Calling model API POST {} (symbol={}, daysAhead={})", url, symbol, daysAhead);
        Map<String, Object> body = Map.of(
                "userId",    userId,
                "symbol",    symbol,
                "daysAhead", daysAhead
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            return restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        } catch (Exception e) {
            log.error("Model API POST {} failed: {}", url, e.toString());
            throw e;
        }
    }

    /**
     * Call external model API to get prediction status.
     * GET {modelApiUrl}/api/predict/status/{taskId}
     */
    public ResponseEntity<Map> getPredictionStatus(String taskId) {
        String url = modelApiUrl + "/api/predict/status/" + taskId;
        log.debug("Calling model API GET {}", url);
        try {
            return restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
        } catch (Exception e) {
            log.error("Model API GET {} failed: {}", url, e.toString());
            throw e;
        }
    }

    /**
     * Call external model API to stop a prediction.
     * POST {modelApiUrl}/api/predict/stop/{taskId}
     */
    public ResponseEntity<Map> stopPrediction(String taskId) {
        String url = modelApiUrl + "/api/predict/stop/" + taskId;
        log.info("Calling model API POST {}", url);
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        } catch (Exception e) {
            log.error("Model API POST {} failed: {}", url, e.toString());
            throw e;
        }
    }
}
