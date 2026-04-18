// src/main/java/com/stockbuddy/controller/SessionController.java
package com.stockbuddy.controller;

import com.stockbuddy.model.UserPreferences;
import com.stockbuddy.model.UserSession;
import com.stockbuddy.repository.SessionRepository;
import com.stockbuddy.repository.UserPreferencesRepository;
import com.stockbuddy.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Device management: primary device is the first app install anchor (deviceKey) or platform+model
 * snapshot. Same physical line (matching model after reinstall) regains primary. Only that device
 * may revoke other sessions or use “revoke all others”.
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

	@Autowired
	private SessionRepository sessionRepository;
	@Autowired
	private UserPreferencesRepository preferencesRepository;
	@Autowired
	private JwtUtil jwtUtil;

	@PostMapping("/register")
	public ResponseEntity<?> registerSession(@RequestBody Map<String, Object> body,
			Authentication auth,
			HttpServletRequest request) {
		String userId = (String) auth.getPrincipal();
		try {
			String platform = getStr(body, "platform", "unknown");
			String deviceModel = getStr(body, "deviceModel", "Unknown");
			String deviceKeyRaw = body.get("deviceKey") != null ? body.get("deviceKey").toString().trim() : "";

			UserPreferences prefs = preferencesRepository.findByUserId(userId).orElseGet(() -> {
				UserPreferences p = new UserPreferences();
				p.setUserId(userId);
				return p;
			});

			ensurePrimaryAnchor(prefs, deviceKeyRaw, platform, deviceModel);

			boolean isPrimaryDevice = matchesPrimaryDevice(prefs, deviceKeyRaw, platform, deviceModel);

			UserSession session = new UserSession();
			session.setUserId(userId);
			session.setDeviceName(getStr(body, "deviceName", "Unknown Device"));
			session.setDeviceModel(deviceModel);
			session.setPlatform(platform);
			session.setOsVersion(getStr(body, "osVersion", ""));
			session.setAppVersion(getStr(body, "appVersion", "1.0.0"));
			session.setDeviceKey(deviceKeyRaw);
			session.setIpAddress(getClientIp(request));
			session.setActive(true);
			session.setCurrent(true);
			session.setLoginTime(new Date());
			session.setLastActive(new Date());

			if (isPrimaryDevice) {
				List<UserSession> allForUser = sessionRepository.findByUserId(userId);
				for (UserSession s : allForUser) {
					if (s.isPrimary()) {
						s.setPrimary(false);
						sessionRepository.save(s);
					}
				}
				session.setPrimary(true);
			} else {
				session.setPrimary(false);
			}

			List<UserSession> existing = sessionRepository.findByUserIdAndIsActiveTrueOrderByLastActiveDesc(userId);
			for (UserSession s : existing) {
				if (s.isCurrent()) {
					s.setCurrent(false);
					sessionRepository.save(s);
				}
			}

			sessionRepository.save(session);

			prefs.setUpdatedAt(new Date());
			preferencesRepository.save(prefs);

			String token = jwtUtil.generateToken(userId, session.getId());
			return ResponseEntity.ok(Map.of(
					"success", true,
					"sessionId", session.getId(),
					"token", token,
					"isPrimaryDevice", isPrimaryDevice,
					"message", "Session registered"));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	@PostMapping("/logout-current")
	public ResponseEntity<?> logoutCurrentSession(Authentication auth, HttpServletRequest request) {
		String userId = (String) auth.getPrincipal();
		try {
			Optional<UserSession> caller = resolveCallerSession(userId, request);
			if (caller.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of(
						"success", false,
						"message", "No matching device session. Try signing in again."));
			}
			UserSession s = caller.get();
			s.setActive(false);
			s.setRevokedAt(new Date());
			s.setCurrent(false);
			sessionRepository.save(s);
			return ResponseEntity.ok(Map.of("success", true, "message", "Session ended on this device"));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	@GetMapping
	public ResponseEntity<?> getActiveSessions(Authentication auth) {
		String userId = (String) auth.getPrincipal();
		try {
			List<UserSession> sessions =
					sessionRepository.findByUserIdAndIsActiveTrueOrderByLastActiveDesc(userId);
			return ResponseEntity.ok(sessions);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
		}
	}

	@GetMapping("/activity")
	public ResponseEntity<?> getActivityLog(Authentication auth) {
		String userId = (String) auth.getPrincipal();
		try {
			List<UserSession> all = sessionRepository.findByUserId(userId);
			all.sort((a, b) -> b.getLoginTime().compareTo(a.getLoginTime()));
			return ResponseEntity.ok(all);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> revokeSession(@PathVariable String id,
			Authentication auth,
			HttpServletRequest request) {
		String userId = (String) auth.getPrincipal();
		try {
			Optional<UserSession> caller = resolveCallerSession(userId, request);
			if (caller.isEmpty()) {
				return ResponseEntity.status(403).body(Map.of(
						"message",
						"Register this device after sign-in, then you can manage sessions."));
			}
			if (!caller.get().isPrimary()) {
				return ResponseEntity.status(403).body(Map.of(
						"message",
						"Only the primary device can revoke other sessions. Use that device, or sign out others from it."));
			}

			Optional<UserSession> opt = sessionRepository.findByIdAndUserId(id, userId);
			if (opt.isEmpty()) {
				return ResponseEntity.status(404).body(Map.of("message", "Session not found"));
			}
			UserSession target = opt.get();

			if (caller.get().getId().equals(target.getId())) {
				return ResponseEntity.badRequest().body(Map.of(
						"message",
						"To sign out on this device, use Log out in the app instead of revoking this session."));
			}

			target.setActive(false);
			target.setRevokedAt(new Date());
			target.setCurrent(false);
			sessionRepository.save(target);

			return ResponseEntity.ok(Map.of("success", true, "message", "Session revoked"));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
		}
	}

	@DeleteMapping("/all")
	public ResponseEntity<?> revokeAllSessions(Authentication auth, HttpServletRequest request) {
		String userId = (String) auth.getPrincipal();
		try {
			Optional<UserSession> callerOpt = resolveCallerSession(userId, request);
			if (callerOpt.isEmpty()) {
				return ResponseEntity.status(403).body(Map.of(
						"message",
						"Register this device after sign-in, then you can sign out other devices."));
			}
			UserSession caller = callerOpt.get();
			if (!caller.isPrimary()) {
				return ResponseEntity.status(403).body(Map.of(
						"message",
						"Only the primary device can sign out all other devices."));
			}

			List<UserSession> sessions =
					sessionRepository.findByUserIdAndIsActiveTrueOrderByLastActiveDesc(userId);
			Date now = new Date();
			int count = 0;
			for (UserSession s : sessions) {
				if (s.getId().equals(caller.getId())) {
					continue;
				}
				s.setActive(false);
				s.setRevokedAt(now);
				s.setCurrent(false);
				count++;
			}
			sessionRepository.saveAll(sessions);

			return ResponseEntity.ok(Map.of(
					"success", true,
					"message", count + " other session(s) revoked"));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
		}
	}

	/** Record first-device anchor once (deviceKey preferred; else platform+model). */
	private void ensurePrimaryAnchor(UserPreferences prefs, String deviceKeyRaw, String platform, String deviceModel) {
		if (prefs.getPrimaryAnchorKey() != null && !prefs.getPrimaryAnchorKey().isBlank()) {
			return;
		}
		String anchor;
		if (deviceKeyRaw != null && !deviceKeyRaw.isBlank()) {
			anchor = deviceKeyRaw.trim();
		} else {
			anchor = "model:" + normPlatform(platform) + ":" + normModel(deviceModel);
		}
		prefs.setPrimaryAnchorKey(anchor);
		prefs.setPrimaryDevicePlatform(platform);
		prefs.setPrimaryDeviceModel(deviceModel);
	}

	private static boolean matchesPrimaryDevice(UserPreferences prefs, String deviceKeyRaw, String platform, String deviceModel) {
		if (prefs.getPrimaryAnchorKey() == null || prefs.getPrimaryAnchorKey().isBlank()) {
			return false;
		}
		String anchor = prefs.getPrimaryAnchorKey().trim();
		String dk = deviceKeyRaw != null ? deviceKeyRaw.trim() : "";
		if (!dk.isEmpty() && dk.equals(anchor)) {
			return true;
		}
		// Same model + platform as first session (e.g. app reinstall with new deviceKey)
		return normPlatform(platform).equals(normPlatform(prefs.getPrimaryDevicePlatform()))
				&& normModel(deviceModel).equals(normModel(prefs.getPrimaryDeviceModel()));
	}

	private static String normPlatform(String p) {
		return p == null ? "" : p.trim().toLowerCase(Locale.ROOT);
	}

	private static String normModel(String m) {
		return m == null ? "" : m.trim();
	}

	private Optional<UserSession> resolveCallerSession(String userId, HttpServletRequest request) {
		String headerSid = request.getHeader("X-Device-Session-Id");
		if (headerSid != null && !headerSid.isBlank()) {
			Optional<UserSession> byHeader = sessionRepository.findByIdAndUserId(headerSid.trim(), userId);
			if (byHeader.isPresent() && byHeader.get().isActive()) {
				return byHeader;
			}
		}
		return sessionRepository.findByUserIdAndIsCurrentTrueAndIsActiveTrue(userId);
	}

	private String getStr(Map<String, Object> body, String key, String def) {
		Object val = body.get(key);
		return val != null ? val.toString() : def;
	}

	private String getClientIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isBlank()) {
			return xForwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
