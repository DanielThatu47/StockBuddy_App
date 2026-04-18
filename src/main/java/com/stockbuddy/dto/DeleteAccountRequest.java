// src/main/java/com/stockbuddy/dto/DeleteAccountRequest.java
package com.stockbuddy.dto;

import lombok.Data;

@Data
public class DeleteAccountRequest {
    private String password;
    private String confirmationText;
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getConfirmationText() {
		return confirmationText;
	}
	public void setConfirmationText(String confirmationText) {
		this.confirmationText = confirmationText;
	}
    
    
}
