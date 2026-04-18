//src/main/java/com/stockbuddy/controller/AuthController.java

package com.stockbuddy.controller;

import com.stockbuddy.dto.*;
import com.stockbuddy.model.DemoTradingAccount;
import com.stockbuddy.model.User;
import com.stockbuddy.repository.DemoTradingAccountRepository;
import com.stockbuddy.repository.PredictionRepository;
import com.stockbuddy.repository.SessionRepository;
import com.stockbuddy.repository.UserPreferencesRepository;
import com.stockbuddy.repository.UserRepository;
import com.stockbuddy.repository.WatchlistEntryRepository;
import com.stockbuddy.security.JwtUtil;
import com.stockbuddy.service.CloudinaryService;
import com.stockbuddy.service.EmailService;
import com.stockbuddy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.security.SecureRandom;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private PredictionRepository predictionRepository;
    @Autowired private DemoTradingAccountRepository tradingAccountRepository;
    @Autowired private UserPreferencesRepository preferencesRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private WatchlistEntryRepository watchlistEntryRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserService userService;
    @Autowired private CloudinaryService cloudinaryService;
    @Autowired private EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${admin.emails:admin@stockbuddy.com}")
    private String adminEmailsConfig;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    // ───────────────────────────────────────────────
    // POST /api/register
    // ───────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            String name     = req.getName();
            String email    = req.getEmail();
            String password = req.getPassword();
            String address  = req.getAddress();
            String dob      = req.getDateOfBirth();

            // Validation
            if (name == null || name.isBlank() ||
                email == null || email.isBlank() ||
                password == null || password.isBlank()) {

                Map<String, Object> errors = new LinkedHashMap<>();
                if (name == null || name.isBlank()) errors.put("name", "Name is required");
                if (email == null || email.isBlank()) errors.put("email", "Email is required");
                if (password == null || password.isBlank()) errors.put("password", "Password is required");
                if (address == null || address.isBlank()) errors.put("address", "Address is required");
                if (dob == null || dob.isBlank()) errors.put("dateOfBirth", "Date Of Birth is Required");

                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Validation failed",
                        "validationErrors", errors));
            }

            if (password.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Password must be at least 6 characters long"));
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid email format"));
            }

            if (userRepository.existsByEmail(email.toLowerCase())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "User already exists"));
            }

            if (!req.isCaptchaVerified()) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "CAPTCHA verification required"));
            }

            Date now = new Date();

            // Create user
            User user = new User();
            user.setName(name.trim());
            user.setEmail(email.toLowerCase().trim());
            user.setPassword(passwordEncoder.encode(password));
            user.setAddress(address != null ? address.trim() : "");
            user.setCountryCode(req.getCountryCode() != null ? req.getCountryCode().trim() : "+1");
            user.setPhoneNumber(req.getPhoneNumber() != null ? req.getPhoneNumber().trim() : "");
            user.setCaptchaVerified(req.isCaptchaVerified());

            if (dob != null && !dob.isBlank()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sdf.setLenient(false);
                    user.setDateOfBirth(sdf.parse(dob.trim()));
                } catch (Exception ignored) {
                }
            }

            user.setCreatedAt(now);
            user.setLastLogin(now);

            userRepository.save(user);

            // ✅ Safe Demo Account Creation
            DemoTradingAccount tradingAccount;

            if (!tradingAccountRepository.existsByUserId(user.getId())) {

                tradingAccount = new DemoTradingAccount();
                tradingAccount.setUserId(user.getId());
                tradingAccount.setBalance(100000.0);
                tradingAccount.setInitialBalance(100000.0);
                tradingAccount.setEquity(100000.0);
                tradingAccount.setHoldings(new ArrayList<>());
                tradingAccount.setTransactions(new ArrayList<>());
                tradingAccount.setCreatedAt(now);
                tradingAccount.setLastUpdated(now);

                try {
                    tradingAccountRepository.save(tradingAccount);
                } catch (Exception e) {
                    tradingAccount = tradingAccountRepository
                            .findByUserId(user.getId())
                            .orElse(null);
                }

            } else {
                tradingAccount = tradingAccountRepository
                        .findByUserId(user.getId())
                        .orElse(null);
            }

            // ✅ RETURN RESPONSE (YOU MISSED THIS)
            String token = jwtUtil.generateToken(user.getId());

            return ResponseEntity.status(201).body(Map.of(
                    "success", true,
                    "token", token,
                    "user", userService.toDto(user),
                    "tradingAccount", Map.of(
                            "balance", tradingAccount.getBalance(),
                            "initialBalance", tradingAccount.getInitialBalance(),
                            "equity", tradingAccount.getEquity()
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Server error during registration",
                    "error", e.getMessage()));
        }
    }
    // ───────────────────────────────────────────────
    // POST /api/login
    // ───────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            if (req.getEmail() == null || req.getPassword() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "All fields are required"));
            }

            Optional<User> optUser = userRepository.findByEmail(req.getEmail());
            if (optUser.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "Invalid credentials"));
            }

            User user = optUser.get();

            if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "Invalid credentials"));
            }

            if (!user.isCaptchaVerified()) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "CAPTCHA verification required",
                        "requiresCaptcha", true));
            }

            // Update last login
            user.setLastLogin(new Date());
            userRepository.save(user);

            String token = jwtUtil.generateToken(user.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "token", token,
                    "user", userService.toDto(user)));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false, "message", "Server error"));
        }
    }

    // ───────────────────────────────────────────────
    // POST /api/forgot-password/request
    // ───────────────────────────────────────────────
    @PostMapping("/forgot-password/request")
    public ResponseEntity<?> forgotPasswordRequest(@RequestBody Map<String, String> body) {
        try {
            String raw = body.get("email");
            if (raw == null || raw.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Email is required"));
            }
            String email = raw.trim().toLowerCase(Locale.ROOT);
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid email format"));
            }

            Optional<User> optUser = userRepository.findByEmail(email);
            if (optUser.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "exists", false,
                        "message", "No account found with this email."));
            }

            User user = optUser.get();
            String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
            user.setPasswordResetCodeHash(passwordEncoder.encode(code));
            user.setPasswordResetExpires(new Date(System.currentTimeMillis() + 15L * 60L * 1000L));
            userRepository.save(user);

            try {
                emailService.sendOtpEmail(email, "PASSWORD_RESET", code);
            } catch (Exception ex) {
                // Log only; client may use devCode when SMTP is not configured
                System.err.println("Password reset email failed: " + ex.getMessage());
            }

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("success", true);
            res.put("exists", true);
            if (emailService.isMailAvailable()) {
                res.put("message", "A verification code was sent to your registered email address.");
            } else {
                res.put("message",
                        "Email is not configured on the server. Use the development code below to continue.");
                res.put("devCode", code);
            }
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Server error"));
        }
    }

    // ───────────────────────────────────────────────
    // POST /api/forgot-password/reset
    // ───────────────────────────────────────────────
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> forgotPasswordReset(@RequestBody Map<String, String> body) {
        try {
            String rawEmail = body.get("email");
            String code = body.get("code");
            String newPassword = body.get("newPassword");
            if (rawEmail == null || rawEmail.isBlank() || code == null || code.isBlank()
                    || newPassword == null || newPassword.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Email, code, and new password are required"));
            }
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "New password must be at least 6 characters"));
            }

            String email = rawEmail.trim().toLowerCase(Locale.ROOT);
            Optional<User> optUser = userRepository.findByEmail(email);
            if (optUser.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No account found with this email."));
            }

            User user = optUser.get();
            if (user.getPasswordResetCodeHash() == null || user.getPasswordResetExpires() == null
                    || new Date().after(user.getPasswordResetExpires())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Reset code is invalid or has expired. Request a new code."));
            }

            if (!passwordEncoder.matches(code.trim(), user.getPasswordResetCodeHash())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid verification code."));
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordResetCodeHash(null);
            user.setPasswordResetExpires(null);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Your password has been reset. You can sign in now."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Server error"));
        }
    }

    // ───────────────────────────────────────────────
    // POST /api/change-password  (requires auth)
    // ───────────────────────────────────────────────
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req,
                                            Authentication auth) {
        try {
            if (req.getCurrentPassword() == null || req.getNewPassword() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Current password and new password are required"));
            }

            if (req.getNewPassword().length() < 8) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "New password must be at least 8 characters long"));
            }

            String userId = (String) auth.getPrincipal();
            Optional<User> optUser = userRepository.findById(userId);
            if (optUser.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false, "message", "User not found"));
            }

            User user = optUser.get();

            if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "Current password is incorrect"));
            }

            user.setPassword(passwordEncoder.encode(req.getNewPassword()));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true, "message", "Password updated successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Server error during password change",
                    "error", e.getMessage()));
        }
    }

    // ───────────────────────────────────────────────
    // DELETE /api/admin/delete-user  (requires auth + admin)
    // ───────────────────────────────────────────────
    @DeleteMapping("/admin/delete-user")
    public ResponseEntity<?> adminDeleteUser(@RequestBody AdminDeleteUserRequest req,
                                             Authentication auth) {
        try {
            String requestingUserId = (String) auth.getPrincipal();
            Optional<User> optRequesting = userRepository.findById(requestingUserId);
            if (optRequesting.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false, "message", "Requesting user not found"));
            }

            User requestingUser = optRequesting.get();

            // Require password
            if (req.getPassword() == null || req.getPassword().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Password is required for this operation"));
            }

            // Require confirmation text
            String target = req.getEmail() != null ? req.getEmail() : req.getName();
            String expected = "delete user " + target;
            if (req.getConfirmationText() == null ||
                !req.getConfirmationText().toLowerCase().equals(expected)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Please type \"" + expected + "\" to confirm user deletion"));
            }

            // Verify admin password
            if (!passwordEncoder.matches(req.getPassword(), requestingUser.getPassword())) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false, "message", "Authentication failed"));
            }

            // Check admin email whitelist
            List<String> adminEmails = Arrays.asList(adminEmailsConfig.split(","));
            if (!adminEmails.contains(requestingUser.getEmail().toLowerCase())) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Not authorized to perform this action"));
            }

            if (req.getEmail() == null && req.getName() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Email or name is required to delete a user"));
            }

            // Find user to delete
            Optional<User> optTarget = req.getEmail() != null
                    ? userRepository.findByEmail(req.getEmail())
                    : userRepository.findByName(req.getName());

            if (optTarget.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "User not found with the provided credentials"));
            }

            User userToDelete = optTarget.get();

            // Prevent self-deletion
            if (userToDelete.getId().equals(requestingUserId)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Admin cannot delete their own account using this endpoint"));
            }

            // Delete Cloudinary profile picture
            if (userToDelete.getProfilePicture() != null && !userToDelete.getProfilePicture().isBlank()) {
                try {
                    cloudinaryService.deleteByUrl(userToDelete.getProfilePicture());
                } catch (Exception ignored) {}
            }

            // Delete user predictions
            try {
                predictionRepository.deleteByUserId(userToDelete.getId());
            } catch (Exception ignored) {}

            // Delete demo trading account
            try {
                tradingAccountRepository.deleteByUserId(userToDelete.getId());
            } catch (Exception ignored) {}

            // Delete user preferences
            try {
                preferencesRepository.deleteByUserId(userToDelete.getId());
            } catch (Exception ignored) {}

            // Delete user sessions
            try {
                sessionRepository.deleteByUserId(userToDelete.getId());
            } catch (Exception ignored) {}

            try {
                watchlistEntryRepository.deleteByUserId(userToDelete.getId());
            } catch (Exception ignored) {}

            // Delete user
            userRepository.deleteById(userToDelete.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Account for " + target + " successfully deleted with all associated data"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false, "message", "Server error during account deletion"));
        }
    }
}