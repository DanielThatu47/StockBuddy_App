// src/main/java/com/stockbuddy/dto/VerifyCaptchaRequest.java
package com.stockbuddy.dto;

import lombok.Data;

@Data
public class VerifyCaptchaRequest {
    private String userInput;

	public String getUserInput() {
		return userInput;
	}

	public void setUserInput(String userInput) {
		this.userInput = userInput;
	}
    
    
}
