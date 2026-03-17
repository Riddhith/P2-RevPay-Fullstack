package com.revature.revpay.model;

import java.time.LocalDateTime;

public class PaymentMethod {
    private Long methodId;
    private Long userId;
    private String cardType; // CREDIT, DEBIT, BANK_ACCOUNT
    private String cardNumberEnc; // encrypted full card number (not displayed)
    private String lastFour;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cardholderName;
    private String billingAddress;
    private String pinHash;
    private boolean isDefault;
    private LocalDateTime createdAt;
    private java.math.BigDecimal balance;

    public PaymentMethod() {
    }

    // Getters and Setters
    public Long getMethodId() {
        return methodId;
    }

    public void setMethodId(Long methodId) {
        this.methodId = methodId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getCardNumberEnc() {
        return cardNumberEnc;
    }

    public void setCardNumberEnc(String cardNumberEnc) {
        this.cardNumberEnc = cardNumberEnc;
    }

    public String getLastFour() {
        return lastFour;
    }

    public void setLastFour(String lastFour) {
        this.lastFour = lastFour;
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(Integer expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(Integer expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.math.BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(java.math.BigDecimal balance) {
        this.balance = balance;
    }

    public String getMaskedNumber() {
        return "**** **** **** " + lastFour;
    }

    public String getExpiryFormatted() {
        if (expiryMonth == null || expiryYear == null)
            return "";
        return String.format("%02d/%04d", expiryMonth, expiryYear);
    }
}
