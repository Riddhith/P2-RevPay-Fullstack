package com.revature.revpay.service;

import com.revature.revpay.dao.MoneyRequestDAO;
import com.revature.revpay.dao.UserDAO;
import com.revature.revpay.model.MoneyRequest;
import com.revature.revpay.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MoneyRequestService {

    private static final Logger log = LogManager.getLogger(MoneyRequestService.class);

    private final MoneyRequestDAO moneyRequestDAO;
    private final UserDAO userDAO;
    private final TransactionService transactionService;
    private final NotificationService notificationService;

    public MoneyRequestService(MoneyRequestDAO moneyRequestDAO, UserDAO userDAO,
            TransactionService transactionService, NotificationService notificationService) {
        this.moneyRequestDAO = moneyRequestDAO;
        this.userDAO = userDAO;
        this.transactionService = transactionService;
        this.notificationService = notificationService;
    }

    public String sendRequest(Long requesterId, String requesteeIdentifier, BigDecimal amount, String purpose) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return "INVALID_AMOUNT";

        User requester = userDAO.findById(requesterId);
        User requestee = userDAO.findByUsernameOrEmailOrPhone(requesteeIdentifier);
        if (requestee == null)
            return "USER_NOT_FOUND";
        if (requesterId.equals(requestee.getUserId()))
            return "CANNOT_REQUEST_SELF";

        MoneyRequest mr = new MoneyRequest();
        mr.setRequesterId(requesterId);
        mr.setRequesteeId(requestee.getUserId());
        mr.setAmount(amount);
        mr.setPurpose(purpose);
        moneyRequestDAO.save(mr);

        notificationService.sendMoneyRequestNotification(requestee.getUserId(),
                "Money Request",
                requester.getFullName() + " is requesting ₹" + amount + (purpose != null ? " for: " + purpose : ""));

        return "SUCCESS";
    }

    public String acceptRequest(Long requestId, Long requesteeId) {
        MoneyRequest mr = moneyRequestDAO.findById(requestId);
        if (mr == null || !mr.getRequesteeId().equals(requesteeId))
            return "NOT_FOUND";
        if (!"PENDING".equals(mr.getStatus()))
            return "ALREADY_PROCESSED";

        // Use sendMoney from requester's perspective — but here requestee pays
        // requester
        String result = transactionService.sendMoney(requesteeId,
                userDAO.findById(mr.getRequesterId()).getEmail(), mr.getAmount(), "Payment for request #" + requestId);

        if ("SUCCESS".equals(result)) {
            moneyRequestDAO.updateStatus(requestId, "ACCEPTED");
            notificationService.sendMoneyRequestNotification(mr.getRequesterId(),
                    "Request Accepted", "Your money request of ₹" + mr.getAmount() + " was accepted.");
        }
        return result;
    }

    public void declineRequest(Long requestId, Long requesteeId) {
        MoneyRequest mr = moneyRequestDAO.findById(requestId);
        if (mr != null && mr.getRequesteeId().equals(requesteeId)) {
            moneyRequestDAO.updateStatus(requestId, "DECLINED");
            notificationService.sendMoneyRequestNotification(mr.getRequesterId(),
                    "Request Declined", "Your money request of ₹" + mr.getAmount() + " was declined.");
        }
    }

    public void cancelRequest(Long requestId, Long requesterId) {
        MoneyRequest mr = moneyRequestDAO.findById(requestId);
        if (mr != null && mr.getRequesterId().equals(requesterId) && "PENDING".equals(mr.getStatus())) {
            moneyRequestDAO.updateStatus(requestId, "CANCELLED");
        }
    }

    public List<MoneyRequest> getIncoming(Long userId) {
        return moneyRequestDAO.findIncoming(userId);
    }

    public List<MoneyRequest> getOutgoing(Long userId) {
        return moneyRequestDAO.findOutgoing(userId);
    }
}
