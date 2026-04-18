// src/main/java/com/stockbuddy/model/UserPreferences.java
package com.stockbuddy.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@Document(collection = "user_preferences")
public class UserPreferences {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    // ── Appearance ────────────────────────────────────────
    private boolean darkMode = false;

    // ── Notifications ─────────────────────────────────────
    private boolean pushNotifications  = true;
    private boolean emailNotifications = true;
    private boolean marketAlerts       = true;
    private boolean portfolioUpdates   = true;
    private boolean newsUpdates        = true;
    private boolean tradingSignals     = true;
    private boolean systemUpdates      = true;
    private boolean marketingEmails    = false;

    // ── Push Token (Expo) ─────────────────────────────────
    private String expoPushToken;

    // ── Regional ──────────────────────────────────────────
    private String language = "English";
    private String currency = "USD";
    private String timezone = "UTC+0";

    // ── Security ──────────────────────────────────────────
    private boolean twoFactorEnabled = false;
    private boolean emailVerified    = false;
    private String  twoFactorSecret;  // TOTP base32 secret (stored hashed)

    /** First-device anchor: client {@code deviceKey} UUID, or {@code model:platform|model} if missing. */
    private String primaryAnchorKey = "";
    /** Snapshot from first session (same model + platform ⇒ same “primary” phone after reinstall). */
    private String primaryDeviceModel = "";
    private String primaryDevicePlatform = "";

    // ── Privacy ───────────────────────────────────────────
    private boolean dataSharing     = false;
    private boolean activityTracking = true;

    private Date updatedAt = new Date();

    // ── Getters And Setters ───────────────────────────────────────────
	
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

	public boolean isDarkMode() {
		return darkMode;
	}

	public void setDarkMode(boolean darkMode) {
		this.darkMode = darkMode;
	}

	public boolean isPushNotifications() {
		return pushNotifications;
	}

	public void setPushNotifications(boolean pushNotifications) {
		this.pushNotifications = pushNotifications;
	}

	public boolean isEmailNotifications() {
		return emailNotifications;
	}

	public void setEmailNotifications(boolean emailNotifications) {
		this.emailNotifications = emailNotifications;
	}

	public boolean isMarketAlerts() {
		return marketAlerts;
	}

	public void setMarketAlerts(boolean marketAlerts) {
		this.marketAlerts = marketAlerts;
	}

	public boolean isPortfolioUpdates() {
		return portfolioUpdates;
	}

	public void setPortfolioUpdates(boolean portfolioUpdates) {
		this.portfolioUpdates = portfolioUpdates;
	}

	public boolean isNewsUpdates() {
		return newsUpdates;
	}

	public void setNewsUpdates(boolean newsUpdates) {
		this.newsUpdates = newsUpdates;
	}

	public boolean isTradingSignals() {
		return tradingSignals;
	}

	public void setTradingSignals(boolean tradingSignals) {
		this.tradingSignals = tradingSignals;
	}

	public boolean isSystemUpdates() {
		return systemUpdates;
	}

	public void setSystemUpdates(boolean systemUpdates) {
		this.systemUpdates = systemUpdates;
	}

	public boolean isMarketingEmails() {
		return marketingEmails;
	}

	public void setMarketingEmails(boolean marketingEmails) {
		this.marketingEmails = marketingEmails;
	}

	public String getExpoPushToken() {
		return expoPushToken;
	}

	public void setExpoPushToken(String expoPushToken) {
		this.expoPushToken = expoPushToken;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public boolean isTwoFactorEnabled() {
		return twoFactorEnabled;
	}

	public void setTwoFactorEnabled(boolean twoFactorEnabled) {
		this.twoFactorEnabled = twoFactorEnabled;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	public String getTwoFactorSecret() {
		return twoFactorSecret;
	}

	public void setTwoFactorSecret(String twoFactorSecret) {
		this.twoFactorSecret = twoFactorSecret;
	}

	public String getPrimaryAnchorKey() {
		return primaryAnchorKey;
	}

	public void setPrimaryAnchorKey(String primaryAnchorKey) {
		this.primaryAnchorKey = primaryAnchorKey != null ? primaryAnchorKey : "";
	}

	public String getPrimaryDeviceModel() {
		return primaryDeviceModel;
	}

	public void setPrimaryDeviceModel(String primaryDeviceModel) {
		this.primaryDeviceModel = primaryDeviceModel != null ? primaryDeviceModel : "";
	}

	public String getPrimaryDevicePlatform() {
		return primaryDevicePlatform;
	}

	public void setPrimaryDevicePlatform(String primaryDevicePlatform) {
		this.primaryDevicePlatform = primaryDevicePlatform != null ? primaryDevicePlatform : "";
	}

	public boolean isDataSharing() {
		return dataSharing;
	}

	public void setDataSharing(boolean dataSharing) {
		this.dataSharing = dataSharing;
	}

	public boolean isActivityTracking() {
		return activityTracking;
	}

	public void setActivityTracking(boolean activityTracking) {
		this.activityTracking = activityTracking;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
    
    
    
   
    
    
}
