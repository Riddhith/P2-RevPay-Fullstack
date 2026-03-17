package com.revature.revpay.controller;

import com.revature.revpay.model.*;
import com.revature.revpay.service.BusinessService;
import com.revature.revpay.service.PaymentMethodService;
import com.revature.revpay.service.UserService;
import com.revature.revpay.service.NotificationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/business")
public class BusinessController {

    private final BusinessService businessService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final PaymentMethodService paymentMethodService;

    public BusinessController(BusinessService businessService, UserService userService, NotificationService notificationService, PaymentMethodService paymentMethodService) {
        this.businessService = businessService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.paymentMethodService = paymentMethodService;
    }

    // ---- Invoices ----

    @GetMapping("/invoices")
    public String invoices(Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("businessProfile", userService.getBusinessProfile(user.getUserId()));
        model.addAttribute("invoices", businessService.getInvoices(user.getUserId()));
        return "business/invoices";
    }

    @GetMapping("/invoices/new")
    public String newInvoicePage(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmailOrPhone(auth.getName()));
        return "business/invoice-form";
    }

    @PostMapping("/invoices/create")
    public String createInvoice(Authentication auth,
            @RequestParam String customerName,
            @RequestParam String customerEmail,
            @RequestParam(required = false) String customerAddress,
            @RequestParam String paymentTerms,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam(required = false) String notes,
            @RequestParam(defaultValue = "DRAFT") String status,
            @RequestParam List<String> itemDesc,
            @RequestParam List<BigDecimal> itemQty,
            @RequestParam List<BigDecimal> itemPrice,
            @RequestParam(required = false) List<BigDecimal> itemTax,
            RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        Invoice invoice = new Invoice();
        invoice.setBusinessUserId(user.getUserId());
        invoice.setCustomerName(customerName);
        invoice.setCustomerEmail(customerEmail);
        invoice.setCustomerAddress(customerAddress);
        invoice.setPaymentTerms(paymentTerms);
        invoice.setDueDate(dueDate);
        invoice.setNotes(notes);
        invoice.setStatus(status);

        List<InvoiceItem> items = new ArrayList<>();
        for (int i = 0; i < itemDesc.size(); i++) {
            if (itemDesc.get(i) == null || itemDesc.get(i).isBlank())
                continue;
            InvoiceItem it = new InvoiceItem();
            it.setDescription(itemDesc.get(i));
            it.setQuantity(itemQty.get(i));
            it.setUnitPrice(itemPrice.get(i));
            it.setTaxRate(itemTax != null && i < itemTax.size() ? itemTax.get(i) : BigDecimal.ZERO);
            items.add(it);
        }
        invoice.setItems(items);
        businessService.createInvoice(invoice);
        ra.addFlashAttribute("successMsg", "Invoice created!");
        return "redirect:/business/invoices";
    }

    @GetMapping("/invoices/{id}")
    public String viewInvoice(@PathVariable Long id, Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        Invoice inv = businessService.getInvoice(id);
        model.addAttribute("user", user);
        model.addAttribute("invoice", inv);
        return "business/invoice-detail";
    }

    @PostMapping("/invoices/{id}/mark-paid")
    public String markPaid(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        businessService.markInvoicePaid(id, user.getUserId());
        ra.addFlashAttribute("successMsg", "Invoice marked as paid.");
        return "redirect:/business/invoices";
    }

    @PostMapping("/invoices/{id}/send")
    public String sendInvoice(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        businessService.sendInvoice(id, user.getUserId());
        ra.addFlashAttribute("successMsg", "Invoice sent to customer.");
        return "redirect:/business/invoices";
    }

    @PostMapping("/invoices/{id}/cancel")
    public String cancelInvoice(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        businessService.cancelInvoice(id, user.getUserId());
        ra.addFlashAttribute("successMsg", "Invoice cancelled.");
        return "redirect:/business/invoices";
    }

    @PostMapping("/invoices/{id}/delete")
    public String deleteInvoice(@PathVariable Long id, RedirectAttributes ra) {
        businessService.deleteInvoice(id);
        ra.addFlashAttribute("successMsg", "Invoice deleted.");
        return "redirect:/business/invoices";
    }

    // ---- Loans ----

    @GetMapping("/loans")
    public String loans(Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("businessProfile", userService.getBusinessProfile(user.getUserId()));
        model.addAttribute("loans", businessService.getLoans(user.getUserId()));
        return "business/loans";
    }

    @GetMapping("/loans/apply")
    public String applyPage(Authentication auth, Model model, RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        List<PaymentMethod> pms = paymentMethodService.getByUser(user.getUserId());
        if (pms == null || pms.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "You must add at least one payment method before applying for a loan.");
            return "redirect:/business/loans";
        }
        model.addAttribute("user", user);
        return "business/loan-form";
    }

    @PostMapping("/loans/apply")
    public String applyLoan(Authentication auth,
            @RequestParam BigDecimal loanAmount,
            @RequestParam String purpose,
            @RequestParam Integer tenureMonths,
            @RequestParam(required = false) MultipartFile document,
            RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        
        List<PaymentMethod> pms = paymentMethodService.getByUser(user.getUserId());
        if (pms == null || pms.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "You must add at least one payment method before applying for a loan.");
            return "redirect:/business/loans";
        }
        
        LoanApplication loan = new LoanApplication();
        loan.setUserId(user.getUserId());
        loan.setLoanAmount(loanAmount);
        loan.setPurpose(purpose);
        loan.setTenureMonths(tenureMonths);
        loan.setInterestRate(BigDecimal.valueOf(12)); // default 12%

        if (document != null && !document.isEmpty()) {
            try {
                String uploadDir = System.getProperty("user.home") + "/revpay-uploads/";
                new File(uploadDir).mkdirs();
                String filename = System.currentTimeMillis() + "_" + document.getOriginalFilename();
                document.transferTo(new File(uploadDir + filename));
                loan.setDocPath(uploadDir + filename);
            } catch (Exception e) {
                /* skip doc save */ }
        }

        businessService.applyForLoan(loan);
        
        // Notify all admins of the new loan application
        List<User> admins = userService.findByRole("ADMIN");
        for (User admin : admins) {
            notificationService.send(admin.getUserId(), "New Loan Application", 
                "User " + user.getFullName() + " has applied for a new loan. Review it in the Admin Dashboard.", "SYSTEM");
        }
        
        ra.addFlashAttribute("successMsg", "Loan application submitted!");
        return "redirect:/business/loans";
    }

    @GetMapping("/loans/{id}")
    public String viewLoan(@PathVariable Long id, Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmailOrPhone(auth.getName()));
        model.addAttribute("loan", businessService.getLoan(id));
        return "business/loan-detail";
    }

    @PostMapping("/loans/{id}/repay")
    public String repay(@PathVariable Long id, Authentication auth,
            @RequestParam BigDecimal amount, RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        try {
            businessService.makeRepayment(id, user.getUserId(), amount);
            ra.addFlashAttribute("successMsg", "Repayment of ₹" + amount + " recorded.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "An unexpected error occurred during repayment.");
        }
        return "redirect:/business/loans/" + id;
    }

    // ---- Analytics ----

    @GetMapping("/analytics")
    public String analytics(Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        List<Invoice> invoices = businessService.getInvoices(user.getUserId());
        model.addAttribute("user", user);
        model.addAttribute("totalReceived", businessService.getTotalReceived(user.getUserId()));
        model.addAttribute("totalSent", businessService.getTotalSent(user.getUserId()));
        model.addAttribute("paidInvoices", businessService.getPaidInvoicesTotal(user.getUserId()));
        model.addAttribute("outstandingInvoices", businessService.getOutstandingInvoicesTotal(user.getUserId()));
        model.addAttribute("invoices", invoices);
        model.addAttribute("countPaid", invoices.stream().filter(i -> "PAID".equals(i.getStatus())).count());
        model.addAttribute("countSent", invoices.stream().filter(i -> "SENT".equals(i.getStatus())).count());
        model.addAttribute("countDraft", invoices.stream().filter(i -> "DRAFT".equals(i.getStatus())).count());
        model.addAttribute("countCancelled", invoices.stream().filter(i -> "CANCELLED".equals(i.getStatus())).count());
        model.addAttribute("topCustomers", businessService.getTopCustomersByVolume(user.getUserId()));
        return "business/analytics";
    }
}
