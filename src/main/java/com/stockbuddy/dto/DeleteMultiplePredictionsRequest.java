// src/main/java/com/stockbuddy/dto/DeleteMultiplePredictionsRequest.java
package com.stockbuddy.dto;

import lombok.Data;
import java.util.List;

@Data
public class DeleteMultiplePredictionsRequest {
    private List<String> ids;

	public List<String> getIds() {
		return ids;
	}

	public void setIds(List<String> ids) {
		this.ids = ids;
	}
    
    
}
