package com.revature.revpay.controller;

import com.revature.revpay.model.PaymentMethod;
import com.revature.revpay.model.User;
import com.revature.revpay.service.PaymentMethodService;
import com.revature.revpay.service.TransactionService;
import com.revature.revpay.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/wallet")
public class WalletController {

    private final TransactionService transactionService;
    private final UserService userService;
    private final PaymentMethodService paymentMethodService;

    public WalletController(TransactionService transactionService, UserService userService,
            PaymentMethodService paymentMethodService) {
        this.transactionService = transactionService;
        this.userService = userService;
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping({ "", "/" })
    public String walletPage(Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        List<PaymentMethod> methods = paymentMethodService.getByUser(user.getUserId());
        model.addAttribute("user", user);
        model.addAttribute("paymentMethods", methods);
        model.addAttribute("hasCard", !methods.isEmpty());
        return "wallet/index";
    }

    @PostMapping("/add-funds")
    public String addFunds(Authentication auth,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(value = "methodId", required = false) Long methodId,
            @RequestParam(value = "pin", required = false) String pin,
            RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        List<PaymentMethod> methods = paymentMethodService.getByUser(user.getUserId());

        // Mandatory card check
        if (methods.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "Please add a card first before adding funds.");
            return "redirect:/payment-methods";
        }
        if (methodId == null) {
            ra.addFlashAttribute("errorMsg", "Please select a payment card.");
            return "redirect:/wallet";
        }

        if (!paymentMethodService.verifyPin(methodId, pin, user.getUserId())) {
            ra.addFlashAttribute("errorMsg",
                    "Invalid Card PIN. Operation aborted. Ensure you have set a PIN for this card.");
            return "redirect:/wallet";
        }

        String result = transactionService.addFunds(user.getUserId(), methodId, amount);
        if ("SUCCESS".equals(result)) {
            ra.addFlashAttribute("successMsg", "₹" + amount + " added to your wallet!");
        } else {
            ra.addFlashAttribute("errorMsg", "Failed to add funds: " + result);
        }
        return "redirect:/wallet";
    }

    @PostMapping("/withdraw")
    public String withdraw(Authentication auth,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(value = "methodId", required = false) Long methodId,
            @RequestParam(value = "pin", required = false) String pin,
            RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        List<PaymentMethod> methods = paymentMethodService.getByUser(user.getUserId());

        // Mandatory card check
        if (methods.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "Please add a card first before withdrawing funds.");
            return "redirect:/payment-methods";
        }
        if (methodId == null) {
            ra.addFlashAttribute("errorMsg", "Please select a payment card.");
            return "redirect:/wallet";
        }

        if (!paymentMethodService.verifyPin(methodId, pin, user.getUserId())) {
            ra.addFlashAttribute("errorMsg",
                    "Invalid Card PIN. Operation aborted. Ensure you have set a PIN for this card.");
            return "redirect:/wallet";
        }

        String result = transactionService.withdraw(user.getUserId(), methodId, amount);
        if ("SUCCESS".equals(result)) {
            ra.addFlashAttribute("successMsg", "₹" + amount + " withdrawn successfully!");
        } else {
            ra.addFlashAttribute("errorMsg", switch (result) {
                case "INSUFFICIENT_FUNDS" -> "Insufficient wallet balance.";
                default -> "Withdrawal failed.";
            });
        }
        return "redirect:/wallet";
    }

    @GetMapping("/card-balance")
    @ResponseBody
    public java.util.Map<String, Object> getCardBalance(Authentication auth, @RequestParam("methodId") Long methodId) {
        User user = userService.findByEmailOrPhone(auth.getName());
        List<PaymentMethod> methods = paymentMethodService.getByUser(user.getUserId());
        java.util.Map<String, Object> resp = new java.util.HashMap<>();

        BigDecimal selectedSlotBalance = BigDecimal.ZERO;

        for (PaymentMethod pm : methods) {
            if (pm.getMethodId().equals(methodId)) {
                selectedSlotBalance = pm.getBalance() != null ? pm.getBalance() : BigDecimal.ZERO;
                break;
            }
        }

        resp.put("status", "SUCCESS");
        resp.put("cardBalance", selectedSlotBalance);
        resp.put("walletBalance", user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO);
        return resp;
    }
}
