// src/main/java/com/stockbuddy/service/CloudinaryService.java
package com.stockbuddy.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    /**
     * Upload a MultipartFile to Cloudinary under the "profile-pictures" folder.
     */
    public String uploadProfilePicture(MultipartFile file) {
        try {
            System.out.println("==== UPLOAD START ====");
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File type: " + file.getContentType());
            System.out.println("File size: " + file.getSize());

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
                throw new IllegalArgumentException("Only image files are allowed");
            }

            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "profile-pictures",
                            "resource_type", "image",
                            "transformation", "c_limit,w_500,h_500" // ✅ FIXED
                    )
            );

            String url = (String) result.get("secure_url");

            System.out.println("Upload success: " + url);
            return url;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Cloudinary upload failed: " + e.getMessage());
        }
    }
    /**
     * Delete an image from Cloudinary using its full URL.
     */
    public Map<String, Object> deleteByUrl(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isBlank()) {
                return Map.of("result", "not_deleted", "reason", "invalid_url");
            }

            String publicId = extractPublicId(imageUrl);
            if (publicId == null) {
                return Map.of("result", "not_deleted", "reason", "invalid_public_id");
            }

            //  Use single delete instead of bulk API
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.emptyMap()
            );

            return Map.of(
                    "result", result.get("result"),
                    "public_id", publicId
            );

        } catch (Exception e) {
        	e.printStackTrace();
            throw new RuntimeException("Cloudinary delete failed", e);
        }
    }

    /**
     * Extract the Cloudinary public_id from a full secure URL.
     */
    private String extractPublicId(String url) {
        try {
            if (!url.contains("cloudinary.com")) return null;

            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) return null;

            String publicPath = url.substring(uploadIndex + 8);

            // Remove query params
            int queryIndex = publicPath.indexOf('?');
            if (queryIndex != -1) {
                publicPath = publicPath.substring(0, queryIndex);
            }

            // Remove transformations + version safely
            publicPath = publicPath.replaceFirst("^.*?/v\\d+/", "");

            // Remove file extension
            int dotIndex = publicPath.lastIndexOf('.');
            if (dotIndex != -1) {
                publicPath = publicPath.substring(0, dotIndex);
            }

            return publicPath;

        } catch (Exception e) {
        	e.printStackTrace();
            return null;
        }
    }
}