package com.revature.revpay.service;

import com.revature.revpay.dao.TransactionDAO;
import com.revature.revpay.dao.UserDAO;
import com.revature.revpay.model.Transaction;
import com.revature.revpay.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private static final Logger log = LogManager.getLogger(TransactionService.class);

    private final TransactionDAO transactionDAO;
    private final UserDAO userDAO;
    private final com.revature.revpay.dao.PaymentMethodDAO paymentMethodDAO;
    private final NotificationService notificationService;

    public TransactionService(TransactionDAO transactionDAO, UserDAO userDAO,
            com.revature.revpay.dao.PaymentMethodDAO paymentMethodDAO, NotificationService notificationService) {
        this.transactionDAO = transactionDAO;
        this.userDAO = userDAO;
        this.paymentMethodDAO = paymentMethodDAO;
        this.notificationService = notificationService;
    }

    /**
     * Send money from sender to receiver
     */
    public String sendMoney(Long senderId, String recipientIdentifier, BigDecimal amount, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return "INVALID_AMOUNT";

        User sender = userDAO.findById(senderId);
        if (sender == null)
            return "SENDER_NOT_FOUND";

        User receiver = userDAO.findByUsernameOrEmailOrPhone(recipientIdentifier);
        if (receiver == null)
            return "RECEIVER_NOT_FOUND";

        if (sender.getUserId().equals(receiver.getUserId()))
            return "CANNOT_SEND_SELF";

        if (sender.getWalletBalance().compareTo(amount) < 0)
            return "INSUFFICIENT_FUNDS";

        // Deduct from sender
        userDAO.updateBalance(senderId, sender.getWalletBalance().subtract(amount));
        // Credit to receiver
        userDAO.updateBalance(receiver.getUserId(), receiver.getWalletBalance().add(amount));

        // Record transaction
        Transaction txn = new Transaction();
        txn.setSenderId(senderId);
        txn.setReceiverId(receiver.getUserId());
        txn.setAmount(amount);
        txn.setTxnType("SEND");
        txn.setStatus("COMPLETED");
        txn.setNote(note);
        transactionDAO.save(txn);

        // Notifications
        notificationService.sendTransactionNotification(senderId,
                "Money Sent", "You sent ₹" + amount + " to " + receiver.getFullName());
        notificationService.sendTransactionNotification(receiver.getUserId(),
                "Money Received", "You received ₹" + amount + " from " + sender.getFullName());

        log.info("Transfer: {} -> {} amount={}", senderId, receiver.getUserId(), amount);
        return "SUCCESS";
    }

    /**
     * Process an invoice payment
     */
    public String payInvoiceTransaction(Long senderId, String recipientIdentifier, BigDecimal amount, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return "INVALID_AMOUNT";

        User sender = userDAO.findById(senderId);
        if (sender == null)
            return "SENDER_NOT_FOUND";

        User receiver = userDAO.findByUsernameOrEmailOrPhone(recipientIdentifier);
        if (receiver == null)
            return "RECEIVER_NOT_FOUND";

        if (sender.getUserId().equals(receiver.getUserId()))
            return "CANNOT_SEND_SELF";

        if (sender.getWalletBalance().compareTo(amount) < 0)
            return "INSUFFICIENT_FUNDS";

        // Deduct from sender
        userDAO.updateBalance(senderId, sender.getWalletBalance().subtract(amount));
        // Credit to receiver
        userDAO.updateBalance(receiver.getUserId(), receiver.getWalletBalance().add(amount));

        // Record transaction as PAYMENT
        Transaction txn = new Transaction();
        txn.setSenderId(senderId);
        txn.setReceiverId(receiver.getUserId());
        txn.setAmount(amount);
        txn.setTxnType("PAYMENT");
        txn.setStatus("COMPLETED");
        txn.setNote(note);
        transactionDAO.save(txn);

        // Notifications
        notificationService.sendTransactionNotification(senderId,
                "Invoice Paid", "You paid ₹" + amount + " to " + receiver.getFullName());
        notificationService.sendTransactionNotification(receiver.getUserId(),
                "Invoice Payment Received",
                "You received ₹" + amount + " from " + sender.getFullName() + " for an invoice.");

        log.info("Invoice Payment: {} -> {} amount={}", senderId, receiver.getUserId(), amount);
        return "SUCCESS";
    }

    /**
     * Add funds to wallet from a payment method
     */
    public String addFunds(Long userId, Long methodId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return "INVALID_AMOUNT";
        User user = userDAO.findById(userId);
        if (user == null)
            return "USER_NOT_FOUND";

        com.revature.revpay.model.PaymentMethod card = paymentMethodDAO.findById(methodId);
        if (card == null || !card.getUserId().equals(userId))
            return "CARD_NOT_FOUND";

        if (card.getBalance().compareTo(amount) < 0)
            return "INSUFFICIENT_CARD_FUNDS";

        // Deduct from card, add to wallet
        paymentMethodDAO.updateBalance(methodId, card.getBalance().subtract(amount));
        userDAO.updateBalance(userId, user.getWalletBalance().add(amount));

        Transaction txn = new Transaction();
        txn.setReceiverId(userId);
        txn.setAmount(amount);
        txn.setTxnType("ADD_FUNDS");
        txn.setStatus("COMPLETED");
        txn.setNote("Added funds from card " + card.getLastFour());
        transactionDAO.save(txn);

        notificationService.sendTransactionNotification(userId,
                "Funds Added", "₹" + amount + " added to your wallet from ending in " + card.getLastFour());
        return "SUCCESS";
    }

    /**
     * Withdraw from wallet to bank
     */
    public String withdraw(Long userId, Long methodId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return "INVALID_AMOUNT";
        User user = userDAO.findById(userId);
        if (user == null)
            return "USER_NOT_FOUND";

        com.revature.revpay.model.PaymentMethod card = paymentMethodDAO.findById(methodId);
        if (card == null || !card.getUserId().equals(userId))
            return "CARD_NOT_FOUND";

        if (user.getWalletBalance().compareTo(amount) < 0)
            return "INSUFFICIENT_FUNDS";

        // Deduct from wallet, add to card
        userDAO.updateBalance(userId, user.getWalletBalance().subtract(amount));
        paymentMethodDAO.updateBalance(methodId, card.getBalance().add(amount));

        Transaction txn = new Transaction();
        txn.setSenderId(userId);
        txn.setAmount(amount);
        txn.setTxnType("WITHDRAWAL");
        txn.setStatus("COMPLETED");
        txn.setNote("Withdrawal to card " + card.getLastFour());
        transactionDAO.save(txn);

        notificationService.sendTransactionNotification(userId,
                "Withdrawal Processed", "₹" + amount + " withdrawn to card ending in " + card.getLastFour());

        return "SUCCESS";
    }

    public List<Transaction> getHistory(Long userId, String type, LocalDate from, LocalDate to,
            BigDecimal minAmt, BigDecimal maxAmt, String search, int page) {
        return transactionDAO.findByUserId(userId, type, from, to, minAmt, maxAmt, search, page, 20);
    }

    public List<Transaction> getRecent(Long userId, int limit) {
        return transactionDAO.findRecentByUserId(userId, limit);
    }

    public List<Transaction> getAllForExport(Long userId) {
        return transactionDAO.findAllByUserId(userId);
    }

    public BigDecimal getTotalSent(Long userId) {
        return transactionDAO.getTotalByType(userId, "SEND");
    }

    public BigDecimal getTotalReceived(Long userId) {
        return transactionDAO.getTotalReceived(userId);
    }
}
