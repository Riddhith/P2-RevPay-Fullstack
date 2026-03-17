package com.revature.revpay.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private Long txnId;
    private Long senderId;
    private Long receiverId;
    private String senderName; // populated via JOIN
    private String receiverName; // populated via JOIN
    private BigDecimal amount;
    private String txnType; // SEND, RECEIVE, ADD_FUNDS, WITHDRAWAL, PAYMENT, LOAN_REPAYMENT
    private String status; // PENDING, COMPLETED, FAILED, CANCELLED
    private String note;
    private String referenceId;
    private LocalDateTime txnTimestamp;

    public Transaction() {
    }

    // Getters and Setters
    public Long getTxnId() {
        return txnId;
    }

    public void setTxnId(Long txnId) {
        this.txnId = txnId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDateTime getTxnTimestamp() {
        return txnTimestamp;
    }

    public void setTxnTimestamp(LocalDateTime txnTimestamp) {
        this.txnTimestamp = txnTimestamp;
    }

    public boolean isDebit(Long currentUserId) {
        return "SEND".equals(txnType) || "WITHDRAWAL".equals(txnType) || "LOAN_REPAYMENT".equals(txnType)
                || ("PAYMENT".equals(txnType) && currentUserId.equals(senderId));
    }

    public boolean isCredit(Long currentUserId) {
        return "ADD_FUNDS".equals(txnType) || "RECEIVE".equals(txnType)
                || ("PAYMENT".equals(txnType) && currentUserId.equals(receiverId));
    }
}
