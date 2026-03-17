package com.revature.revpay.controller;

import com.revature.revpay.model.User;
import com.revature.revpay.service.NotificationService;
import com.revature.revpay.service.TransactionService;
import com.revature.revpay.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final UserService userService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;

    public DashboardController(UserService userService, TransactionService transactionService,
            NotificationService notificationService) {
        this.userService = userService;
        this.transactionService = transactionService;
        this.notificationService = notificationService;
    }

    @GetMapping({ "", "/" })
    public String dashboard(Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        if (user == null)
            return "redirect:/auth/login";

        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("user", user);
        model.addAttribute("recentTransactions", transactionService.getRecent(user.getUserId(), 5));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user.getUserId()));
        model.addAttribute("totalSent", transactionService.getTotalSent(user.getUserId()));
        model.addAttribute("totalReceived", transactionService.getTotalReceived(user.getUserId()));

        if (user.isBusiness())
            return "dashboard/business";
        return "dashboard/personal";
    }
}
