// src/main/java/com/stockbuddy/model/User.java
package com.stockbuddy.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @NotBlank
    @Email
    @Indexed(unique = true)
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    private String countryCode = "+1";

    private String phoneNumber = "";

    private String address = "";

    private String profilePicture = "";

    private Date dateOfBirth;

    private boolean captchaVerified = false;

    @CreatedDate
    private Date createdAt = new Date();

	private Date lastLogin;

	/** Bcrypt hash of short numeric reset code; cleared after successful reset or expiry. */
	private String passwordResetCodeHash;

	/** When the reset code expires (null if none pending). */
	private Date passwordResetExpires;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getProfilePicture() {
		return profilePicture;
	}

	public void setProfilePicture(String profilePicture) {
		this.profilePicture = profilePicture;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public boolean isCaptchaVerified() {
		return captchaVerified;
	}

	public void setCaptchaVerified(boolean captchaVerified) {
		this.captchaVerified = captchaVerified;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}

	public String getPasswordResetCodeHash() {
		return passwordResetCodeHash;
	}

	public void setPasswordResetCodeHash(String passwordResetCodeHash) {
		this.passwordResetCodeHash = passwordResetCodeHash;
	}

	public Date getPasswordResetExpires() {
		return passwordResetExpires;
	}

	public void setPasswordResetExpires(Date passwordResetExpires) {
		this.passwordResetExpires = passwordResetExpires;
	}
	
	
	
}
