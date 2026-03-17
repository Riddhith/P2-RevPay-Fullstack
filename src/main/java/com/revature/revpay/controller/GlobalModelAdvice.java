package com.revature.revpay.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final com.revature.revpay.service.UserService userService;
    private final com.revature.revpay.service.NotificationService notificationService;

    public GlobalModelAdvice(com.revature.revpay.service.UserService userService, com.revature.revpay.service.NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @ModelAttribute("requestURI")
    public String getRequestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("unreadCount")
    public Long populateUnreadCount(org.springframework.security.core.Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            com.revature.revpay.model.User user = userService.findByEmailOrPhone(auth.getName());
            if (user != null) {
                return notificationService.getUnreadCount(user.getUserId());
            }
        }
        return 0L;
    }
}
