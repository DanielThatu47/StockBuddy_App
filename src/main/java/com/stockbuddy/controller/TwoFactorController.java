// src/main/java/com/stockbuddy/controller/TwoFactorController.java
package com.stockbuddy.controller;

import com.stockbuddy.model.OTPRecord;
import com.stockbuddy.model.UserPreferences;
import com.stockbuddy.model.User;
import com.stockbuddy.repository.OTPRepository;
import com.stockbuddy.repository.UserPreferencesRepository;
import com.stockbuddy.repository.UserRepository;
import com.stockbuddy.service.EmailService;
import com.stockbuddy.dto.OTPRequest;
import com.stockbuddy.dto.VerifyOTPRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Two-Factor Authentication and Email Verification
 *
 * POST /api/2fa/send-otp — generate OTP (for 2FA enable OR email verify) POST
 * /api/2fa/verify-otp — verify the OTP and enable 2FA / mark email verified
 * POST /api/2fa/disable — disable 2FA GET /api/2fa/status — get current 2FA and
 * email-verified status
 */
@RestController
@RequestMapping("/api/2fa")
public class TwoFactorController {

	@Autowired
	private OTPRepository otpRepository;
	@Autowired
	private UserPreferencesRepository preferencesRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private EmailService emailService;

	private final SecureRandom random = new SecureRandom();

	// ── POST /api/2fa/send-otp ────────────────────────────────────
	// purpose: "2FA_ENABLE" or "EMAIL_VERIFY"
	@PostMapping("/send-otp")
	public ResponseEntity<?> sendOtp(@RequestBody Map<String, Object> body, Authentication auth) {
		String userId = (String) auth.getPrincipal();
		String purpose = body.get("purpose") != null ? body.get("purpose").toString() : "2FA_ENABLE";

		try {
			// delete old OTP
			otpRepository.deleteByUserIdAndPurpose(userId, purpose);

			// generate OTP
			String otp = String.format("%06d", random.nextInt(1_000_000));

			OTPRecord record = new OTPRecord(userId, otp, purpose);
			otpRepository.save(record);

			boolean mailed = false;
			Optional<User> userOpt = userRepository.findById(userId);
			if (userOpt.isPresent() && emailService.isMailAvailable()) {
				try {
					emailService.sendOtpEmail(userOpt.get().getEmail(), purpose, otp);
					mailed = true;
				} catch (Exception mailEx) {
					mailEx.printStackTrace();
				}
			}

			Map<String, Object> emailbody = new LinkedHashMap<>();
			emailbody.put("success", true);
			emailbody.put("message", mailed ? "Verification code sent to your email"
					: (emailService.isMailAvailable() ? "Could not send email; use a new code request or check SMTP settings"
							: "Verification code generated (configure SMTP to receive it by email)"));
			emailbody.put("expiresIn", 300);
			emailbody.put("sentByEmail", mailed);
			if (!mailed) {
				emailbody.put("otpPreview", otp);
			}

			return ResponseEntity.ok(emailbody);

		} catch (Exception e) {
			e.printStackTrace(); // 🔥 VERY IMPORTANT
			return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	// ── POST /api/2fa/verify-otp ──────────────────────────────────
	@PostMapping("/verify-otp")
	public ResponseEntity<?> verifyOtp(@RequestBody VerifyOTPRequest request, Authentication auth) {
		String userId = (String) auth.getPrincipal();
		String inputOtp = request.getOtp();
		String purpose = request.getPurpose() != null ? request.getPurpose() : "2FA_ENABLE";
		try {
			Optional<OTPRecord> opt = otpRepository.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(userId,
					purpose);

			if (opt.isEmpty()) {
				return ResponseEntity.badRequest()
						.body(Map.of("success", false, "message", "OTP not found or already used. Request a new one."));
			}

			OTPRecord record = opt.get();

			// Check expiry
			if (record.getExpiresAt().before(new Date())) {
				return ResponseEntity.badRequest()
						.body(Map.of("success", false, "message", "OTP has expired. Please request a new one."));
			}

			// Verify
			if (!record.getOtp().equals(inputOtp)) {
				return ResponseEntity.badRequest()
						.body(Map.of("success", false, "message", "Invalid OTP. Please try again."));
			}

			// Mark OTP as used
			record.setUsed(true);
			otpRepository.save(record);

			// Update user preferences
			UserPreferences prefs = preferencesRepository.findByUserId(userId).orElseGet(() -> {
				UserPreferences p = new UserPreferences();
				p.setUserId(userId);
				return p;
			});

			if ("2FA_ENABLE".equals(purpose)) {
				prefs.setTwoFactorEnabled(true);
			} else if ("EMAIL_VERIFY".equals(purpose)) {
				prefs.setEmailVerified(true);
			}

			prefs.setUpdatedAt(new Date());
			preferencesRepository.save(prefs);

			return ResponseEntity.ok(Map.of("success", true, "message",
					"2FA_ENABLE".equals(purpose) ? "Two-factor authentication enabled successfully"
							: "Email verified successfully",
					"preferences", prefs));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	// ── POST /api/2fa/disable ─────────────────────────────────────
	@PostMapping("/disable")
	public ResponseEntity<?> disableTwoFactor(Authentication auth) {
		String userId = (String) auth.getPrincipal();
		try {
			UserPreferences prefs = preferencesRepository.findByUserId(userId).orElseGet(() -> {
				UserPreferences p = new UserPreferences();
				p.setUserId(userId);
				return p;
			});
			prefs.setTwoFactorEnabled(false);
			prefs.setTwoFactorSecret(null);
			prefs.setUpdatedAt(new Date());
			preferencesRepository.save(prefs);

			return ResponseEntity.ok(Map.of("success", true, "message", "Two-factor authentication disabled"));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	// ── GET /api/2fa/status ───────────────────────────────────────
	@GetMapping("/status")
	public ResponseEntity<?> getStatus(Authentication auth) {
		String userId = (String) auth.getPrincipal();
		try {
			UserPreferences prefs = preferencesRepository.findByUserId(userId).orElseGet(() -> {
				UserPreferences p = new UserPreferences();
				p.setUserId(userId);
				return p;
			});
			return ResponseEntity.ok(Map.of("success", true, "twoFactorEnabled", prefs.isTwoFactorEnabled(),
					"emailVerified", prefs.isEmailVerified()));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
		}
	}
}
