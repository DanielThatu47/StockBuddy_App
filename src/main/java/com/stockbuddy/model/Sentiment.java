// src/main/java/com/stockbuddy/model/Sentiment.java
package com.stockbuddy.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sentiment {
    private SentimentTotals totals;
    private String summary;
	public SentimentTotals getTotals() {
		return totals;
	}
	public void setTotals(SentimentTotals totals) {
		this.totals = totals;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
    
    
}
