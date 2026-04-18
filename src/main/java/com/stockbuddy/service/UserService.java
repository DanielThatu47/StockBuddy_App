// src/main/java/com/stockbuddy/service/UserService.java
package com.stockbuddy.service;

import com.stockbuddy.dto.UserDto;
import com.stockbuddy.model.User;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    /**
     * Convert a User entity to the public-facing UserDto.
     * Mirrors the user object shape returned in every Node.js route.
     */
    public UserDto toDto(User user) {
    	 UserDto dto = new UserDto();

    	    dto.setId(user.getId());
    	    dto.setName(user.getName() != null ? user.getName() : "");
    	    dto.setEmail(user.getEmail() != null ? user.getEmail() : "");
    	    dto.setCountryCode(user.getCountryCode() != null ? user.getCountryCode() : "+1");
    	    dto.setPhoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
    	    dto.setAddress(user.getAddress() != null ? user.getAddress() : "");
    	    dto.setProfilePicture(user.getProfilePicture() != null ? user.getProfilePicture() : "");
    	    dto.setDateOfBirth(user.getDateOfBirth());
    	    dto.setCreatedAt(user.getCreatedAt());
    	    dto.setLastLogin(user.getLastLogin());
    	    dto.setCaptchaVerified(user.isCaptchaVerified());
		    return dto;
    }
}
