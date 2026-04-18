// src/main/java/com/stockbuddy/model/DemoTradingAccount.java
package com.stockbuddy.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "demotradingaccounts")
public class DemoTradingAccount {

	@Id
	private String id;

	@Indexed(unique = true)
	private String userId;

	private double balance = 100000.0;

	private double initialBalance = 100000.0;

	private double equity = 0.0;

	private double totalProfitLoss = 0.0;

	private double totalProfitLossPercentage = 0.0;

	private double dayChange = 0.0;

	private double dayChangePercentage = 0.0;

	private double weekChange = 0.0;

	private double weekChangePercentage = 0.0;

	private double monthChange = 0.0;

	private double monthChangePercentage = 0.0;

	private double yearChange = 0.0;

	private double yearChangePercentage = 0.0;

	private List<Holding> holdings = new ArrayList<>();

	private List<Transaction> transactions = new ArrayList<>();

	private Date createdAt = new Date();

	private Date lastUpdated = new Date();

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

	public double getBalance() {
		return balance;
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}

	public double getInitialBalance() {
		return initialBalance;
	}

	public void setInitialBalance(double initialBalance) {
		this.initialBalance = initialBalance;
	}

	public double getEquity() {
		return equity;
	}

	public void setEquity(double equity) {
		this.equity = equity;
	}

	public double getTotalProfitLoss() {
		return totalProfitLoss;
	}

	public void setTotalProfitLoss(double totalProfitLoss) {
		this.totalProfitLoss = totalProfitLoss;
	}

	public double getTotalProfitLossPercentage() {
		return totalProfitLossPercentage;
	}

	public void setTotalProfitLossPercentage(double totalProfitLossPercentage) {
		this.totalProfitLossPercentage = totalProfitLossPercentage;
	}

	public double getDayChange() {
		return dayChange;
	}

	public void setDayChange(double dayChange) {
		this.dayChange = dayChange;
	}

	public double getDayChangePercentage() {
		return dayChangePercentage;
	}

	public void setDayChangePercentage(double dayChangePercentage) {
		this.dayChangePercentage = dayChangePercentage;
	}

	public double getWeekChange() {
		return weekChange;
	}

	public void setWeekChange(double weekChange) {
		this.weekChange = weekChange;
	}

	public double getWeekChangePercentage() {
		return weekChangePercentage;
	}

	public void setWeekChangePercentage(double weekChangePercentage) {
		this.weekChangePercentage = weekChangePercentage;
	}

	public double getMonthChange() {
		return monthChange;
	}

	public void setMonthChange(double monthChange) {
		this.monthChange = monthChange;
	}

	public double getMonthChangePercentage() {
		return monthChangePercentage;
	}

	public void setMonthChangePercentage(double monthChangePercentage) {
		this.monthChangePercentage = monthChangePercentage;
	}

	public double getYearChange() {
		return yearChange;
	}

	public void setYearChange(double yearChange) {
		this.yearChange = yearChange;
	}

	public double getYearChangePercentage() {
		return yearChangePercentage;
	}

	public void setYearChangePercentage(double yearChangePercentage) {
		this.yearChangePercentage = yearChangePercentage;
	}

	public List<Holding> getHoldings() {
		return holdings;
	}

	public void setHoldings(List<Holding> holdings) {
		this.holdings = holdings;
	}

	public List<Transaction> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<Transaction> transactions) {
		this.transactions = transactions;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	/**
	 * Recalculates equity and profit/loss from current holdings and balance. Called
	 * before saving (mirrors Mongoose pre-save hook).
	 */
	public void recalculate() {
		double holdingsValue = holdings.stream().mapToDouble(h -> h.getCurrentValue()).sum();

		this.equity = holdingsValue + this.balance;
		this.totalProfitLoss = this.equity - this.initialBalance;
		this.totalProfitLossPercentage = (this.totalProfitLoss / this.initialBalance) * 100.0;
		this.lastUpdated = new Date();
	}
}
