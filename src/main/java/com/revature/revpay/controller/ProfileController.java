package com.revature.revpay.controller;

import com.revature.revpay.model.User;
import com.revature.revpay.service.UserService;
import com.revature.revpay.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final NotificationService notificationService;

    public ProfileController(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping({ "", "/" })
    public String profilePage(Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        model.addAttribute("user", user);
        if (user.isBusiness()) {
            model.addAttribute("businessProfile", userService.getBusinessProfile(user.getUserId()));
        }
        return "profile/index";
    }

    @PostMapping("/update")
    public String updateProfile(Authentication auth,
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        userService.updateProfile(user.getUserId(), fullName, phone);
        ra.addFlashAttribute("successMsg", "Profile updated.");
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(Authentication auth,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmNewPassword,
            RedirectAttributes ra) {
        if (!newPassword.equals(confirmNewPassword)) {
            ra.addFlashAttribute("errorMsg", "New passwords do not match.");
            return "redirect:/profile";
        }
        User user = userService.findByEmailOrPhone(auth.getName());
        if (userService.changePassword(user.getUserId(), currentPassword, newPassword)) {
            ra.addFlashAttribute("successMsg", "Password changed successfully.");
        } else {
            ra.addFlashAttribute("errorMsg", "Current password is incorrect.");
        }
        return "redirect:/profile";
    }



    @PostMapping("/business-profile")
    public String updateBusinessProfile(Authentication auth,
            @RequestParam String businessName,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String taxId,
            @RequestParam(required = false) String gstNumber,
            @RequestParam(required = false) String address,
            RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        if (user.isBusiness()) {
            userService.updateBusinessProfile(user.getUserId(), businessName, businessType, taxId, gstNumber, address);
            
            // Notify admins of the update/verification request
            java.util.List<User> admins = userService.findByRole("ADMIN");
            for (User admin : admins) {
                notificationService.send(admin.getUserId(), "Business Profile Verification", 
                    "User " + user.getFullName() + " has updated their business profile details. Review in the Admin Dashboard.", "SYSTEM");
            }
            
            ra.addFlashAttribute("successMsg", "Business profile updated.");
        }
        return "redirect:/profile";
    }
}
