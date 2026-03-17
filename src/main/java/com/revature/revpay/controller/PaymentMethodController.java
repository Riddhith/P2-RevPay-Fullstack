package com.revature.revpay.controller;

import com.revature.revpay.model.PaymentMethod;
import com.revature.revpay.model.User;
import com.revature.revpay.service.PaymentMethodService;
import com.revature.revpay.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;
    private final UserService userService;

    public PaymentMethodController(PaymentMethodService paymentMethodService, UserService userService) {
        this.paymentMethodService = paymentMethodService;
        this.userService = userService;
    }

    @GetMapping({ "", "/" })
    public String listPage(Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("paymentMethods", paymentMethodService.getByUser(user.getUserId()));
        return "payment-methods/index";
    }

    @PostMapping("/add")
    public String addCard(Authentication auth,
            @RequestParam String cardType,
            @RequestParam String cardNumber,
            @RequestParam String cardholderName,
            @RequestParam int expiryMonth,
            @RequestParam int expiryYear,
            @RequestParam(required = false) String billingAddress,
            @RequestParam(required = false, defaultValue = "false") boolean makeDefault,
            @RequestParam String pin,
            @RequestParam String confirmPin,
            RedirectAttributes ra) {
        if (!pin.equals(confirmPin)) {
            ra.addFlashAttribute("errorMsg", "PINs do not match. Card not added.");
            return "redirect:/payment-methods";
        }
        User user = userService.findByEmailOrPhone(auth.getName());
        Long cardId = paymentMethodService.addCard(user.getUserId(), cardType, cardNumber, cardholderName,
                expiryMonth, expiryYear, billingAddress, makeDefault);
        if (cardId != null) {
            paymentMethodService.setPin(cardId, pin, user.getUserId());
            ra.addFlashAttribute("successMsg", "Card and PIN added successfully!");
        } else {
            ra.addFlashAttribute("errorMsg", "Failed to add card.");
        }
        return "redirect:/payment-methods";
    }

    @PostMapping("/{id}/delete")
    public String deleteCard(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        paymentMethodService.delete(id, user.getUserId());
        ra.addFlashAttribute("successMsg", "Card removed.");
        return "redirect:/payment-methods";
    }

    @PostMapping("/{id}/set-default")
    public String setDefault(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        paymentMethodService.setDefault(id, user.getUserId());
        ra.addFlashAttribute("successMsg", "Default card updated.");
        return "redirect:/payment-methods";
    }

    @PostMapping("/{id}/edit")
    public String editCard(@PathVariable Long id, Authentication auth,
            @RequestParam String cardholderName,
            @RequestParam(required = false) String billingAddress,
            RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        PaymentMethod pm = paymentMethodService.getById(id);
        if (pm != null && pm.getUserId().equals(user.getUserId())) {
            pm.setCardholderName(cardholderName);
            pm.setBillingAddress(billingAddress);
            paymentMethodService.update(pm);
            ra.addFlashAttribute("successMsg", "Card updated.");
        }
        return "redirect:/payment-methods";
    }

    @PostMapping("/{id}/pin")
    public String setPin(@PathVariable Long id, Authentication auth,
            @RequestParam String pin,
            RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        boolean success = paymentMethodService.setPin(id, pin, user.getUserId());
        if (success) {
            ra.addFlashAttribute("successMsg", "PIN set successfully for the card.");
        } else {
            ra.addFlashAttribute("errorMsg", "Failed to set PIN. Invalid card.");
        }
        return "redirect:/payment-methods";
    }
}
