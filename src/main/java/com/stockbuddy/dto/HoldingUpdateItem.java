// src/main/java/com/stockbuddy/dto/HoldingUpdateItem.java
package com.stockbuddy.dto;

import lombok.Data;

@Data
public class HoldingUpdateItem {
    private String symbol;
    private double currentPrice;
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public double getCurrentPrice() {
		return currentPrice;
	}
	public void setCurrentPrice(double currentPrice) {
		this.currentPrice = currentPrice;
	}
    
    
}
