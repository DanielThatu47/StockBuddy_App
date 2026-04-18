// src/main/java/com/stockbuddy/model/OTPRecord.java
package com.stockbuddy.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@Document(collection = "otp_records")
public class OTPRecord {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String purpose;   // "2FA_ENABLE" | "EMAIL_VERIFY" | "LOGIN_2FA"

    private String otp;       // 6-digit code

    private boolean used = false;

    private Date createdAt = new Date();

    // Auto-expire after 5 minutes
    @Indexed(expireAfterSeconds = 300)
    private Date expiresAt;

    public OTPRecord(String userId, String otp, String purpose) {
        this.userId  = userId;
        this.otp     = otp;
        this.purpose = purpose;
        this.createdAt = new Date();
        this.expiresAt = new Date(System.currentTimeMillis() + 5 * 60 * 1000);
    }

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public String getOtp() {
		return otp;
	}

	public void setOtp(String otp) {
		this.otp = otp;
	}

	public boolean isUsed() {
		return used;
	}

	public void setUsed(boolean used) {
		this.used = used;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Date expiresAt) {
		this.expiresAt = expiresAt;
	}
    
    
}
