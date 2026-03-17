package com.revature.revpay.controller;

import com.revature.revpay.dao.BusinessProfileDAO;
import com.revature.revpay.dao.InvoiceDAO;
import com.revature.revpay.dao.LoanApplicationDAO;
import com.revature.revpay.dao.MoneyRequestDAO;
import com.revature.revpay.dao.NotificationDAO;
import com.revature.revpay.model.User;
import com.revature.revpay.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserService userService;
    private final NotificationDAO notificationDAO;
    private final MoneyRequestDAO moneyRequestDAO;
    private final InvoiceDAO invoiceDAO;
    private final LoanApplicationDAO loanApplicationDAO;
    private final BusinessProfileDAO businessProfileDAO;

    public GlobalControllerAdvice(UserService userService, NotificationDAO notificationDAO,
            MoneyRequestDAO moneyRequestDAO, InvoiceDAO invoiceDAO,
            LoanApplicationDAO loanApplicationDAO, BusinessProfileDAO businessProfileDAO) {
        this.userService = userService;
        this.notificationDAO = notificationDAO;
        this.moneyRequestDAO = moneyRequestDAO;
        this.invoiceDAO = invoiceDAO;
        this.loanApplicationDAO = loanApplicationDAO;
        this.businessProfileDAO = businessProfileDAO;
    }

    @ModelAttribute
    public void addGlobalAttributes(Authentication auth, HttpServletRequest request,
            org.springframework.ui.Model model) {
        try {
            if (auth != null && auth.isAuthenticated()) {
                User user = userService.findByEmailOrPhone(auth.getName());
                if (user != null) {
                    model.addAttribute("user", user);
                    model.addAttribute("requestURI", request.getRequestURI());

                    // Notifications unread count
                    model.addAttribute("unreadCount", notificationDAO.countUnread(user.getUserId()));

                    // Money requests incoming count
                    model.addAttribute("incomingCount", moneyRequestDAO.countIncomingPending(user.getUserId()));

                    if ("PERSONAL".equals(user.getAccountType())) {
                        model.addAttribute("pendingInvoicesCount", invoiceDAO.countPendingForCustomer(user.getEmail()));
                    } else if ("BUSINESS".equals(user.getAccountType())) {
                        model.addAttribute("pendingLoansCount",
                                loanApplicationDAO.countPendingForBusiness(user.getUserId()));
                    }

                    if ("ADMIN".equals(user.getRole())) {
                        model.addAttribute("pendingGstCount", businessProfileDAO.countPendingApprovals());
                        model.addAttribute("pendingLoansCount", loanApplicationDAO.countPendingForAdmin());
                    }
                }
            }
        } catch (Exception e) {
            // Log but don't crash - let the page render with defaults
            org.apache.logging.log4j.LogManager.getLogger(GlobalControllerAdvice.class)
                    .error("Error in GlobalControllerAdvice: {}", e.getMessage(), e);
        }
    }
}
