package com.revature.revpay.controller;

import com.revature.revpay.model.Invoice;
import com.revature.revpay.model.User;
import com.revature.revpay.service.BusinessService;
import com.revature.revpay.service.UserService;
import com.revature.revpay.service.TransactionService;
import com.revature.revpay.dao.InvoiceDAO;
import com.revature.revpay.dao.BusinessProfileDAO;
import com.revature.revpay.model.BusinessProfile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/personal/invoices")
public class PersonalInvoiceController {
    
    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(PersonalInvoiceController.class);

    private final UserService userService;
    private final BusinessService businessService;
    private final InvoiceDAO invoiceDAO;
    private final TransactionService transactionService;
    private final BusinessProfileDAO businessProfileDAO;
    private final com.revature.revpay.service.PaymentMethodService paymentMethodService;

    public PersonalInvoiceController(UserService userService, BusinessService businessService, InvoiceDAO invoiceDAO, TransactionService transactionService, BusinessProfileDAO businessProfileDAO, com.revature.revpay.service.PaymentMethodService paymentMethodService) {
        this.userService = userService;
        this.businessService = businessService;
        this.invoiceDAO = invoiceDAO;
        this.transactionService = transactionService;
        this.businessProfileDAO = businessProfileDAO;
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping
    public String myInvoices(Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        if (!"PERSONAL".equals(user.getAccountType())) {
            return "redirect:/dashboard";
        }
        
        List<Invoice> myInvoices = invoiceDAO.findByCustomerEmail(user.getEmail());
        
        model.addAttribute("user", user);
        model.addAttribute("invoices", myInvoices);
        return "personal/invoices";
    }

    @GetMapping("/{id}")
    public String viewInvoice(@PathVariable Long id, Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        Invoice inv = businessService.getInvoice(id);
        
        if (inv == null || !user.getEmail().equalsIgnoreCase(inv.getCustomerEmail())) {
            return "redirect:/personal/invoices";
        }
        
        BusinessProfile bp = businessProfileDAO.findByUserId(inv.getBusinessUserId());
        
        model.addAttribute("user", user);
        model.addAttribute("invoice", inv);
        model.addAttribute("businessProfile", bp);
        model.addAttribute("paymentMethods", paymentMethodService.getByUser(user.getUserId()));
        return "personal/invoice-detail";
    }

    @PostMapping("/{id}/pay")
    public String payInvoice(@PathVariable Long id, 
                             @RequestParam(required = false) Long methodId,
                             @RequestParam(required = false) String pin,
                             Authentication auth, RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        Invoice inv = businessService.getInvoice(id);
        
        if (inv == null || !user.getEmail().equalsIgnoreCase(inv.getCustomerEmail())) {
            ra.addFlashAttribute("errorMsg", "Invoice not found or unauthorized.");
            return "redirect:/personal/invoices";
        }
        
        try {
            log.info("Starting payment for invoice ID: {} by user: {}", id, user.getUserId());
            
            if ("PAID".equals(inv.getStatus()) || "CANCELLED".equals(inv.getStatus())) {
                log.warn("Invoice {} is already {} and cannot be paid.", id, inv.getStatus());
                ra.addFlashAttribute("errorMsg", "Invoice cannot be paid.");
                return "redirect:/personal/invoices/" + id;
            }

            if (paymentMethodService.getByUser(user.getUserId()).isEmpty()) {
                log.warn("User {} tried to pay invoice {} without any payment methods.", user.getUserId(), id);
                ra.addFlashAttribute("errorMsg", "You must have at least one payment card added to make payments.");
                return "redirect:/payment-methods";
            }

            if (methodId == null) {
                ra.addFlashAttribute("errorMsg", "Please select a card to authenticate the payment.");
                return "redirect:/personal/invoices/" + id;
            }

            if (!paymentMethodService.verifyPin(methodId, pin, user.getUserId())) {
                log.warn("Invalid PIN provided for card {} by user {} while paying invoice {}.", methodId, user.getUserId(), id);
                ra.addFlashAttribute("errorMsg", "Invalid Card PIN. Authentication failed.");
                return "redirect:/personal/invoices/" + id;
            }

            // Send money from personal user to business user
            User businessUser = userService.findById(inv.getBusinessUserId());
            if (businessUser == null) {
                log.error("Business user not found for ID: {} associated with invoice: {}", inv.getBusinessUserId(), id);
                ra.addFlashAttribute("errorMsg", "Business account associated with this invoice was not found.");
                return "redirect:/personal/invoices/" + id;
            }

            log.info("Processing transaction for invoice {}: Sender={}, Receiver={}", id, user.getEmail(), businessUser.getEmail());
            String paymentResult = transactionService.payInvoiceTransaction(user.getUserId(), businessUser.getEmail(), inv.getTotalAmount(), "Payment for Invoice #" + inv.getInvoiceNumber());
            
            if ("SUCCESS".equals(paymentResult)) {
                log.info("Transaction SUCCESS for invoice {}. Now marking as PAID in business service.", id);
                try {
                    businessService.markInvoicePaid(id, businessUser.getUserId());
                    log.info("Invoice {} marked as PAID successfully.", id);
                    ra.addFlashAttribute("successMsg", "Invoice #" + inv.getInvoiceNumber() + " paid successfully!");
                    return "redirect:/personal/invoices";
                } catch (Exception e) {
                    log.error("FAILED to mark invoice {} as paid: {}", id, e.getMessage(), e);
                    ra.addFlashAttribute("errorMsg", "Payment processed, but failed to update invoice status. Please contact support.");
                }
            } else if ("INSUFFICIENT_FUNDS".equals(paymentResult)) {
                log.warn("Insufficient funds for user {} to pay invoice {}.", user.getUserId(), id);
                ra.addFlashAttribute("errorMsg", "Insufficient wallet balance to pay this invoice.");
            } else {
                log.error("Payment transaction FAILED for invoice {} with result: {}", id, paymentResult);
                ra.addFlashAttribute("errorMsg", "Payment failed. Result: " + paymentResult);
            }
        } catch (Throwable t) {
            log.error("FATAL error in payInvoice for ID {}: {}", id, t.getMessage(), t);
            ra.addFlashAttribute("errorMsg", "An unexpected error occurred during payment processing: " + t.getMessage());
        }
        
        return "redirect:/personal/invoices/" + id;
    }
}
