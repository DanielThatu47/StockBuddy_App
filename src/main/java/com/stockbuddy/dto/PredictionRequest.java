// src/main/java/com/stockbuddy/dto/PredictionRequest.java
package com.stockbuddy.dto;

import lombok.Data;

@Data
public class PredictionRequest {
    private String symbol;
    private Integer daysAhead;
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public Integer getDaysAhead() {
		return daysAhead;
	}
	public void setDaysAhead(Integer daysAhead) {
		this.daysAhead = daysAhead;
	}
    
    
}
