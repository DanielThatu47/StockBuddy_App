// src/main/java/com/stockbuddy/controller/WatchlistController.java
package com.stockbuddy.controller;

import com.stockbuddy.model.WatchlistEntry;
import com.stockbuddy.repository.WatchlistEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

	@Autowired
	private WatchlistEntryRepository watchlistRepository;

	@GetMapping
	public ResponseEntity<?> list(Authentication auth) {
		String userId = (String) auth.getPrincipal();
		List<WatchlistEntry> items = watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
		return ResponseEntity.ok(items);
	}

	@PostMapping
	public ResponseEntity<?> add(@RequestBody Map<String, Object> body, Authentication auth) {
		String userId = (String) auth.getPrincipal();
		String symbol = str(body, "symbol");
		if (symbol == null || symbol.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "symbol is required"));
		}
		symbol = symbol.trim();
		if (watchlistRepository.findByUserIdAndSymbol(userId, symbol).isPresent()) {
			return ResponseEntity.ok(Map.of("success", true, "message", "Already in watchlist", "duplicate", true));
		}
		WatchlistEntry e = new WatchlistEntry();
		e.setUserId(userId);
		e.setSymbol(symbol);
		e.setDisplaySymbol(str(body, "displaySymbol", symbol));
		e.setDescription(str(body, "description", ""));
		e.setExchange(str(body, "exchange", ""));
		e.setCreatedAt(new Date());
		watchlistRepository.save(e);
		return ResponseEntity.ok(Map.of("success", true, "message", "Added to watchlist", "entry", e));
	}

	@DeleteMapping
	public ResponseEntity<?> remove(@RequestParam("symbol") String symbol, Authentication auth) {
		String userId = (String) auth.getPrincipal();
		if (symbol == null || symbol.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "symbol query parameter is required"));
		}
		Optional<WatchlistEntry> opt = watchlistRepository.findByUserIdAndSymbol(userId, symbol.trim());
		if (opt.isEmpty()) {
			return ResponseEntity.status(404).body(Map.of("success", false, "message", "Not in watchlist"));
		}
		watchlistRepository.deleteByUserIdAndSymbol(userId, symbol.trim());
		return ResponseEntity.ok(Map.of("success", true, "message", "Removed from watchlist"));
	}

	private static String str(Map<String, Object> body, String key) {
		Object v = body.get(key);
		return v != null ? v.toString().trim() : null;
	}

	private static String str(Map<String, Object> body, String key, String def) {
		String s = str(body, key);
		return s != null && !s.isEmpty() ? s : def;
	}
}
