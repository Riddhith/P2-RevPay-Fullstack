package com.revature.revpay.service;

import com.revature.revpay.dao.PaymentMethodDAO;
import com.revature.revpay.model.PaymentMethod;
import com.revature.revpay.util.PasswordUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentMethodService {

    private static final Logger log = LogManager.getLogger(PaymentMethodService.class);
    private final PaymentMethodDAO paymentMethodDAO;
    private final NotificationService notificationService;

    public PaymentMethodService(PaymentMethodDAO paymentMethodDAO, NotificationService notificationService) {
        this.paymentMethodDAO = paymentMethodDAO;
        this.notificationService = notificationService;
    }

    public Long addCard(Long userId, String cardType, String cardNumber, String cardholderName,
            int expiryMonth, int expiryYear, String billingAddress, boolean makeDefault) {
        // Simple obfuscation for storage (not real encryption - use AES in production)
        String lastFour = cardNumber.replaceAll("[^0-9]", "");
        lastFour = lastFour.length() >= 4 ? lastFour.substring(lastFour.length() - 4) : lastFour;
        String maskedStored = "****-****-****-" + lastFour; // store masked, not real card number

        if (makeDefault) {
            paymentMethodDAO.findByUserId(userId).forEach(pm -> {
                if (pm.isDefault())
                    paymentMethodDAO.setDefault(-1L, userId); // clear defaults
            });
        }

        PaymentMethod pm = new PaymentMethod();
        pm.setUserId(userId);
        pm.setCardType(cardType);
        pm.setCardNumberEnc(maskedStored);
        pm.setLastFour(lastFour);
        pm.setExpiryMonth(expiryMonth);
        pm.setExpiryYear(expiryYear);
        pm.setCardholderName(cardholderName);
        pm.setBillingAddress(billingAddress);
        pm.setDefault(makeDefault);

        Long id = paymentMethodDAO.save(pm);
        if (id != null) {
            notificationService.send(userId, "Card Added",
                    "Card ending in " + lastFour + " has been added to your account.", "CARD_CHANGE");
        }
        return id;
    }

    public List<PaymentMethod> getByUser(Long userId) {
        return paymentMethodDAO.findByUserId(userId);
    }

    public PaymentMethod getById(Long methodId) {
        return paymentMethodDAO.findById(methodId);
    }

    public void delete(Long methodId, Long userId) {
        PaymentMethod pm = paymentMethodDAO.findById(methodId);
        if (pm != null && pm.getUserId().equals(userId)) {
            paymentMethodDAO.delete(methodId);
            notificationService.send(userId, "Card Removed",
                    "Card ending in " + pm.getLastFour() + " has been removed.", "CARD_CHANGE");
        }
    }

    public void setDefault(Long methodId, Long userId) {
        paymentMethodDAO.setDefault(methodId, userId);
    }

    public void update(PaymentMethod pm) {
        paymentMethodDAO.update(pm);
    }

    public boolean setPin(Long methodId, String pin, Long userId) {
        PaymentMethod pm = paymentMethodDAO.findById(methodId);
        if (pm == null || !pm.getUserId().equals(userId))
            return false;
        paymentMethodDAO.updatePin(methodId, PasswordUtil.hash(pin));
        return true;
    }

    public boolean verifyPin(Long methodId, String pin, Long userId) {
        PaymentMethod pm = paymentMethodDAO.findById(methodId);
        if (pm == null || !pm.getUserId().equals(userId) || pm.getPinHash() == null)
            return false;
        return PasswordUtil.matches(pin, pm.getPinHash());
    }
}
