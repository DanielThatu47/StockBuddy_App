// src/main/java/com/stockbuddy/dto/UpdateHoldingsRequest.java
package com.stockbuddy.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateHoldingsRequest {
    private List<HoldingUpdateItem> holdings;

	public List<HoldingUpdateItem> getHoldings() {
		return holdings;
	}

	public void setHoldings(List<HoldingUpdateItem> holdings) {
		this.holdings = holdings;
	}
    
    
}
