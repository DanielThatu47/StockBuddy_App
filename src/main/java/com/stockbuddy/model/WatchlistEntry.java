// src/main/java/com/stockbuddy/model/WatchlistEntry.java
package com.stockbuddy.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * One saved symbol per user (unique userId + symbol).
 */
@Document(collection = "watchlist_entries")
@CompoundIndexes({
		@CompoundIndex(name = "user_symbol", def = "{'userId': 1, 'symbol': 1}", unique = true)
})
public class WatchlistEntry {

	@Id
	private String id;

	private String userId;
	private String symbol;
	private String displaySymbol;
	private String description;
	private String exchange;

	private Date createdAt = new Date();

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

	public String getDisplaySymbol() {
		return displaySymbol;
	}

	public void setDisplaySymbol(String displaySymbol) {
		this.displaySymbol = displaySymbol;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
}
