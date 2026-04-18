// src/main/java/com/stockbuddy/model/Prediction.java
package com.stockbuddy.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "predictions")
@CompoundIndexes({
    @CompoundIndex(name = "userId_status_idx", def = "{'userId': 1, 'status': 1}"),
    @CompoundIndex(name = "userId_symbol_status_idx", def = "{'userId': 1, 'symbol': 1, 'status': 1}")
})
public class Prediction {

    @Id
    private String id;

    private String userId;

    private String symbol;

    private int daysAhead;

    private List<PredictionPoint> predictions = new ArrayList<>();

    private Sentiment sentiment;

    private Date createdAt = new Date();

    private String status = "pending"; // pending, running, completed, failed, stopped

    @Indexed(unique = true)
    private String taskId;

    @Indexed(unique = true)
    private String predictionId = UUID.randomUUID().toString();

    private String error;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public int getDaysAhead() {
		return daysAhead;
	}

	public void setDaysAhead(int daysAhead) {
		this.daysAhead = daysAhead;
	}

	public List<PredictionPoint> getPredictions() {
		return predictions;
	}

	public void setPredictions(List<PredictionPoint> predictions) {
		this.predictions = predictions;
	}

	public Sentiment getSentiment() {
		return sentiment;
	}

	public void setSentiment(Sentiment sentiment) {
		this.sentiment = sentiment;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public String getPredictionId() {
		return predictionId;
	}

	public void setPredictionId(String predictionId) {
		this.predictionId = predictionId;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
    
    
    
}
