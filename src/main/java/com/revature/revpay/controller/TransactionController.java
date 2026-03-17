package com.revature.revpay.controller;

import com.revature.revpay.model.User;
import com.revature.revpay.service.TransactionService;
import com.revature.revpay.service.UserService;
import com.revature.revpay.util.ExportUtil;
import com.revature.revpay.model.Transaction;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;
    private final com.revature.revpay.service.PaymentMethodService paymentMethodService;

    public TransactionController(TransactionService transactionService, UserService userService,
            com.revature.revpay.service.PaymentMethodService paymentMethodService) {
        this.transactionService = transactionService;
        this.userService = userService;
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping("/send")
    public String sendPage(Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("paymentMethods", paymentMethodService.getByUser(user.getUserId()));
        return "transactions/send";
    }

    @PostMapping("/send")
    public String doSend(Authentication auth,
            @RequestParam("recipient") String recipient,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "methodId", required = false) Long methodId,
            @RequestParam(value = "pin", required = false) String pin,
            RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());

        if (paymentMethodService.getByUser(user.getUserId()).isEmpty()) {
            ra.addFlashAttribute("errorMsg", "You must have at least one payment card added to send money.");
            return "redirect:/payment-methods";
        }

        if (methodId == null) {
            ra.addFlashAttribute("errorMsg", "Please select a card to authenticate the transfer.");
            return "redirect:/transactions/send";
        }

        if (!paymentMethodService.verifyPin(methodId, pin, user.getUserId())) {
            ra.addFlashAttribute("errorMsg", "Invalid Card PIN. Authentication failed.");
            return "redirect:/transactions/send";
        }

        String result = transactionService.sendMoney(user.getUserId(), recipient, amount, note);
        if ("SUCCESS".equals(result)) {
            ra.addFlashAttribute("successMsg", "Money sent successfully!");
            return "redirect:/dashboard";
        }
        ra.addFlashAttribute("errorMsg", switch (result) {
            case "INSUFFICIENT_FUNDS" -> "Insufficient wallet balance.";
            case "RECEIVER_NOT_FOUND" -> "Recipient not found.";
            case "CANNOT_SEND_SELF" -> "You cannot send money to yourself.";
            default -> "Transfer failed: " + result;
        });
        return "redirect:/transactions/send";
    }

    @GetMapping("/history")
    public String history(Authentication auth, Model model,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
            @RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page) {
        User user = userService.findByEmailOrPhone(auth.getName());
        List<Transaction> transactions = transactionService.getHistory(
                user.getUserId(), type, from, to, minAmount, maxAmount, search, page);
        model.addAttribute("user", user);
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentPage", page);
        model.addAttribute("type", type);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("minAmount", minAmount);
        model.addAttribute("maxAmount", maxAmount);
        model.addAttribute("search", search);
        return "transactions/history";
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(Authentication auth) {
        User user = userService.findByEmailOrPhone(auth.getName());
        try {
            byte[] data = ExportUtil.exportToCsv(transactionService.getAllForExport(user.getUserId()));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(Authentication auth) {
        User user = userService.findByEmailOrPhone(auth.getName());
        try {
            byte[] data = ExportUtil.exportToPdf(transactionService.getAllForExport(user.getUserId()));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
