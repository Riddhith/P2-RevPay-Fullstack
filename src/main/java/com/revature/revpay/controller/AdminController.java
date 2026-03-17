package com.revature.revpay.controller;

import com.revature.revpay.dao.BusinessProfileDAO;
import com.revature.revpay.dao.LoanApplicationDAO;
import com.revature.revpay.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.revature.revpay.service.UserService;
import com.revature.revpay.service.NotificationService;
import com.revature.revpay.model.BusinessProfile;
import com.revature.revpay.model.LoanApplication;

import java.util.Random;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BusinessProfileDAO businessProfileDAO;
    private final LoanApplicationDAO loanApplicationDAO;
    private final UserService userService;
    private final NotificationService notificationService;
    private final com.revature.revpay.dao.UserDAO userDAO;

    public AdminController(BusinessProfileDAO businessProfileDAO, LoanApplicationDAO loanApplicationDAO, 
                           UserService userService, NotificationService notificationService,
                           com.revature.revpay.dao.UserDAO userDAO) {
        this.businessProfileDAO = businessProfileDAO;
        this.loanApplicationDAO = loanApplicationDAO;
        this.userService = userService;
        this.notificationService = notificationService;
        this.userDAO = userDAO;
    }

    private boolean requireAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        User user = userService.findByEmailOrPhone(auth.getName());
        return user != null && user.isAdmin();
    }

    private String generateRandomGst() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("22"); // GST prefix
        Random rnd = new Random();
        for (int i = 0; i < 13; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @GetMapping("/dashboard")
    public String adminRedirect() {
        return "redirect:/admin/gst";
    }

    @GetMapping("/gst")
    public String adminGst(Model model, Authentication auth) {
        if (!requireAdmin(auth)) {
            return "redirect:/auth/login";
        }
        
        User user = userService.findByEmailOrPhone(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("pendingBusinesses", businessProfileDAO.findAllPending());
        return "admin/gst";
    }

    @GetMapping("/loans")
    public String adminLoans(Model model, Authentication auth) {
        if (!requireAdmin(auth)) {
            return "redirect:/auth/login";
        }
        
        User user = userService.findByEmailOrPhone(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("pendingLoans", loanApplicationDAO.findAllPending());
        return "admin/loans";
    }

    @PostMapping("/approve-business/{id}")
    public String approveBusiness(@PathVariable Long id, @RequestParam String action, Authentication auth) {
        if (!requireAdmin(auth)) return "redirect:/auth/login";
        
        String status = "approve".equalsIgnoreCase(action) ? "APPROVED" : "REJECTED";
        if ("APPROVED".equals(status)) {
            businessProfileDAO.updateGstStatus(id, status, generateRandomGst());
        } else {
            businessProfileDAO.updateStatus(id, status);
        }
        
        BusinessProfile bp = businessProfileDAO.findById(id);
        if (bp != null) {
            String title = "GST Verification " + ("APPROVED".equals(status) ? "Approved" : "Rejected");
            String message = "Your business profile/GST registration for " + bp.getBusinessName() + " has been " + status.toLowerCase() + " by the admin.";
            notificationService.send(bp.getUserId(), title, message, "SYSTEM");
        }
        
        return "redirect:/admin/gst";
    }

    @PostMapping("/approve-loan/{id}")
    public String approveLoan(@PathVariable Long id, @RequestParam String action, Authentication auth) {
        if (!requireAdmin(auth)) return "redirect:/auth/login";
        
        String status = "approve".equalsIgnoreCase(action) ? "APPROVED" : "REJECTED";
        loanApplicationDAO.updateStatus(id, status);
        
        LoanApplication loan = loanApplicationDAO.findById(id);
        if (loan != null) {
            if ("APPROVED".equals(status)) {
                User applicant = userDAO.findById(loan.getUserId());
                if (applicant != null) {
                    java.math.BigDecimal newBalance = applicant.getWalletBalance().add(loan.getLoanAmount());
                    userDAO.updateBalance(applicant.getUserId(), newBalance);
                }
            }
            
            String title = "Loan Application " + ("APPROVED".equals(status) ? "Approved" : "Rejected");
            String message = "Your loan application for ₹" + loan.getLoanAmount() + " (" + loan.getPurpose() + ") has been " + status.toLowerCase() + ".";
            notificationService.send(loan.getUserId(), title, message, "LOAN");
        }

        return "redirect:/admin/loans";
    }
    @GetMapping("/history")
    public String adminHistory(Model model, Authentication auth) {
        if (!requireAdmin(auth)) {
            return "redirect:/auth/login";
        }
        
        User user = userService.findByEmailOrPhone(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("approvedBusinesses", businessProfileDAO.findAllApproved());
        model.addAttribute("approvedLoans", loanApplicationDAO.findAllApproved());
        return "admin/history";
    }
}
