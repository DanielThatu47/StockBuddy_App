// src/main/java/com/stockbuddy/controller/ProfileController.java
package com.stockbuddy.controller;

import com.stockbuddy.dto.DeleteAccountRequest;
import com.stockbuddy.dto.UpdateProfileRequest;
import com.stockbuddy.model.User;
import com.stockbuddy.repository.DemoTradingAccountRepository;
import com.stockbuddy.repository.PredictionRepository;
import com.stockbuddy.repository.SessionRepository;
import com.stockbuddy.repository.UserPreferencesRepository;
import com.stockbuddy.repository.UserRepository;
import com.stockbuddy.repository.WatchlistEntryRepository;
import com.stockbuddy.service.CloudinaryService;
import com.stockbuddy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired private UserRepository userRepository;
    @Autowired private PredictionRepository predictionRepository;
    @Autowired private DemoTradingAccountRepository tradingAccountRepository;
    @Autowired private UserPreferencesRepository preferencesRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private WatchlistEntryRepository watchlistEntryRepository;
    @Autowired private CloudinaryService cloudinaryService;
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;

    // ───────────────────────────────────────────────
    // GET /api/profile
    // ───────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> getProfile(Authentication auth) {
        try {
            String userId = (String) auth.getPrincipal();
            Optional<User> opt = userRepository.findById(userId);

            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false, "message", "User not found"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", userService.toDto(opt.get())));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Server error during profile fetch"));
        }
    }

    // ───────────────────────────────────────────────
    // PUT /api/profile
    // ───────────────────────────────────────────────
    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest req,
                                           Authentication auth) {
        try {
            String userId = (String) auth.getPrincipal();
            Optional<User> opt = userRepository.findById(userId);

            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false, "message", "User not found"));
            }

            User user = opt.get();

            // Update email (check uniqueness)
            if (req.getEmail() != null && !req.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(req.getEmail())) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false, "message", "Email already in use"));
                }
                user.setEmail(req.getEmail());
            }

            if (req.getName() != null)        user.setName(req.getName());
            if (req.getCountryCode() != null) user.setCountryCode(req.getCountryCode());
            if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());
            if (req.getAddress() != null)     user.setAddress(req.getAddress());
            if (req.getProfilePicture() != null) user.setProfilePicture(req.getProfilePicture());

            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", userService.toDto(user)));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false, "message", "Server error"));
        }
    }

    // ───────────────────────────────────────────────
    // POST /api/profile/upload-picture
    // multipart/form-data, field name: "image"
    // ───────────────────────────────────────────────
    @PostMapping(value = "/upload-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPicture(@RequestParam("image") MultipartFile file,
                                           Authentication auth) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "No file uploaded"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null ||
                (!contentType.equals("image/jpeg") &&
                 !contentType.equals("image/jpg") &&
                 !contentType.equals("image/png"))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Unsupported file type. Please upload only JPG or PNG images."));
            }

            // Validate file size (5 MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "File size must not exceed 5MB"));
            }

            String userId = (String) auth.getPrincipal();
            Optional<User> opt = userRepository.findById(userId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false, "message", "User not found"));
            }

            User user = opt.get();

            // Delete old profile picture from Cloudinary if present
            if (user.getProfilePicture() != null && !user.getProfilePicture().isBlank()) {
                try {
                    cloudinaryService.deleteByUrl(user.getProfilePicture());
                } catch (Exception ignored) {}
            }

            // Upload new picture to Cloudinary
            String secureUrl = cloudinaryService.uploadProfilePicture(file);

            // Persist URL to user
            user.setProfilePicture(secureUrl);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "profilePicture", secureUrl,
                    "user", userService.toDto(user)));

        } catch (Exception e) {
        	  e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Image upload to cloud storage failed",
                    "error", e.getMessage()));
        }
    }

    // ───────────────────────────────────────────────
    // DELETE /api/profile/profile-picture
    // ───────────────────────────────────────────────
    @DeleteMapping("/profile-picture")
    public ResponseEntity<?> deleteProfilePicture(Authentication auth) {
        try {
            String userId = (String) auth.getPrincipal();
            Optional<User> opt = userRepository.findById(userId);

            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false, "message", "User not found"));
            }

            User user = opt.get();

            if (user.getProfilePicture() != null && !user.getProfilePicture().isBlank()) {
                try {
                    Map<String, Object> deleteResult = cloudinaryService.deleteByUrl(user.getProfilePicture());
                    String result = (String) deleteResult.get("result");

                    if ("deleted".equals(result)) {
                        System.out.println("Successfully deleted image from Cloudinary");
                    } else {
                        System.out.println("Partial or unknown deletion result: " + deleteResult);
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting from Cloudinary: " + e.getMessage());
                    // Continue even if Cloudinary deletion fails
                }

                user.setProfilePicture("");
                userRepository.save(user);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile picture removed",
                    "user", userService.toDto(user)));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Server error during picture deletion"));
        }
    }

    // ───────────────────────────────────────────────
    // DELETE /api/profile   (delete own account)
    // ───────────────────────────────────────────────
    @DeleteMapping
    public ResponseEntity<?> deleteAccount(@RequestBody DeleteAccountRequest req,
                                           Authentication auth) {
        try {
            // Validate password provided
            if (req.getPassword() == null || req.getPassword().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Password is required to delete your account"));
            }

            // Validate confirmation text
            String expectedConfirmation = "delete my account";
            if (req.getConfirmationText() == null ||
                !req.getConfirmationText().toLowerCase().equals(expectedConfirmation)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Please type \"" + expectedConfirmation +
                                   "\" to confirm account deletion"));
            }

            String userId = (String) auth.getPrincipal();
            Optional<User> opt = userRepository.findById(userId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false, "message", "User not found"));
            }

            User user = opt.get();

            // Verify password
            if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "Incorrect password"));
            }

            // Delete Cloudinary profile picture
            if (user.getProfilePicture() != null && !user.getProfilePicture().isBlank()) {
                try {
                    cloudinaryService.deleteByUrl(user.getProfilePicture());
                } catch (Exception ignored) {}
            }

            // Delete user's predictions
            try {
                predictionRepository.deleteByUserId(userId);
            } catch (Exception ignored) {}

            // Delete demo trading account
            try {
                tradingAccountRepository.deleteByUserId(userId);
            } catch (Exception ignored) {}

            // Delete user preferences
            try {
                preferencesRepository.deleteByUserId(userId);
            } catch (Exception ignored) {}

            // Delete user sessions
            try {
                sessionRepository.deleteByUserId(userId);
            } catch (Exception ignored) {}

            try {
                watchlistEntryRepository.deleteByUserId(userId);
            } catch (Exception ignored) {}

            // Delete the account
            userRepository.deleteById(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Account successfully deleted with all associated data"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Server error during account deletion"));
        }
    }
}
