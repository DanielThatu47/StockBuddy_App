// src/main/java/com/stockbuddy/model/UserSession.java
package com.stockbuddy.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Stores one record per device login session.
 * Created when user logs in, updated on every API call, deleted on revoke.
 */
@Data
@NoArgsConstructor
@Document(collection = "user_sessions")
public class UserSession {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String deviceName;    // e.g. "iPhone 15 Pro"
    private String deviceModel;   // e.g. "iPhone15,4"
    private String platform;      // "ios" | "android"
    private String osVersion;     // e.g. "17.2"
    private String appVersion;    // e.g. "1.0.0"
    private String ipAddress;

    /** Stable id from the client (per app install); used with account primary-device anchor. */
    private String deviceKey = "";

    private boolean isActive = true;
    private boolean isCurrent = false;  // flag set by the registering request

    /** First device to register a session for this account; can revoke other devices. */
    private boolean primary = false;

    private Date loginTime   = new Date();
    private Date lastActive  = new Date();
    private Date revokedAt;
   
  
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
	public String getDeviceName() {
		return deviceName;
	}
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	public String getDeviceModel() {
		return deviceModel;
	}
	public void setDeviceModel(String deviceModel) {
		this.deviceModel = deviceModel;
	}
	public String getPlatform() {
		return platform;
	}
	public void setPlatform(String platform) {
		this.platform = platform;
	}
	public String getOsVersion() {
		return osVersion;
	}
	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}
	public String getAppVersion() {
		return appVersion;
	}
	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public String getDeviceKey() {
		return deviceKey;
	}
	public void setDeviceKey(String deviceKey) {
		this.deviceKey = deviceKey != null ? deviceKey : "";
	}
	public boolean isActive() {
		return isActive;
	}
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
	public boolean isCurrent() {
		return isCurrent;
	}
	public void setCurrent(boolean isCurrent) {
		this.isCurrent = isCurrent;
	}
	public boolean isPrimary() {
		return primary;
	}
	public void setPrimary(boolean primary) {
		this.primary = primary;
	}
	public Date getLoginTime() {
		return loginTime;
	}
	public void setLoginTime(Date loginTime) {
		this.loginTime = loginTime;
	}
	public Date getLastActive() {
		return lastActive;
	}
	public void setLastActive(Date lastActive) {
		this.lastActive = lastActive;
	}
	public Date getRevokedAt() {
		return revokedAt;
	}
	public void setRevokedAt(Date revokedAt) {
		this.revokedAt = revokedAt;
	}
    
    

    
}
