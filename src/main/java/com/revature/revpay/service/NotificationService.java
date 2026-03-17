package com.revature.revpay.service;

import com.revature.revpay.dao.NotificationDAO;
import com.revature.revpay.model.Notification;
import com.revature.revpay.model.NotificationPreference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LogManager.getLogger(NotificationService.class);
    private final NotificationDAO notificationDAO;

    public NotificationService(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
    }

    public void send(Long userId, String title, String message, String notifType) {
        if (notificationDAO.isPreferenceEnabled(userId, notifType)) {
            notificationDAO.save(new Notification(userId, title, message, notifType));
        }
    }

    public void sendTransactionNotification(Long userId, String title, String message) {
        send(userId, title, message, "TRANSACTION");
    }

    public void sendMoneyRequestNotification(Long userId, String title, String message) {
        send(userId, title, message, "MONEY_REQUEST");
    }

    public void sendLoanNotification(Long userId, String title, String message) {
        send(userId, title, message, "LOAN");
    }

    public void sendInvoiceNotification(Long userId, String title, String message) {
        send(userId, title, message, "INVOICE");
    }

    public List<Notification> getNotifications(Long userId) {
        return notificationDAO.findByUserId(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationDAO.countUnread(userId);
    }

    public void markRead(Long notifId) {
        notificationDAO.markRead(notifId);
    }

    public void markAllRead(Long userId) {
        notificationDAO.markAllRead(userId);
    }

    public List<NotificationPreference> getPreferences(Long userId) {
        return notificationDAO.findPreferencesByUserId(userId);
    }

    public void updatePreference(Long userId, String notifType, boolean enabled) {
        notificationDAO.savePreference(new NotificationPreference(userId, notifType, enabled));
    }
}
