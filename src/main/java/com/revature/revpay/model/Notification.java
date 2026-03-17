package com.revature.revpay.model;

import java.time.LocalDateTime;

public class Notification {
    private Long notifId;
    private Long userId;
    private String title;
    private String message;
    private String notifType; // TRANSACTION, MONEY_REQUEST, CARD_CHANGE, LOW_BALANCE, LOAN, INVOICE, GENERAL
    private boolean isRead;
    private LocalDateTime createdAt;

    public Notification() {
    }

    public Notification(Long userId, String title, String message, String notifType) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.notifType = notifType;
        this.isRead = false;
    }

    // Getters and Setters
    public Long getNotifId() {
        return notifId;
    }

    public void setNotifId(Long notifId) {
        this.notifId = notifId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNotifType() {
        return notifType;
    }

    public void setNotifType(String notifType) {
        this.notifType = notifType;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
