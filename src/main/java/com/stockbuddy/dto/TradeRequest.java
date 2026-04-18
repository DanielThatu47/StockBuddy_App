// src/main/java/com/stockbuddy/dto/TradeRequest.java
package com.stockbuddy.dto;

import lombok.Data;

@Data
public class TradeRequest {
    private String symbol;
    private String companyName;
    private String type;   // BUY | SELL
    private int quantity;
    private double price;
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public String getCompanyName() {
		return companyName;
	}
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public int getQuantity() {
		return quantity;
	}
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
    
}
