// src/main/java/com/stockbuddy/model/SentimentTotals.java
package com.stockbuddy.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SentimentTotals {
    private int positive;
    private int negative;
    private int neutral;
	public int getPositive() {
		return positive;
	}
	public void setPositive(int positive) {
		this.positive = positive;
	}
	public int getNegative() {
		return negative;
	}
	public void setNegative(int negative) {
		this.negative = negative;
	}
	public int getNeutral() {
		return neutral;
	}
	public void setNeutral(int neutral) {
		this.neutral = neutral;
	}
    
    
}
