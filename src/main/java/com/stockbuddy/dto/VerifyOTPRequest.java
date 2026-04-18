// src/main/java/com/stockbuddy/dto/VerifyOTPRequest.java
package com.stockbuddy.dto;

public class VerifyOTPRequest {
    private String otp;
    private String purpose;

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}