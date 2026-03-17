package com.revature.revpay.controller;

import com.revature.revpay.model.User;
import com.revature.revpay.service.NotificationService;
import com.revature.revpay.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping({ "", "/" })
    public String listPage(Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("notifications", notificationService.getNotifications(user.getUserId()));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user.getUserId()));
        return "notifications/index";
    }

    @PostMapping("/{id}/read")
    public String markRead(@PathVariable Long id, RedirectAttributes ra) {
        notificationService.markRead(id);
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead(Authentication auth, RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        notificationService.markAllRead(user.getUserId());
        ra.addFlashAttribute("successMsg", "All notifications marked as read.");
        return "redirect:/notifications";
    }

    @GetMapping("/preferences")
    public String prefsPage(Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("preferences", notificationService.getPreferences(user.getUserId()));
        return "notifications/preferences";
    }

    @PostMapping("/preferences")
    public String updatePrefs(Authentication auth,
            @RequestParam(defaultValue = "false") boolean transaction,
            @RequestParam(defaultValue = "false") boolean moneyRequest,
            @RequestParam(defaultValue = "false") boolean cardChange,
            @RequestParam(defaultValue = "false") boolean lowBalance,
            @RequestParam(defaultValue = "false") boolean loan,
            @RequestParam(defaultValue = "false") boolean invoice,
            RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        Long uid = user.getUserId();
        notificationService.updatePreference(uid, "TRANSACTION", transaction);
        notificationService.updatePreference(uid, "MONEY_REQUEST", moneyRequest);
        notificationService.updatePreference(uid, "CARD_CHANGE", cardChange);
        notificationService.updatePreference(uid, "LOW_BALANCE", lowBalance);
        notificationService.updatePreference(uid, "LOAN", loan);
        notificationService.updatePreference(uid, "INVOICE", invoice);
        ra.addFlashAttribute("successMsg", "Preferences updated.");
        return "redirect:/notifications/preferences";
    }
}
