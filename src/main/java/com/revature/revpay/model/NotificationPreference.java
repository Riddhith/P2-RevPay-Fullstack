package com.revature.revpay.model;

public class NotificationPreference {
    private Long prefId;
    private Long userId;
    private String notifType;
    private boolean isEnabled;

    public NotificationPreference() {
    }

    public NotificationPreference(Long userId, String notifType, boolean isEnabled) {
        this.userId = userId;
        this.notifType = notifType;
        this.isEnabled = isEnabled;
    }

    public Long getPrefId() {
        return prefId;
    }

    public void setPrefId(Long prefId) {
        this.prefId = prefId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNotifType() {
        return notifType;
    }

    public void setNotifType(String notifType) {
        this.notifType = notifType;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
